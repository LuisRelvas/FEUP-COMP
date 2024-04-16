package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.specs.comp.ollir.AccessModifier.*;
import static org.specs.comp.ollir.ElementType.*;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "\t";

    private final OllirResult ollirResult;

    List<Report> reports;
    String code;
    Method currentMethod;
    ClassUnit currentClass;
    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethodDecl);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLoad);
        generators.put(Operand.class, this::generateLoad);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CallInstruction.class, this::generateInvoke);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        if (code == null)
        {
            code = generators.apply(ollirResult.getOllirClass());
        }
        return code;
    }

    private String generateFields(ClassUnit classUnit) {
        StringBuilder result = new StringBuilder();

        for (var field : classUnit.getFields()) {
            result.append(".field ");
            if (field.getFieldAccessModifier() == PUBLIC) {
                result.append("public ");
            } else if (field.getFieldAccessModifier() == PRIVATE) {
                result.append("private ");
            }
            if (field.isStaticField()) {
                result.append("static ");
            }
            if (field.isFinalField()) {
                result.append("final ");
            }
            result.append(field.getFieldName()).append(" ")
                    .append(translateOllirToJasminType(field.getFieldType())).append(NL);
        }
        return result.toString();
    }

    private String generateClassUnit(ClassUnit classUnit) {
        currentClass = classUnit;

        String className = ollirResult.getOllirClass().getClassName();
        String superClass = (classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")) ? "java/lang/Object" : classUnit.getSuperClass();
        String fields = generateFields(classUnit);
        String methods = ollirResult.getOllirClass().getMethods().stream()
                .map(generators::apply)
                .collect(Collectors.joining());

        currentClass = null;

        return String.format(".class public %s\n\n.super %s\n%s%s", className, superClass, fields, methods);
    }

    private String translateOllirToJasminType(Type e) {
        switch (e.getTypeOfElement()) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case OBJECTREF:
                return "L" + ((ClassType) e).getName() + ";";
            case STRING:
                return "Ljava/lang/String;";
            case ARRAYREF:
                return "[" + translateOllirToJasminType(((ArrayType)e).getElementType());
            case VOID:
                return "V";
            default:
                return "; ERROR: translate thing error" + NL;
        }
    }

    private long getLocals(Method method) {
        return method.getVarTable().values().stream()
                .map(Descriptor::getVirtualReg)
                .distinct()
                .count()+1;
    }

    private String generateMethodDecl(Method method) {

        currentMethod = method;

        var code = new StringBuilder();

        var modifier = method.getMethodAccessModifier() != DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        StringBuilder typeMethodBuilder = new StringBuilder();
        if (method.isStaticMethod()) {
            typeMethodBuilder.append("static ");
        }
        String typeMethod = typeMethodBuilder.toString();

        var methodName = method.getMethodName();

        code.append(NL).append(".method ").append(modifier).append(typeMethod);
        if(method.isConstructMethod()) code.append("<init>");
        else code.append(methodName);
        code.append("(");
        for(Element e : method.getParams()) {
            code.append(translateOllirToJasminType(e.getType()));
        }
        code.append(")");
        code.append(translateOllirToJasminType(method.getReturnType())).append(NL);

        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals ").append(getLocals(method)).append(NL);

        for (Instruction inst : method.getInstructions()) {
            String labelKey = method.getLabels().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(inst))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (labelKey != null) {
                code.append(TAB).append(labelKey).append(":").append(NL);
            }

            String instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .map(line -> TAB + line)
                    .collect(Collectors.joining(NL+TAB, TAB, NL));

            code.append(instCode);

            if (inst.getInstType() == InstructionType.CALL) {
                CallInstruction callInst = (CallInstruction) inst;
                if (callInst.getReturnType().getTypeOfElement() != ElementType.VOID) {
                    code.append(TAB).append(TAB).append("pop").append(NL);
                }
            }
        }
        if(method.isConstructMethod()) {
            code.append("return").append(NL);
        }

        code.append(".end method").append(NL);

        currentMethod = null;

        return code.toString();
    }
    private String buildVarName(String s) {
        StringBuilder str = new StringBuilder();
        var reg = currentMethod.getVarTable().get(s).getVirtualReg();

        if (reg < 4) {
            str.append("_");
        } else {
            str.append(" ");
        }

        str.append(reg);
        return str.toString();
    }

    private String generateAssign(AssignInstruction assignInstruction) {
        StringBuilder code = new StringBuilder();

        Operand lhs = (Operand) assignInstruction.getDest();
        Instruction rhs = assignInstruction.getRhs();

        code.append(generators.apply(rhs));
        code.append(generateStore(lhs));
        return code.toString();
    }


    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        StringBuilder result = new StringBuilder();

        result.append(generateLoad(unaryOpInstruction.getOperand()));
        if(unaryOpInstruction.getOperation().getOpType().equals(OperationType.NOT))
        {
            result.append("ineg").append(NL);
        }
        else
        {
            throw new NotImplementedException(unaryOpInstruction.getOperation().getOpType());
        }
        return result.toString();
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOpInstruction) {
        var code = new StringBuilder();

        code.append(generators.apply(binaryOpInstruction.getLeftOperand()));
        code.append(generators.apply(binaryOpInstruction.getRightOperand()));

        var op = switch (binaryOpInstruction.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            case AND -> "iand";
            case OR -> "ior";
            default -> throw new NotImplementedException(binaryOpInstruction.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInstruction) {
        var code = new StringBuilder();

        if (returnInstruction.hasReturnValue()) {
            code.append(generators.apply(returnInstruction.getOperand()));
        }

        var retType = switch (returnInstruction.getElementType()) {
            case INT32, BOOLEAN -> "ireturn";
            case ARRAYREF, OBJECTREF, CLASS, THIS, STRING -> "areturn";
            case VOID -> "return";
            default -> throw new IllegalStateException("Unexpected value: " + returnInstruction.getReturnType());
        };

        code.append(retType).append(NL);

        return code.toString();
    }

    private String generateLoad(Element e) {
        StringBuilder result = new StringBuilder();

        if (e instanceof LiteralElement literalElement)
        {
            handleLiteralElement(result, literalElement);
        }

        else if (e instanceof Operand operand)
        {
            loadOperand(result, operand);
        }

        else
        {
            result.append("Error in generateLoad()");
        }

        result.append(NL);
        return result.toString();
    }

    private void handleLiteralElement(StringBuilder result, LiteralElement literalElement) {
        if (literalElement.getType().getTypeOfElement().equals(INT32) || literalElement.getType().getTypeOfElement().equals(BOOLEAN)) {
            int value = Integer.parseInt(literalElement.getLiteral());
            if (value == -1) {
                result.append("iconst_m1");
            } else {
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    result.append("bipush ").append(value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    result.append("sipush ").append(value);
                } else {
                    result.append("ldc ").append(value);
                }
            }
        } else {
            result.append("ldc ").append(literalElement);
        }
    }

    private void loadOperand(StringBuilder result, Operand operand) {
        switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> result.append("iload").append(buildVarName(operand.getName()));
            case OBJECTREF, STRING, ARRAYREF -> result.append("aload").append(buildVarName(operand.getName()));
            case THIS -> result.append("aload_0");
            default -> throw new NotImplementedException(operand.getType().getTypeOfElement());
        }
    }


    private String generateStore(Operand variable) {
        StringBuilder str = new StringBuilder();

        ElementType elementType = variable.getType().getTypeOfElement();
        String variableName = variable.getName();

        switch (elementType) {
            case OBJECTREF, THIS, STRING, ARRAYREF -> {
                str.append("astore").append(buildVarName(variableName));
            }
            case INT32, BOOLEAN -> {
                if (currentMethod.getVarTable().get(variableName).getVarType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
                    str.append("iastore");
                } else {
                    str.append("istore").append(buildVarName(variableName));
                }
            }
            default -> throw new NotImplementedException(elementType);
        }

        str.append(NL);
        return str.toString();
    }

    private String buildFullClassImport(String classNameWithoutImports) {

        if (classNameWithoutImports.equals("this")) {
            return currentClass.getClassName();
        }

        Optional<String> matchingImport = currentClass.getImports().stream()
                .filter(s -> s.endsWith(classNameWithoutImports))
                .findFirst();

        return matchingImport.map(s -> s.replaceAll("\\.", "/")).orElse(classNameWithoutImports);

    }

    private String generatePutField(PutFieldInstruction putField) {
        Operand firstOperand = (Operand) putField.getOperands().get(0);
        Operand secondOperand = (Operand) putField.getOperands().get(1);
        Element thirdOperand = putField.getOperands().get(2);

        String loadFirstOperand = generateLoad(firstOperand);
        String loadThirdOperand = generateLoad(thirdOperand);

        String classFullName = buildFullClassImport(firstOperand.getName());
        String fieldName = secondOperand.getName();
        String fieldDescriptor = translateOllirToJasminType(secondOperand.getType());

        String putFieldInstruction = String.format("putfield %s/%s %s\n", classFullName, fieldName, fieldDescriptor);

        return loadFirstOperand + loadThirdOperand + putFieldInstruction;
    }

    private String generateGetField(GetFieldInstruction getField) {
        Operand firstOperand = (Operand) getField.getOperands().get(0);
        Operand secondOperand = (Operand) getField.getOperands().get(1);

        String loadObjectReference = generateLoad(firstOperand);
        String classFullName = buildFullClassImport(firstOperand.getName());
        String fieldName = secondOperand.getName();
        String fieldDescriptor = translateOllirToJasminType(secondOperand.getType());

        String getFieldInstruction = String.format("getfield %s/%s %s\n", classFullName, fieldName, fieldDescriptor);

        return loadObjectReference + getFieldInstruction;
    }

    private String generateInvoke(CallInstruction callInstruction) {
        StringBuilder result = new StringBuilder();
        switch (callInstruction.getInvocationType()) {
            case invokevirtual -> {
                result.append(generateLoad(callInstruction.getOperands().get(0)));
                for(Element e : callInstruction.getArguments()) {
                    result.append(generateLoad(e));
                }
                result.append("invokevirtual ");
                String className = buildFullClassImport(((ClassType) callInstruction.getOperands().get(0).getType()).getName());
                result.append(className);

                result.append("/").append(((LiteralElement) callInstruction.getOperands().get(1)).getLiteral().replaceAll("\"", ""));
                result.append("(");
                for (Element element : callInstruction.getArguments()) {
                    result.append(translateOllirToJasminType(element.getType()));
                }
                result.append(")").append(translateOllirToJasminType(callInstruction.getReturnType())).append(NL);
            }
            case invokestatic -> {
                for (Element e : callInstruction.getArguments()) {
                    result.append(generateLoad(e));
                }
                result.append("invokestatic ");
                String className = buildFullClassImport(((Operand) callInstruction.getCaller()).getName());
                result.append(className);
                result.append("/").append(((LiteralElement) callInstruction.getOperands().get(1)).getLiteral().replaceAll("\"", ""));
                result.append("(");
                for (Element element : callInstruction.getArguments()) {
                    result.append(translateOllirToJasminType(element.getType()));
                }
                result.append(")").append(translateOllirToJasminType(callInstruction.getReturnType())).append(NL);
            }
            case invokespecial -> {
                result.append(generateLoad(callInstruction.getOperands().get(0)));
                for(Element e : callInstruction.getArguments()) {
                    result.append(generateLoad(e));
                }

                result.append("invokespecial ");

                if (callInstruction.getOperands().get(0).getType().getTypeOfElement() == THIS) {
                    if (currentClass.getSuperClass() == null || currentClass.getSuperClass().equals("Object")) {
                        result.append("java/lang/Object");
                    } else {
                        result.append(buildFullClassImport(currentClass.getSuperClass()));
                    }
                } else {
                    String className = buildFullClassImport(((ClassType) callInstruction.getOperands().get(0).getType()).getName());
                    result.append(className);
                }
                result.append("/");

                if(((LiteralElement) callInstruction.getOperands().get(1)).getLiteral().startsWith("<init>")) {
                    result.append(((LiteralElement)(callInstruction.getOperands().get(1))).getLiteral().replaceAll("\"", ""));
                }
                else
                {
                    result.append("<init>");
                }

                result.append("(");
                for (Element element : callInstruction.getArguments()) {
                    result.append(translateOllirToJasminType(element.getType()));
                }
                result.append(")").append(translateOllirToJasminType(callInstruction.getReturnType())).append(NL);
            }
            case NEW -> {
                ElementType elementType = callInstruction.getReturnType().getTypeOfElement();

                if (elementType == OBJECTREF) {
                    for (Element element : callInstruction.getArguments()) {
                        result.append(generateLoad(element));
                    }

                    result.append("new ").append(buildFullClassImport(((Operand) callInstruction.getOperands().get(0)).getName())).append(NL);
                } else {
                    throw new NotImplementedException(callInstruction.getInvocationType());
                }
            }
            case arraylength -> {
                result.append("arraylength").append(NL);
            }
            case ldc -> {
                result.append("ldc ").append(((LiteralElement) callInstruction.getOperands().get(0)).getLiteral()).append(NL);
            }
        }
        return result.toString();
    }

}