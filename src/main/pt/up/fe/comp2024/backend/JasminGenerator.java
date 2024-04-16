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

// constants
import static org.specs.comp.ollir.AccessModifier.*;
import static org.specs.comp.ollir.ElementType.*;
import static org.specs.comp.ollir.InstructionType.*;

// TODO NEXT DO REST OF INVOKE

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
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
//        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(LiteralElement.class, this::generateLoad);
//        generators.put()
        generators.put(Operand.class, this::generateLoad);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(CallInstruction.class, this::generateInvoke);
//        generators.put(CondBranchInstruction.class, this::generateCondBranch;
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
//            System.out.println("PRINTING->JasminGenerator.java ollirResult.getOllirClass().getMethods():");
//            System.out.println(ollirResult.getOllirClass().getMethods());
//            for (Method m : ollirResult.getOllirClass().getMethods()) {
//                System.out.println(m.getMethodName());
//                System.out.println(m.getInstructions());
//            }
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private String generateFields(ClassUnit classUnit) {
        StringBuilder str = new StringBuilder();

        for (Field field : classUnit.getFields()) {
            str.append(".field ");
//            if (field.getFieldAccessModifier() != DEFAULT) {
//                str.append(field.getFieldAccessModifier().name().toLowerCase()).append(" ");
//            }
            switch(field.getFieldAccessModifier()) {
                case PUBLIC -> str.append("public ");
                case PRIVATE -> str.append("private ");
                case PROTECTED -> str.append("protected ");
                case DEFAULT -> {} // do nothing xd
                default -> {}
            }
            if (field.isStaticField()) {
                str.append("static ");
            }
            if (field.isFinalField()) {
                str.append("final ");
            }
            str.append(field.getFieldName()).append(" ").append(translateTypeToFD(field.getFieldType())).append(NL);
        }
        // System.out.println(str);
        return str.toString();
    }

    private String generateClassUnit(ClassUnit classUnit) {
        currentClass = classUnit;
        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL).append(NL);

        // find super class default to java/lang/Object
        if(classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")) code.append(".super java/lang/Object").append(NL);
        else code.append(".super ").append(classUnit.getSuperClass()).append(NL);

        // generate fields
        code.append(generateFields(classUnit));

//         generate a single constructor method
//        Eventually will be replaced when calls are done
//        var defaultConstructor = """
//                ;default constructor
//                .method public <init>()V
//                    aload_0
//                    invokespecial java/lang/Object/<init>()V
//                    return
//                .end method
//                """;
//        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
// will also be replaced when calls are done and invokespecial is done
//            if (method.isConstructMethod()) {
//                continue;
//            }

            code.append(generators.apply(method));
        }

        String codeStr = code.toString();
        // System.out.println(codeStr);

        currentClass = null;

        return codeStr;
    }

    private String translateTypeToFD(Type e) {
        StringBuilder fd = new StringBuilder();

        switch (e.getTypeOfElement()) {

            case INT32 -> fd.append("I");
            case BOOLEAN -> fd.append("Z");
            case OBJECTREF -> fd.append("L").append(((ClassType) e).getName()).append(";");
            case CLASS -> {
            }
            case THIS -> {
            }
            case STRING -> fd.append("Ljava/lang/String;");
            case ARRAYREF -> {
                fd.append("[");
                fd.append(translateTypeToFD(((ArrayType)e).getElementType()));
            }
            case VOID -> fd.append("V");

            default -> fd.append("; ERROR: translate thing error").append(NL);
        }
        return fd.toString();
    }

    private long calculateLocals(Method method) {
        return method.getVarTable().values().stream()
                .map(Descriptor::getVirtualReg)
                .distinct()
                .count()+1; // sum 1 because of the "this" local
    }

    private String generateMethod(Method method) {

        // set method in the class variable
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        // get spec
        StringBuilder specBuilder = new StringBuilder();
        if (method.isStaticMethod()) {
            specBuilder.append("static ");
        }
        if (method.isFinalMethod()) {
            specBuilder.append("final ");
        }
        String spec = specBuilder.toString();

        var methodName = method.getMethodName();

        code.append(NL).append(".method ").append(modifier).append(spec);
        if(method.isConstructMethod()) code.append("<init>");
        else code.append(methodName);
        code.append("("); // append first part of method
        for(Element e : method.getParams()) {
            code.append(translateTypeToFD(e.getType()));
        }
        code.append(")"); // closing parenthesis
        code.append(translateTypeToFD(method.getReturnType())).append(NL); // return type

        // Add limits TODO: PROBABLY HAS TO BE CALCULATED IDK
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals ").append(calculateLocals(method)).append(NL);

        for (Instruction inst : method.getInstructions()) {
            // Check if the instruction is a label
            String labelKey = method.getLabels().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(inst))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            // If instruction was a label, append the label with indentation
            if (labelKey != null) {
                code.append(TAB).append(labelKey).append(":").append(NL);
            }

//            var instCode = StringLines.getLines(generators.apply(inst)).stream()
//                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            // Append the instruction code
            String instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .map(line -> TAB + line) // Add indentation to each line
                    .collect(Collectors.joining(NL+TAB, TAB, NL));

            code.append(instCode);

            // Check if the instruction is a non-void method call
            if (inst.getInstType() == InstructionType.CALL) {
                CallInstruction callInst = (CallInstruction) inst;
                if (callInst.getReturnType().getTypeOfElement() != ElementType.VOID) {
                    code.append(TAB).append(TAB).append("pop").append(NL);
                    // stack -1
                }
            }
        }
        if(method.isConstructMethod()) {
            code.append("return").append(NL);
        }

        code.append(".end method").append(NL);

        // unset method
        currentMethod = null;

        return code.toString();
    }

    // will return either "_0" to "_4"    OR     " " + "5..."
    private String buildVarName(String s) {
        // System.out.println(s);
        StringBuilder str = new StringBuilder();
        var reg = currentMethod.getVarTable().get(s).getVirtualReg(); // get the virtual register

        if (reg < 4) {
            str.append("_");
        } else {
            str.append(" ");
        }

        str.append(reg);
        return str.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        StringBuilder code = new StringBuilder();

        Operand lhs = (Operand) assign.getDest();
        Instruction rhs = assign.getRhs();

        if (lhs instanceof ArrayOperand) {
            code.append(buildArrayAssignment((ArrayOperand) lhs));
        } else {
            // Check if we need to use iinc
            if (rhs.getInstType() == BINARYOPER && isIncrementOperation((BinaryOpInstruction) rhs, lhs)) {
                code.append(buildIncrementOperation((BinaryOpInstruction) rhs, lhs));
            } else {
                // Otherwise, generate code normally
                code.append(generators.apply(rhs));
                code.append(generateStore(lhs));
            }
        }

        return code.toString();
    }

    private boolean isIncrementOperation(BinaryOpInstruction binaryOpInstruction, Operand lhs) {
        boolean leftIsLiteral = binaryOpInstruction.getLeftOperand().isLiteral();
        boolean rightIsLiteral = binaryOpInstruction.getRightOperand().isLiteral();
        LiteralElement literal = null;
        Operand operand = null;

        // Determine which side is the literal and which is the operand
        if (leftIsLiteral && !rightIsLiteral) {
            literal = (LiteralElement) binaryOpInstruction.getLeftOperand();
            operand = (Operand) binaryOpInstruction.getRightOperand();
        } else if (!leftIsLiteral && rightIsLiteral) {
            literal = (LiteralElement) binaryOpInstruction.getRightOperand();
            operand = (Operand) binaryOpInstruction.getLeftOperand();
        }

        // Check if the operand is the same as the lhs variable
        return literal != null && operand != null && operand.getName().equals(lhs.getName());
    }

    private String buildIncrementOperation(BinaryOpInstruction binaryOpInstruction, Operand lhs) {
        LiteralElement literal;
        Operand operand;

        // Determine which side is the literal and which is the operand
        if (binaryOpInstruction.getLeftOperand().isLiteral()) {
            literal = (LiteralElement) binaryOpInstruction.getLeftOperand();
            operand = (Operand) binaryOpInstruction.getRightOperand();
        } else {
            literal = (LiteralElement) binaryOpInstruction.getRightOperand();
            operand = (Operand) binaryOpInstruction.getLeftOperand();
        }

        // Check if the operand matches the lhs variable
        if (operand.getName().equals(lhs.getName())) {
            int literalValue = Integer.parseInt(literal.getLiteral());
            if (literalValue >= Byte.MIN_VALUE && literalValue <= Byte.MAX_VALUE) {
                // If the literal value fits within the iinc bounds, generate the iinc instruction
                return "iinc " + currentMethod.getVarTable().get(operand.getName()).getVirtualReg() + " " + literalValue + "\n";
            }
        }
        // Return an empty string if no iinc instruction is generated
        return "";
    }

    private String buildArrayAssignment(ArrayOperand arrayVariable) {
        StringBuilder code = new StringBuilder();

        code.append("aload").append(buildVarName(arrayVariable.getName())).append(NL);
        code.append(generateLoad(arrayVariable.getIndexOperands().get(0)));

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return "iload " + reg + NL;
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        StringBuilder str = new StringBuilder();

        str.append(generateLoad(unaryOpInstruction.getOperand())); // Load operand onto the stack

        switch (unaryOpInstruction.getOperation().getOpType()) {
            case NOT -> str.append("ineg").append(NL); // Use "ineg" instruction for arithmetic negation
            // case NOTB -> // TODO BOOLEAN
            default -> throw new NotImplementedException(unaryOpInstruction.getOperation().getOpType());
        }

        return str.toString();
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        // System.out.println("PRINTING BinaryOpInstruction binaryOp.getOperation().getType()");
        // System.out.println(binaryOp.toString());
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation TODO MAKE BOOLEANS WORK
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            case SHR -> "ishr";
            case SHL -> "ishl";
            case SHRR -> "iushr";
            case XOR -> "ixor";
            case AND -> "iand";
            case OR -> "ior";
            case LTH -> "if_icmplt";
            case GTH -> "if_icmpgt";
            case EQ -> "if_icmpeq";
            case NEQ -> "if_icmpne";
            case LTE -> "if_icmple";
            case GTE -> "if_icmpge";
            case ANDB -> "land"; // bool TODO
            case ORB -> "lor"; // bool TODO
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();
        // System.out.println("Ret:"+returnInst);

        if (returnInst.hasReturnValue()) {
            // load value to the stack
            code.append(generators.apply(returnInst.getOperand()));
        }

        var retType = switch (returnInst.getElementType()) {
            case INT32, BOOLEAN -> "ireturn";
            case ARRAYREF, OBJECTREF, CLASS, THIS, STRING -> "areturn";
            case VOID -> "return";
            default -> throw new IllegalStateException("Unexpected value: " + returnInst.getReturnType());
        };

        code.append(retType).append(NL);

        return code.toString();
    }

    private String generateLoad(Element element) {
        StringBuilder str = new StringBuilder();

        if (element instanceof LiteralElement literalElement) {
            handleLiteralElement(str, literalElement);
        } else if (element instanceof ArrayOperand arrayOperand) {
            loadArrayOperand(str, arrayOperand);
        } else if (element instanceof Operand operand) {
            loadOperand(str, operand);
        } else {
            str.append("Error in generateLoad()");
        }

        str.append(NL);
        return str.toString();
    }

    private void handleLiteralElement(StringBuilder str, LiteralElement literalElement) {
        if (literalElement.getType().getTypeOfElement().equals(INT32) || literalElement.getType().getTypeOfElement().equals(BOOLEAN)) {
            int value = Integer.parseInt(literalElement.getLiteral());
            switch (value) {
                case -1 -> {
                    str.append("iconst_m1");
                }
                default -> {
                    if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                        str.append("bipush ").append(value);
                    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                        str.append("sipush ").append(value);
                    } else {
                        str.append("ldc ").append(value);
                    }
                }
            }
        } else {
            str.append("ldc ").append(literalElement);
        }
    }

    private void loadArrayOperand(StringBuilder str, ArrayOperand arrayOperand) {
        // Load the reference to the array
        str.append("aload").append(buildVarName(arrayOperand.getName())).append(NL);
        // Load the first element in the array
        str.append(generateLoad(arrayOperand.getIndexOperands().get(0)));
        str.append("iaload");
    }

    private void loadOperand(StringBuilder str, Operand operand) {
        switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> str.append("iload").append(buildVarName(operand.getName()));
            case OBJECTREF, STRING, ARRAYREF -> str.append("aload").append(buildVarName(operand.getName()));
            case THIS -> str.append("aload_0");
            default -> throw new NotImplementedException(operand.getType().getTypeOfElement());
        }
    }


    private String generateStore(Operand variable) {
        StringBuilder str = new StringBuilder();

        ElementType elementType = variable.getType().getTypeOfElement();
        String variableName = variable.getName();
//        int stackChange;

        switch (elementType) {
            case OBJECTREF, THIS, STRING, ARRAYREF -> {
                str.append("astore").append(buildVarName(variableName));
//                stackChange = -1;
            }
            case INT32, BOOLEAN -> {
                if (currentMethod.getVarTable().get(variableName).getVarType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
                    str.append("iastore");
//                    stackChange = -3;
                } else {
                    str.append("istore").append(buildVarName(variableName));
//                    stackChange = -1;
                }
            }
            default -> throw new NotImplementedException(elementType);
        }

        str.append(NL);
        // Adjust stack based on stackChange

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
        Element firstOperand = putField.getOperands().get(0);
        Element secondOperand = putField.getOperands().get(1);
        Element thirdOperand = putField.getOperands().get(2);

        String loadFirstOperand = generateLoad(firstOperand);
        String loadThirdOperand = generateLoad(thirdOperand);

        String classFullName = buildFullClassImport(((Operand)firstOperand).getName());
        String fieldName = ((Operand) secondOperand).getName();
        String fieldDescriptor = translateTypeToFD(secondOperand.getType());

        String putFieldInstruction = String.format("putfield %s/%s %s", classFullName, fieldName, fieldDescriptor) + NL;

//        -2 stack

        return loadFirstOperand + loadThirdOperand + putFieldInstruction;
    }

    private String generateGetField(GetFieldInstruction getField) {
        Element firstOperand = getField.getOperands().get(0);
        Element secondOperand = getField.getOperands().get(1);

        // Load the object reference onto the stack
        String loadObjectReference = generateLoad(firstOperand);

        // Get the class name and field name
        String classFullName = buildFullClassImport(((Operand) firstOperand).getName());
        String fieldName = ((Operand)secondOperand).getName();

        // Get the field descriptor
        String fieldDescriptor = translateTypeToFD(secondOperand.getType());

        // Construct the getfield instruction
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
                // Handle invokevirtual
                // This typically calls instance methods on objects
                result.append("invokevirtual ");
                // Append the class name
                String className = buildFullClassImport(((ClassType) callInstruction.getOperands().get(0).getType()).getName());
                result.append(className);
                // Append the method name and descriptor
                result.append("/").append(((LiteralElement) callInstruction.getOperands().get(1)).getLiteral().replaceAll("\"", ""));
                result.append("(");
                for (Element element : callInstruction.getArguments()) {
                    result.append(translateTypeToFD(element.getType()));
                }
                result.append(")").append(translateTypeToFD(callInstruction.getReturnType())).append(NL);
            }
            case invokestatic -> {
                for (Element e : callInstruction.getArguments()) {
                    result.append(generateLoad(e));
                }
                // Handle invokestatic
                // This is used for static method invocation
                // Add your implementation here
                result.append("invokestatic ");
                // Append the class name
                // System.out.println("PRINTING CLASS TYPE: " + callInstruction.getOperands().get(0));
                String className = buildFullClassImport(((Operand) callInstruction.getCaller()).getName());
                result.append(className);
                // Append the method name and descriptor
                result.append("/").append(((LiteralElement) callInstruction.getOperands().get(1)).getLiteral().replaceAll("\"", ""));
                result.append("(");
                for (Element element : callInstruction.getArguments()) {
                    result.append(translateTypeToFD(element.getType()));
                }
                result.append(")").append(translateTypeToFD(callInstruction.getReturnType())).append(NL);
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

                // Check if the method being invoked is a constructor and already have the <init> prefix
                if(((LiteralElement) callInstruction.getOperands().get(1)).getLiteral().startsWith("<init>")) {
                    result.append(((LiteralElement)(callInstruction.getOperands().get(1))).getLiteral().replaceAll("\"", ""));
                }
                else
                {
                    result.append("<init>");
                }

                result.append("(");
                for (Element element : callInstruction.getArguments()) {
                    result.append(translateTypeToFD(element.getType()));
                }
                result.append(")").append(translateTypeToFD(callInstruction.getReturnType())).append(NL);
            }
            case NEW -> {
                ElementType elementType = callInstruction.getReturnType().getTypeOfElement();

                // System.out.println(callInstruction.getOperands().toString());
                // System.out.println(callInstruction.getArguments().toString());
                if (elementType == OBJECTREF) {
                    for (Element element : callInstruction.getArguments()) {
                        result.append(generateLoad(element));
                    } // Handle arguments

                    // Then operands
                    result.append("new ").append(buildFullClassImport(((Operand) callInstruction.getOperands().get(0)).getName())).append(NL);
                } else {
                    throw new NotImplementedException(callInstruction.getInvocationType());
                }
            }
            case arraylength -> {
                // Handle arraylength
                // This is used for getting the length of an array
                // Add your implementation here
                result.append("arraylength").append(NL);
            }
            case ldc -> {
                result.append("ldc ").append(((LiteralElement) callInstruction.getOperands().get(0)).getLiteral()).append(NL);
            }
        }
//    stack -pop
        return result.toString();
    }

    private String generateGoto(GotoInstruction gotoInstruction) {
        return gotoInstruction.getLabel() + NL;
    }

}