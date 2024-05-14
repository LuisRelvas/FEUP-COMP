package pt.up.fe.comp2024.backend;

import com.sun.jdi.ObjectReference;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    int maxByte = java.lang.Byte.MAX_VALUE;

    int maxShort = java.lang.Short.MAX_VALUE;

    List<String> lastlabel;
    List<String> imports;

    List<Report> reports;

    String code;

    String noper = null;
    Method currentMethod;

    ClassUnit curCLass;

    private int curStackSize;

    private int maxStackSize;

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
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(CondBranchInstruction.class, this::generateBranchCondition);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(ArrayOperand.class,this::generateArrayOperand);
        generators.put(UnaryOpInstruction.class,this::generateUnariyOperator);



    }

    private String generateUnariyOperator(UnaryOpInstruction unaryOpInstruction) {
        //TODO rever,o ixor não parece estar correto
        StringBuilder answer = new StringBuilder();
        if(unaryOpInstruction.getOperand().getClass().toString().equals("class org.specs.comp.ollir.LiteralElement")){
            answer.append(generators.apply(unaryOpInstruction.getOperand()));
            answer.append("iconst_1"+NL);
        }
        else{
            var op = (Operand) unaryOpInstruction.getOperand();
            var reg = currentMethod.getVarTable().get(op.getName()).getVirtualReg();
            answer.append(loader("i",reg)+NL);
            answer.append("iconst_1"+NL);
        }
        curStackSize++;
        if(this.curStackSize>this.maxStackSize){
            this.maxStackSize=this.curStackSize;
        }
        answer.append("ixor"+NL);
        curStackSize--;
        return answer.toString();
    }

    private String generateArrayOperand(ArrayOperand arrayOperand) {
        StringBuilder answer = new StringBuilder();
        if(arrayOperand.getIndexOperands().get(0).getClass().toString().equals("class org.specs.comp.ollir.LiteralElement")){
            answer.append(generators.apply(arrayOperand.getIndexOperands().get(0)));
        }
        else{
            var index = (Operand) arrayOperand.getIndexOperands().get(0);
            var reg = currentMethod.getVarTable().get(arrayOperand.getName()).getVirtualReg();
            if(arrayOperand.getType().toString().equals("INT32")){
                answer.append(loader("a",reg)+NL);
                var reg2 = currentMethod.getVarTable().get(index.getName()).getVirtualReg();
                answer.append(loader("i",reg2)+NL);
                answer.append("iaload"+NL);
                curStackSize--;

            }
            else if(arrayOperand.getType().toString().equals("BOOLEAN")){
                answer.append(loader("a",reg)+NL);
                var reg2 = currentMethod.getVarTable().get(index.getName()).getVirtualReg();
                answer.append(loader("b",reg2));
                answer.append("baload"+NL);
                curStackSize--;
            }
        }

        return answer.toString();
    }

    private String generateGoto(GotoInstruction gotoInstruction) {
        StringBuilder answer = new StringBuilder();
        answer.append("goto ");
        answer.append(gotoInstruction.getLabel()+NL);
        this.lastlabel.add(gotoInstruction.getLabel());
        return answer.toString();
    }

    private String generateBranchCondition(CondBranchInstruction condBranchInstruction) {
        String fullCond = condBranchInstruction.getCondition().toString();
        fullCond = fullCond.replace("Inst: BINARYOPER ","");
        StringBuilder answer = new StringBuilder();
        var operands = condBranchInstruction.getOperands();


        for (var op : operands){
            if(op.getClass().toString().equals("class org.specs.comp.ollir.LiteralElement")){
                fullCond = fullCond.replace(op.toString(),"");
                answer.append(generators.apply(op));
                curStackSize++;
                if(this.curStackSize>this.maxStackSize){
                    this.maxStackSize=this.curStackSize;
                }
            }
            else{
                var operand = (Operand) op;
                fullCond = fullCond.replace(operand.toString(),"");
                var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
                answer.append(loader("i", reg)+NL);
            }

        }
        String condition = fullCond.replace(" ","");

        //TODO fazer resto operações
        this.lastlabel.add(condBranchInstruction.getLabel());
        curStackSize = curStackSize-2;
        switch (condition){
            case "LTH":
                answer.append("if_icmplt ");
                break;
            case "GTE":
                answer.append("if_icmpge ");
                break;
            default:
                throw new NotImplementedException(condition);

        }
        answer.append(condBranchInstruction.getLabel()+NL);
        return answer.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction singleOpCondInstruction) {
        StringBuilder answer = new StringBuilder();
        if(singleOpCondInstruction.getCondition().getInstType().toString().equals("NOPER")){
            var args = singleOpCondInstruction.getOperands();
            for(var ele : args){

                if(ele.getClass().toString().equals("class org.specs.comp.ollir.LiteralElement")){
                    answer.append(generators.apply(ele));
                    curStackSize++;
                    if(this.curStackSize>this.maxStackSize){
                        this.maxStackSize=this.curStackSize;
                    }
                }
                else{
                    var op = (Operand) ele;
                    var reg = currentMethod.getVarTable().get(op.getName()).getVirtualReg();
                    answer.append(loader("i", reg)+NL);
                }

            }
            this.noper = null;
            answer.append("ifne");
            curStackSize = curStackSize-2;

        }
        else{
            System.out.println("here no else ghu8af");
        }
        answer.append(SPACE+singleOpCondInstruction.getLabel()+NL);
        this.lastlabel.add(singleOpCondInstruction.getLabel());
        return answer.toString();
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



    private String loader(String prefix, int reg) {
        curStackSize++;
        if(this.curStackSize>this.maxStackSize){
            this.maxStackSize=this.curStackSize;
        }
        if (reg < 4) {
            return prefix + "load_" + reg;
        } else {
            return prefix+"load "+reg;
        }
    }
    private String storer(String prefix, int reg) {
        this.curStackSize--;
        if (reg < 4) {
            return prefix + "store_" + reg;
        } else {
            return prefix+"store "+reg;
        }
    }
    private String generateGetFields(GetFieldInstruction getFieldInstruction) {
        //verificar
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
        putCode.append("putfield ");
        String fullClassName = fullClassImport(getFunctionObjectName(putFieldInstruction.getOperands().get(0).toString()));
        putCode.append(fullClassName + "/");
        putCode.append(putFieldInstruction.getField().getName() + SPACE + ollirToJasminType(putFieldInstruction.getField().getType().toString())+NL);
        putCode.append("aload_0"+NL);
        this.curStackSize++;
        if(this.curStackSize>this.maxStackSize){
            this.maxStackSize=this.curStackSize;
        }


        return putCode.toString();
    }

    private String fullClassImport(String className){
        if(className==null){
            return null;
        }
        if(className.equals("this")){
            return className;
        }
        for(var x : this.curCLass.getImports()){
            if(x.contains(".")) {
                var integer = x.lastIndexOf(".");
                var substring = x.substring(integer +1);
                if(substring.equals(className)) {
                    className = x;
                    className = className.replace(".", "/");
                }
            }
        }

        return className;
    }

    private String generateCallInstruction(CallInstruction callInstruction) {

        StringBuilder answer = new StringBuilder();
        String invoker = callInstruction.getInvocationType().toString();

        if(invoker.equals("NEW")){
            var args = callInstruction.getOperands();
            if(callInstruction.getCaller().toString().equals("Operand: array.<no type>")){
                if(callInstruction.getOperands().get(1).getType().toString().equals("INT32")) {
                    for( var x : args){
                        if(x.equals(callInstruction.getCaller())){continue;}
                        if(x.getClass().toString().equals("class org.specs.comp.ollir.LiteralElement")){
                            answer.append(generators.apply(x));
                        }
                        else{
                            var op = (Operand) x;
                            var reg = currentMethod.getVarTable().get(op.getName()).getVirtualReg();
                            answer.append(loader("i",reg)+NL);
                        }
                    }
                    answer.append("newarray int"+NL);
                }
            }
        }
        else if(invoker.equals("arraylength")){

            var op = (Operand) callInstruction.getOperands().get(0);
            var reg = currentMethod.getVarTable().get(op.getName()).getVirtualReg();

            answer.append(loader("a",reg)+NL);
            answer.append("arraylength"+NL);
        }
        else {
            if (callInstruction.getInvocationType().toString().equals("invokespecial")){


                String caller = callInstruction.getCaller().toString();
                int indx1 = caller.indexOf("(");
                int indx2 = caller.indexOf(")");
                String callerName = caller.substring(indx1 + 1, indx2);

                for(var i : this.imports)
                {
                    if(i.contains("."))
                    {
                        var integer = i.lastIndexOf(".");
                        var substring = i.substring(integer +1 ,i.length());
                        if(substring.equals(callerName))
                        {
                            callerName = i;
                            callerName = callerName.replace(".", "/");
                        }
                    }
                }
                answer.append("new "+ callerName+NL);
                answer.append("dup"+NL);
                answer.append(callInstruction.getInvocationType() + SPACE);
                answer.append(callerName);
                answer.append("/<init>()V" + NL);
                var operand = (Operand) callInstruction.getOperands().get(0);
                var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
                answer.append(storer("a", reg)+NL);

            }
            if(callInstruction.getInvocationType().toString().equals("invokestatic")){


                var args = callInstruction.getArguments();

                for(var x : args){
                    if(x.getClass().toString().equals("class org.specs.comp.ollir.Operand")){
                        var operand = (Operand) x;
                        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
                        answer.append(loader("i",reg)+NL);
                    }
                    if(x.getClass().toString().equals("class org.specs.comp.ollir.LiteralElement")){
                        answer.append(generators.apply(x));
                    }
                }


                int ind1 = callInstruction.getMethodName().toString().indexOf('"');
                String argAux = callInstruction.getMethodName().toString().substring(ind1 + 1);
                int ind2 = argAux.indexOf('"');
                String function = argAux.substring(0, ind2);

                ind1 = callInstruction.getCaller().toString().indexOf(' ');
                String argAux2 = callInstruction.getCaller().toString().substring(ind1 + 1);
                ind2 = argAux2.indexOf('.');
                String functionClass = argAux2.substring(0, ind2);
                answer.append("invokestatic ");
                functionClass = functionClass+"/"+function;
                functionClass = fullClassImport(functionClass);
                answer.append(functionClass+"(");
                for(var x : args){
                    answer.append(ollirToJasminType(x.getType().toString()));
                }
                answer.append(")" + ollirToJasminType(callInstruction.getReturnType().toString()) + NL);

            }
            if(callInstruction.getInvocationType().toString().equals("invokevirtual")){
                //objref
                //arg1, arg2...
                //invoke virtual Object/func(a1t a2t) return
                StringBuilder answerAux = new StringBuilder();
                answerAux.append(callInstruction.getInvocationType() + SPACE);
                String caller = callInstruction.getCaller().toString();
                int indx1 = caller.indexOf(" ");
                int indx2 = caller.indexOf(".");
                String callerName = caller.substring(indx1+1, indx2);
                var reg = currentMethod.getVarTable().get(callerName).getVirtualReg();
                answer.append(loader("a",reg)+NL); //TODO rever para o tipo de caller // será necessário?
                //objref pushado para a stack
                var args = callInstruction.getArguments();
                for (var arg : args){
                    if(arg.getClass().toString().equals("class org.specs.comp.ollir.LiteralElement")){
                        answer.append(generators.apply(arg));
                    }
                    else {
                        var op = (Operand) arg;
                        reg = currentMethod.getVarTable().get(op.getName()).getVirtualReg();
                        switch (op.getType().toString()) {
                            case "INT32":
                                answer.append(loader("i", reg) + NL);
                                break;
                            case "BOOLEAN":
                                answer.append(loader("i", reg) + NL);
                                break;
                            case "INT32[]":
                                answer.append(loader("a", reg) + NL);
                                break;
                            default:
                                throw new NotImplementedException(op.getType().toString());
                        }
                    }
                }

                // loads de args feitos
                indx1 = caller.indexOf("(");
                indx2 = caller.indexOf(")");
                String callerType = caller.substring(indx1+1,indx2);
                answer.append("invokevirtual "+callerType+"/");
                // "invokevirtual tipo/" feito
                String function = callInstruction.getMethodName().toString();
                indx1 = function.indexOf(" ");
                function = function.substring(indx1+2);
                indx1 = function.indexOf('"');
                function = function.substring(0,indx1);
                answer.append(function+"(");
                // "invokevirtual tipo/função(" feito
                for (var arg : args){
                    answer.append(ollirToJasminType(arg.getType().toString()));
                }
                answer.append(")");
                answer.append(ollirToJasminType(callInstruction.getReturnType().toString())+NL);
                if (callInstruction.getMethodName().getType().toString().equals("STRING")) {
//                    int ind1 = callInstruction.getMethodName().toString().indexOf('"');
//                    String argAux = callInstruction.getMethodName().toString().substring(ind1 + 1);
//                    int ind2 = argAux.indexOf('"');
//                    String arg = argAux.substring(0, ind2);
//                    answerAux.append('/').append(arg);
//                    answerAux.append("(");
//                    var args = callInstruction.getOperands();
//                    StringBuilder loads = new StringBuilder();
//                    if (args.size() > 2) {
//                        for (int i = 2; i < args.size(); i++) {
//                            answerAux.append(ollirToJasminType(args.get(i).getType().toString()));
//                            var operand = (Operand) args.get(i);
//                            reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
//                            var type = currentMethod.getVarTable().get(operand.getName()).getVarType();
//                            if (type.toString().equals("INT32") ||type.toString().equals("BOOLEAN")){
//                                answer.append(loader("i", reg)+NL);
//                            }
//                            else{
//                                answer.append(loader("a", reg)+NL);
//                            }
//                        }
//                    }

                } else {
                    answer.append("/<init>()V" + NL);
                    answer.append("pop" + NL);
                }
            }
        }
        return answer.toString();
    }


    private int calculateLimitLocals(){
        return this.currentMethod.getVarTable().size();
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {


        code = generators.apply(ollirResult.getOllirClass());

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {
        this.curCLass = classUnit;
        var code = new StringBuilder();

        this.imports = classUnit.getImports();

        var className =  fullClassImport(ollirResult.getOllirClass().getClassName());

        code.append(".class ").append(className).append(NL);
        String superClass = classUnit.getSuperClass();
        superClass = fullClassImport(superClass);

        if (superClass == null || superClass.equals("Object")){
            superClass = "java/lang/Object";
            code.append(".super "+ superClass).append(NL).append(NL);
        }else{
            code.append(".super "+ superClass).append(NL).append(NL);
        }

        //fields
        StringBuilder fields = new StringBuilder();
        for (var field : classUnit.getFields()){
            fields.append(".field public ");
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

            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));


        }
        this.curCLass = null;
        return code.toString();
    }

    private String ollirToJasminType(String ollirType){

        String answer;
        switch (ollirType){
            case "INT32" :
                answer = "I";
                break;
            case "BOOLEAN" :
                answer = "Z";
                break;
            case "VOID" :
                answer = "V";
                break;
            case "STRING[]" :
                answer = "[Ljava/lang/String;";
                break;
            default :
                String endAnser = ollirType.substring(ollirType.length()-2);
                if(endAnser.equals("[]")){
                    StringBuilder answerAux = new StringBuilder();
                    answerAux.append("[");
                    answerAux.append(ollirToJasminType(ollirType.substring(0,ollirType.length()-2)));
                    answer = answerAux.toString();
                }
                else{
                    answer = "A";
                }
                break;
        };

        return answer;
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;
        this.lastlabel = new ArrayList<>();
        this.curStackSize = 0;
        this.maxStackSize = 0;
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
        //TODO rever se o calLimLoc está certo
        //var limitLocals = calculateLimitLocals();

        //TOdo clacular limit stack ver o tamanho máximo que a stack ocupa dentro de um método
        code.append(TAB).append(".limit stack 99").append(NL);

        int limitLocals = this.currentMethod.getVarTable().size();
        System.out.println(this.currentMethod.getVarTable());
        System.out.println(limitLocals);
        for(var argument: this.currentMethod.getParams()){
            int ind1 = argument.toString().indexOf(" ");
            int ind2 = argument.toString().indexOf(".");
            String name = argument.toString().substring(ind1+1,ind2);
            if(this.currentMethod.getVarTable().get(name)==null){
                limitLocals++;
            }
        }
        System.out.println(limitLocals);
        if(!this.currentMethod.getMethodName().equals("main")){
            if(this.currentMethod.getVarTable().get("this")==null){
                limitLocals++;
            }
        }
        System.out.println(limitLocals);
        //code.append(TAB).append(".limit stack ").append(limitLocals).append(NL);
        code.append(TAB).append(".limit locals ").append(limitLocals).append(NL);


        for (var inst : method.getInstructions()) {
            if(this.lastlabel.isEmpty()){}
            else{
                for (int i= 0; i < this.lastlabel.size(); i++){
                    if(method.getLabels().get(this.lastlabel.get(i)).equals(inst)){
                        code.append(this.lastlabel.get(i)+" :"+NL);
                        this.lastlabel.remove(i);
                    }
                }
            }


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

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {

            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if(!operand.getChildren().isEmpty()){
            code.append(loader("a",reg)+NL);
            if(operand.getChildren().get(0).getClass().toString().equals("class org.specs.comp.ollir.LiteralElement")){
                code.append(generators.apply(operand.getChildren().get(0)));
            }
            else{
                var ind = (Operand) operand.getChildren().get(0);
                var reg2 = currentMethod.getVarTable().get(ind.getName()).getVirtualReg();
                code.append(loader("i",reg2)+NL);
            }
            code.append(generators.apply(assign.getRhs()));
            code.append("iastore"+NL);

        }
        else{
            code.append(generators.apply(assign.getRhs()));
            if(operand.getType().toString().substring(operand.getType().toString().length()-2).equals("[]")){
                if (operand.getType().toString().equals("INT32[]")){
                    code.append(storer("a", reg)+NL);
                }
                if (operand.getType().toString().equals("BOOLEAN[]")){
                    code.append(storer("a", reg)+NL);
                }
            }
            else if (operand.getType().toString().equals("INT32") ||operand.getType().toString().equals("BOOLEAN")){
                code.append(storer("i", reg)+NL);
            }
            else if(!assign.getRhs().toString().contains(" NEW ")) {
                code.append(storer("a", reg) + NL);
            }
        }

        //TODO : fazer para os restantes tipos
        return code.toString();

    }

    private String generateSingleOp(SingleOpInstruction singleOp) {

        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        var integerVaue = Integer.parseInt(literal.getLiteral());
        if(integerVaue < 6){
            return "iconst_" + literal.getLiteral() + NL;
        }
        if(integerVaue < maxByte){
            return "bipush " + literal.getLiteral() + NL;
        }
        if(integerVaue < maxShort){
            return "sipush " + literal.getLiteral() + NL;
        }
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        var type = currentMethod.getVarTable().get(operand.getName()).getVarType();
        if(type.toString().equals("INT32") || type.toString().equals("BOOLEAN")){
            return loader("i", reg)+NL;
        }
        else{
            return loader("a", reg)+NL;
        }
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {

        var code = new StringBuilder();
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));
        this.curStackSize--;
        switch (binaryOp.getOperation().getOpType()){
            case ADD :
                code.append("iadd").append(NL);
                break;
            case SUB :
                code.append("isub").append(NL);
                break;
            case MUL :
                code.append("imul").append(NL);
                break;
            case DIV :
                code.append("idiv").append(NL);
                break;
            case OR :
                code.append("ior").append(NL);
                break;
            case ANDB:
                code.append("iand").append(NL);
                break;
            case LTH:
                this.noper = "iflt";
                //code.append("iflt").append(NL);
                break;
            default:
                throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }
        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();


        if(returnInst.getOperand()==null){
            code.append("return");
        }
        else{
            code.append(generators.apply(returnInst.getOperand()));
            code.append("ireturn");
        }

        return code.toString();
    }

}
