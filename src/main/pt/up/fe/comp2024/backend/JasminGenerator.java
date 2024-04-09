package pt.up.fe.comp2024.backend;

import com.sun.jdi.ObjectReference;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private  static final String SPACE = " ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

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
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class,this::generateCallInstruction);
        generators.put(PutFieldInstruction.class,this::generatePutFields);
        generators.put(GetFieldInstruction.class,this::generateGetFields);

    }


    private String getIntFromLiteral(Element ele){
        String fullEle = ele.toString();
        String typeString = ele.getType().toString();
        fullEle = fullEle.replaceAll(typeString,"");
        return getNumberOfLiteral(fullEle);
    }

    private String getNumberOfLiteral(String literalElement) {
        StringBuilder answer = new StringBuilder();

        int startInd = literalElement.indexOf(':');
        int endInd = literalElement.indexOf('.');
        answer.append(literalElement.substring(startInd+1,endInd));
        String answer2 = answer.toString();
        answer2 = answer2.replaceAll(" ","");
        return answer2;
    }

    //TODO : apagar maybe
    private String generateGetFields(GetFieldInstruction getFieldInstruction) {
        StringBuilder putCode = new StringBuilder();
        putCode.append("getfield"+SPACE);
        putCode.append(getFunctionObjectName(getFieldInstruction.getOperands().get(0).toString()) + "/");
        putCode.append(getFieldInstruction.getField().getName() + SPACE + ollirToJasminType(getFieldInstruction.getField().getType().toString())+NL);
        return putCode.toString();
    }

    private String generatePutFields(PutFieldInstruction putFieldInstruction) {

        StringBuilder putCode = new StringBuilder();
        putCode.append("bipush ");
        putCode.append(getIntFromLiteral(putFieldInstruction.getValue())+NL);
        //TODO : verificar com outros typos a ver se realmente é necessário o switch /A
        switch (putFieldInstruction.getValue().getType().toString()) {
            case "INT32":
                putCode.append("putfield ");
                putCode.append(getFunctionObjectName(putFieldInstruction.getOperands().get(0).toString()) + "/");
                putCode.append(putFieldInstruction.getField().getName() + SPACE + ollirToJasminType(putFieldInstruction.getField().getType().toString())+NL);
                putCode.append("aload_0"+NL);
                break;
            default:
                break;
        }
        return putCode.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction) {
        StringBuilder answer = new StringBuilder();
        String invoker = callInstruction.getInvocationType().toString();
        if (invoker.equals("NEW")){
            return "";
        }

        answer.append(callInstruction.getInvocationType()+SPACE);
        String caller = callInstruction.getCaller().toString();
        int indx1 = caller.indexOf(" ");
        int indx2 = caller.indexOf(".");
        String callerName = caller.substring(indx1+1, indx2);
        answer.append(callerName);
        switch(callInstruction.getMethodName().getType().toString()){
            case "STRING" :
                int ind1 = callInstruction.getMethodName().toString().indexOf('"');
                String argAux = callInstruction.getMethodName().toString().substring(ind1+1);
                int ind2 = argAux.indexOf('"');
                String arg = argAux.substring(0,ind2);
                answer.append('/'+arg);
                answer.append("()"+ollirToJasminType(callInstruction.getReturnType().toString())+NL);
                break;
            default:
                answer.append("/<init>()V"+NL);
                break;
        }
        //answer.append("pop"+NL);
        return answer.toString();
    }



    public List<Report> getReports() {
        return reports;
    }

    public String build() {


        code = generators.apply(ollirResult.getOllirClass());

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {
        var code = new StringBuilder();
        // generate class name
        var imports = classUnit.getImports();
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL);
        String superClass = classUnit.getSuperClass();
        //if (classUnit.getChildren().get(0). .equals("this"))
        // TODO: Hardcoded to Object, needs to be expanded
        if (superClass != null){
            superClass = "java/lang/Object";
            code.append(".super "+ superClass).append(NL).append(NL);
        }else{
            superClass = "java/lang/Object";
            code.append(".super "+ superClass).append(NL).append(NL);
        }

        /*
        ir buscar o que tem o mesmo nome ao fim, replace com "." "\"
        se for this entao é só o nome
         */
        //fields
        StringBuilder fields = new StringBuilder();
        for (var field :classUnit.getFields()){
            fields.append(".field ");
            //System.out.println(field.getFieldAccessModifier().toString().toLowerCase());
            //fields.append(field.getFieldAccessModifier().toString().toLowerCase()).append(SPACE);
            fields.append(field.getFieldName()).append(SPACE);
            fields.append(ollirToJasminType(field.getFieldType().toString())).append(NL);
        }

        code.append(fields).append(NL);

        // generate a single constructor method
        StringBuilder defaultConstructor = new StringBuilder();
        defaultConstructor.append(";default constructor"+NL);
        defaultConstructor.append(".method"+SPACE);
        defaultConstructor.append("public <init>()V"+NL);
        defaultConstructor.append(TAB+SPACE+"aload_0"+NL);

        if(superClass != null){
            defaultConstructor.append(TAB+ " invokespecial " + superClass + "/<init>()V"+ NL);
        }
        defaultConstructor.append(
                """
                    return
                .end method
                """
        );
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {
            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously

            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String ollirToJasminType(String ollirType){
        String answer = switch (ollirType){
            // TODO : outros return types /A
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "VOID" -> "V";
            case "STRING[]" -> "[Ljava/lang/String;"; //TODO : de momento está brute force, arrays são apenas tratados no cp3 /A
            default -> ollirType;
        };
        return answer;
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();
        String isStatic = "";
        if (method.isStaticMethod()){
            isStatic = "static ";
        }
        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        var retType = method.getReturnType();
        var returnType = ollirToJasminType(retType.toString());

        StringBuilder params = new StringBuilder();

        for (var param : method.getParams()){
            String newParam = ollirToJasminType(param.getType().toString());
            params.append(newParam);
        }


        code.append("\n.method ").append(modifier).append(isStatic).append(methodName).append("("+params+")"+returnType).append(NL);

        // Add limits
        //TODO : no cp3 não é suposto ser "99", porém no pdf do moodle diz para deixarmos assim de momento /A
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);
        if (!currentMethod.isStaticMethod()){
            code.append(TAB+"aload_0").append(NL);
        }
        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            code.append(instCode);

        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }


    private String getFunctionObjectName(String objectRefName){
        StringBuilder answer = new StringBuilder();

        int startInd = objectRefName.indexOf('(');
        int endInd = objectRefName.indexOf(')');
        answer.append(objectRefName.substring(startInd+1,endInd));

        return answer.toString();
    }

    private String generateAssign(AssignInstruction assign) {

        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));
        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {

            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        StringBuilder type = new StringBuilder();
        switch (operand.getType().toString()){
            case "INT32" :
                type.append("istore_").append(reg).append(NL);
                break;
            case "BOOLEAN" :
                type.append("istore_").append(reg).append(NL);
                break;
            default :
                type.append("new ").append(getFunctionObjectName(operand.getType().toString())).append(NL).append("dup"); //TODO : de momento a assumir que só há bools/ints e o que não for é um invoke
                break;
        };
        code.append(type);
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
        return "iload_" + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();


        switch (binaryOp.getOperation().getOpType()){
            case ADD :
                code.append(generators.apply(binaryOp.getLeftOperand()));
                code.append(generators.apply(binaryOp.getRightOperand()));
                code.append("iadd").append(NL);
                break;
            case SUB :
                code.append(generators.apply(binaryOp.getRightOperand()));
                code.append(generators.apply(binaryOp.getLeftOperand()));
                code.append("isub").append(NL);
                break;
            case MUL :
                code.append(generators.apply(binaryOp.getLeftOperand()));
                code.append(generators.apply(binaryOp.getRightOperand()));
                code.append("imul").append(NL);
                break;
            case DIV :
                code.append(generators.apply(binaryOp.getRightOperand()));
                code.append(generators.apply(binaryOp.getLeftOperand()));
                code.append("idiv").append(NL);
                break;
            default:

                throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        if(returnInst.getOperand()==null){
            code.append("return");
        }
        else{
            code.append(generators.apply(returnInst.getOperand()));
            code.append("ireturn").append(NL);
        }

        return code.toString();
    }

}
