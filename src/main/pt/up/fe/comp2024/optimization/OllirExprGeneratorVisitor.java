package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {



    int ifCounter = -1;

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    public String currentMethod;
    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    public void setCurrentMethod(String current) {
        currentMethod = current;
    }

    @Override
    protected void buildVisitor() {
        //addVisit(NEG_EXPR, this::visitNegExpr); ainda não está
        addVisit(VAR_REF, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(METHOD_CALL_EXPR, this::visitMethodInvocation);
        addVisit(NEW_OBJECT_EXPR, this::visitNewObjectExpr);
        addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(THIS_EXPR, this::visitThisExpr);
        addVisit(PARENTHESIS_EXPR, this::visitParenthesisExpr);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(UNARY_EXPR,this::visitUnaryExpr);
        addVisit(ARRAY_ACCESS_EXPR,this::visitArrayAccessExpr);
        addVisit(ARRAY_LENGTH_EXPR,this::visitArrayLengthExpr);
        addVisit(ARRAY_CREATION_EXPR, this::visitArrayCreationExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private int getCounterIfStmt(int counter)
    {
        counter += 1;
        return counter;
    }

    private OllirExprResult visitArrayCreationExpr(JmmNode arrayCreationExpr, Void unused)
    {
        StringBuilder computation = new StringBuilder();
        String code = "";
        if(arrayCreationExpr.getNumChildren() == 0)
        {
            var temp = OptUtils.getTemp();
            code = temp + ".array" + ".i32";
            computation.append(temp).append(".array").append(".i32").append(ASSIGN).append(".array").append(".i32").append(" new(array, ").append(arrayCreationExpr.getNumChildren()).append(".i32)").append(".array").append(".i32").append(END_STMT);
        }
        else {
            var aux = visit(arrayCreationExpr.getJmmChild(0));
            var ollirType = OptUtils.toOllirType(TypeUtils.getExprType(arrayCreationExpr.getJmmChild(0), table));
            var temp = OptUtils.getTemp();
            code = temp + ".array" + ollirType;
            computation.append(temp).append(".array").append(ollirType).append(ASSIGN).append(".array").append(ollirType).append(" new(array, ").append(arrayCreationExpr.getNumChildren()).append(".i32)").append(".array").append(ollirType).append(END_STMT);
            for (int i = 0; i < arrayCreationExpr.getNumChildren(); i++) {
                var auxiliar = visit(arrayCreationExpr.getJmmChild(i));
                computation.append(auxiliar.getComputation());
                computation.append(temp).append("[").append(i).append(".i32").append("]").append(ollirType).append(SPACE).append(ASSIGN).append(SPACE).append(ollirType).append(SPACE).append(auxiliar.getCode()).append(END_STMT);
            }
        }

        return new OllirExprResult(code, computation);

    }

    private OllirExprResult visitArrayLengthExpr(JmmNode arrayLengthExpr, Void unused)
    {
        StringBuilder computation = new StringBuilder();
        var index = visit(arrayLengthExpr.getJmmChild(0));
        computation.append(index.getComputation());
        String code = "";
        var temp = OptUtils.getTemp();
        var ollirType = OptUtils.toOllirType(TypeUtils.getExprType(arrayLengthExpr,table));
        code = temp + ollirType;
        computation.append(index.getComputation());
        computation.append(temp).append(ollirType).append(ASSIGN).append(ollirType).append(SPACE).append("arraylength").append("(").append(index.getCode()).append(")").append(ollirType).append(END_STMT);
        return new OllirExprResult(code,computation);
    }

    private OllirExprResult visitArrayAccessExpr(JmmNode arrayAccessExpr, Void unused)
    {
        StringBuilder computation = new StringBuilder();
        var index = visit(arrayAccessExpr.getJmmChild(1));
        var array = visit(arrayAccessExpr.getJmmChild(0));
        computation.append(index.getComputation());
        computation.append(array.getComputation());
        String code = "";
        var temp = OptUtils.getTemp();
        var ollirType = OptUtils.toOllirType(TypeUtils.getExprType(arrayAccessExpr,table));
        code = temp + ollirType;
        if(array.getCode().contains("tmp"))
        {
            computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append(array.getCode()).append("[").append(index.getCode()).append("]").append(ollirType).append(END_STMT);
        }
        else {
            computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append(arrayAccessExpr.getChild(0).get("value")).append("[").append(index.getCode()).append("]").append(ollirType).append(END_STMT);
        }
        return new OllirExprResult(code,computation);
    }
    private OllirExprResult visitUnaryExpr(JmmNode unaryNode, Void unused)
    {
        StringBuilder computation = new StringBuilder();
        var expr = visit(unaryNode.getJmmChild(0));
        computation.append(expr.getComputation());
        String code = "";
        String temp = OptUtils.getTemp();
        String ollirType = OptUtils.toOllirType(TypeUtils.getExprType(unaryNode,table));
        code = temp + ollirType;
        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append(unaryNode.get("value")).append(OptUtils.toOllirType(TypeUtils.getExprType(unaryNode,table))).append(SPACE).append(expr.getCode()).append(END_STMT);
        return new OllirExprResult(code,computation);
    }
    private OllirExprResult visitBoolean(JmmNode booleanNode, Void unused)
    {
        String code = "";
        StringBuilder computation = new StringBuilder();
        String value = booleanNode.get("value");
        if(value.equals("false"))
        {
            code = "0.bool";
        }
        else
        {
            code = "1.bool";
        }
        return new OllirExprResult(code);

    }
    private OllirExprResult visitThisExpr(JmmNode thisNode, Void unused) {
        String code = "";
        StringBuilder computation = new StringBuilder();
        computation.append("this");
        computation.append(".");
        computation.append(table.getClassName());
        code = computation.toString();
        return new OllirExprResult(code);
    }

    private OllirExprResult visitParenthesisExpr(JmmNode node, Void unused)
    {
        StringBuilder computation = new StringBuilder();
        var expr = visit(node.getJmmChild(0));
        computation.append(expr.getComputation());
        String code = expr.getCode();
        return new OllirExprResult(code,computation);
    }

    private OllirExprResult visitNewArrayExpr(JmmNode node, Void unused)
    {
        StringBuilder computation = new StringBuilder();
        String code = "";
        var aux = visit(node.getJmmChild(0));
        var ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0),table));
        var temp = OptUtils.getTemp();
        code = temp + ollirType;
        computation.append(aux.getComputation());
        computation.append(temp).append(ollirType).append(ASSIGN).append(ollirType).append(SPACE).append(aux.getCode()).append(END_STMT);
        temp = OptUtils.getTemp();
        computation.append(temp).append(".array").append(ollirType).append(ASSIGN).append(".array").append(ollirType).append(" new(array,").append(code).append(" )").append(".array").append(ollirType).append(END_STMT);
        code = temp + ".array" + ollirType;
        return new OllirExprResult(code,computation);

    }

    private OllirExprResult visitNewObjectExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        var ollirType = OptUtils.toOllirType(node);
        var temp = OptUtils.getTemp();
        String code = "";
        computation.append(temp);
        computation.append(ollirType);
        computation.append(ASSIGN);
        computation.append(ollirType);
        computation.append(SPACE);
        computation.append("new ");
        computation.append("(");
        computation.append(node.get("value"));
        computation.append(")");
        computation.append(ollirType);
        computation.append(END_STMT);
        code = temp + ollirType; // variable name
        computation.append("invokespecial ( " + code + "," + "\"<init>\""+ ")" + ".V");
        computation.append(END_STMT);
        return new OllirExprResult(code,computation);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code,computation);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();
        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        var temp = OptUtils.getTemp();
        String code = temp + resOllirType;

        if(node.get("op").toString().equals("&&") && !node.getParent().getKind().equals(IF_STMT.toString())){
            computation.append(visit(node.getJmmChild(0)).getComputation());
            computation.append("if(");
            computation.append(visit(node.getChild(0)).getCode());
            this.ifCounter = getCounterIfStmt(this.ifCounter);
            int aux = this.ifCounter;
            computation.append(") goto true_"+aux +";\n");
            //caso seja falso
            computation.append(code+SPACE+ASSIGN+resOllirType+SPACE);
            computation.append("0.bool;\n");
            computation.append("goto end_"+ aux +";\n");
            //caso seja falso
            computation.append("true_"+aux+":\n");
            computation.append(rhs.getComputation());
            computation.append(code+SPACE+ASSIGN+resOllirType+SPACE);
            computation.append(rhs.getCode()+END_STMT);
            computation.append("end_"+aux+":\n");


        }
        else if(node.get("op").toString().equals("&&") && !node.getParent().getKind().equals(IF_STMT.toString())) {
            computation.append(visit(node.getJmmChild(1)).getComputation());
            computation.append("if(");
            computation.append(visit(node.getChild(1)).getCode());
            this.ifCounter = getCounterIfStmt(this.ifCounter);
            int aux = this.ifCounter;
            computation.append(") goto true_" + aux + ";\n");
            //caso seja falso
            computation.append(code + SPACE + ASSIGN + resOllirType + SPACE);
            computation.append("0.bool;\n");
            computation.append("goto end_" + aux + ";\n");
            //caso seja falso
            computation.append("true_" + aux + ":\n");
            computation.append(lhs.getComputation());
            computation.append(code + SPACE + ASSIGN + resOllirType + SPACE);
            computation.append(lhs.getCode() + END_STMT);
            computation.append("end_" + aux + ":\n");
        }
        else {
            computation.append(lhs.getComputation());
            computation.append(rhs.getComputation());
            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append(lhs.getCode()).append(SPACE);

            Type type = TypeUtils.getExprType(node, table);
            computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                    .append(rhs.getCode()).append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        var fields = table.getFields();
        var optionalParams = table.getParametersTry(currentMethod);
        var optionalLocals = table.getLocalVariablesTry(currentMethod);
        var inFields = false;
        var inParams = false;
        var inLocals = false;
        for(Symbol s : fields)
        {
            if(s.getName().equals(node.get("value"))) {
                inFields = true;
            }
        }
        if(optionalParams.isPresent())
        {
            for(Symbol s : optionalParams.get())
            {
                if(s.getName().equals(node.get("value")))
                {
                    inParams = true;
                }
            }
        }
        if(optionalLocals.isPresent())
        {
            for(Symbol s : optionalLocals.get())
            {
                if(s.getName().equals(node.get("value")))
                {
                    inLocals = true;
                }
            }
        }
        if(inLocals || inParams)
        {
            var id = node.get("value");
            Type type = TypeUtils.getExprType(node, table);
            String ollirType = OptUtils.toOllirType(type);
            String code = "";
            if(type.isArray())
            {
                code = id + ".array" + ollirType;
            }
            else
            {
                code = id + ollirType;
            }
            return new OllirExprResult(code,computation);
        }
        else if(inFields)
        {
            var temp = OptUtils.getTemp();
            Type type = TypeUtils.getExprType(node, table);
            String ollirType = OptUtils.toOllirType(type);
            String code = "";
            if(type.isArray())
            {
                code = temp;
                computation.append(temp).append(".array.i32").append(SPACE).append(ASSIGN).append(".array.i32").append(SPACE).append("getfield").append(SPACE).append("(").append("this.").append(table.getClassName()).append(",").append(node.get("value")).append(".array").append(ollirType).append(")").append(".array").append(ollirType).append(END_STMT);
            }
            else
            {
                code = temp + ollirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append("getfield").append(SPACE).append("(").append("this.").append(table.getClassName()).append(",").append(node.get("value")).append(ollirType).append(")").append(ollirType).append(END_STMT);

            }
            return new OllirExprResult(code,computation);
        }
        return new OllirExprResult("");
    }
    private OllirExprResult visitMethodInvocation(JmmNode node, Void unused) {
        String code = "";
        int indexVarArgsStart = 0;
        String tempFixed = "";
        StringBuilder computation = new StringBuilder();
        String typeFunction = "";
        StringBuilder params = new StringBuilder(); // to store the parameters
        boolean hasArgs = false;
        boolean varArgs = false;
        boolean hasvarArgsEmpty = false;
        StringBuilder arraysCode = new StringBuilder();
        StringBuilder varArgsEmpty  = new StringBuilder();
        Type returnType = new Type("int", false);
        var ollirType = ".V";
        var lhs = visit(node.getJmmChild(0));
        computation.append(lhs.getComputation());
        if(!node.getJmmChild(0).getKind().equals(THIS_EXPR.toString()))
        {
            if(node.getJmmChild(0).hasAttribute("value"))
            {
                if(table.getImports().contains(node.getJmmChild(0).get("value")))
                {
                    typeFunction  ="invokestatic";
                }
                else
                {
                    typeFunction = "invokevirtual";
                }
            }
            else
            {
                typeFunction = "invokevirtual";
            }
        }
        else
        {
            typeFunction = "invokevirtual";
        }

        if(typeFunction.equals("invokevirtual")) {
            //check the params from the method that I am calling if it is possible
            if(table.getMethods().contains(node.get("value")))
            {
                var paramsAux = table.getParameters(node.get("value"));
                //iterate over the parameters of the method
                for(int i = 0; i < paramsAux.size(); i++) {
                    //check if there is i + 1 child in the node
                    if(node.getNumChildren() - 1 < i + 1)
                    {
                        //varArgs is empty
                        var tempAux = OptUtils.getTemp();
                        tempFixed = tempAux;
                        ollirType = OptUtils.toOllirType(paramsAux.get(i).getType());
                        computation.append(tempAux).append(".array").append(ollirType).append(SPACE).append(ASSIGN).append(".array").append(ollirType).append(SPACE).append("new").append(SPACE).append("(").append("array").append(",").append("0.i32").append(")").append(".array").append(ollirType).append(END_STMT);
                        hasvarArgsEmpty = true;
                        varArgsEmpty.append(tempAux).append(".array").append(ollirType);
                    }
                    else if (paramsAux.get(i).getType().isArray()) {
                            if (!node.getJmmChild(i + 1).getKind().equals(ARRAY_CREATION_EXPR.toString()) && !node.getJmmChild(i + 1).getKind().equals(NEW_ARRAY_EXPR.toString()) && !node.getJmmChild(i + 1).getKind().equals(VAR_REF.toString())) {
                                indexVarArgsStart = i + 1;
                                varArgs = true;
                                var temp2 = OptUtils.getTemp();
                                tempFixed = temp2;
                                ollirType = OptUtils.toOllirType(paramsAux.get(i).getType());
                                arraysCode.append(temp2).append(".array").append(ollirType).append(",");
                                computation.append(temp2).append(".array").append(ollirType).append(ASSIGN).append(".array").append(ollirType).append(SPACE).append("new(array, ");
                                //append the rest of the childs that are not varRef
                                var total = -1;
                                for (int m = 0; m < node.getNumChildren(); m++) {
                                    if (!node.getJmmChild(m).getKind().equals(VAR_REF.toString())) {
                                        total++;
                                    }
                                }
                                if (node.getParent().getKind().equals(ARRAY_CREATION_EXPR.toString()) && node.getParent().getParent().getKind().equals(METHOD_CALL_EXPR.toString())) {
                                    total = total - 1;
                                }
                                computation.append(total).append(".i32 )");
                                if (!ollirType.contains(".array")) {
                                    computation.append(".array");
                                }
                                computation.append(ollirType).append(END_STMT);
                                for (int j = 0; j < node.getNumChildren() - 1 - i; j++) {
                                    var aux = visit(node.getJmmChild(j + 1 + i));
                                    if (node.getJmmChild(j + 1 + i).getKind().equals(ARRAY_ACCESS_EXPR.toString())) {
                                        computation.append(aux.getComputation());
                                    }
                                    computation.append(temp2).append("[").append(j).append(".i32").append("]").append(ollirType).append(ASSIGN).append(ollirType).append(SPACE).append(aux.getCode()).append(END_STMT);
                                }
                        }
                    }
                }
            }
            for (int i = 1; i < node.getNumChildren(); i++) {
                var rhs = visit(node.getJmmChild(i));
                computation.append(rhs.getComputation());
                if(!varArgs) {
                    params.append(rhs.getCode());
                    if (i != node.getNumChildren() - 1) {
                        params.append(",");
                    }
                }
                else
                {
                    if(i < indexVarArgsStart )
                    {
                        params.append(rhs.getCode());
                        if(!rhs.getCode().isEmpty()){
                            if (i != node.getNumChildren() - 1) {
                                params.append(",");
                            }

                        }
                    }
                    else
                    {
                        //remove the last comma to the arraysCode
                        arraysCode.deleteCharAt(arraysCode.length() - 1);
                        params.append(arraysCode);
                        break;
                    }
                }

                hasArgs = true;
            }
            if(varArgsEmpty.length() > 0)
            {
                if(!params.isEmpty())
                {
                    params.append(",");
                }
                params.append(varArgsEmpty);
            }
            if(node.getParent().getKind().equals(EXPR_STMT.toString()))
            {
                if(table.getMethods().contains(node.get("value")))
                {
                    returnType = table.getReturnType(node.get("value"));
                    ollirType = OptUtils.toOllirType(returnType);
                }
            }
            else if(node.getParent().getKind().equals(ASSIGN_STMT.toString()))
            {
                var aux = TypeUtils.getExprType(node.getParent(),table);
                var temp = OptUtils.getTemp();
                ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node.getParent(),table));
                if(aux.isArray())
                {
                    ollirType = ".array" + ollirType;
                    code = temp;
                    computation.append(code).append(ollirType).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
                }

                else
                {
                    code = temp + ollirType;
                    computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
                }
            }
            else
            {
                var temp = OptUtils.getTemp();
                ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node, table));
                code = temp + ollirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
            }
            computation.append(typeFunction).append("(").append(lhs.getCode()).append(",").append("\"").append(node.get("value")).append("\"");
            if(!params.isEmpty())
            {
                computation.append(",").append(params);
            }
            computation.append(")").append(ollirType).append(END_STMT);
        }
        else {
            for(int i = 1; i < node.getNumChildren();i++)
            {

                var rhs = visit(node.getJmmChild(i));
                computation.append(rhs.getComputation());
                if(!rhs.getCode().contains(".i32"))
                {
                    params.append(rhs.getCode()).append(".array.i32");
                }
                else {
                    params.append(rhs.getCode());
                }
                if(i != node.getNumChildren() - 1)
                {
                    params.append(",");
                }
            }
            if(node.getParent().getKind().equals(EXPR_STMT.toString()))
            {
            }
            //cannot return void
            else if(node.getParent().getKind().equals(RETURN_STMT.toString()))
            {
                var temp = OptUtils.getTemp();
                ollirType = OptUtils.toOllirType(table.getReturnType(currentMethod));
                code = temp + ollirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
            }
            else if(node.getParent().getKind().equals(ARRAY_ASSIGN_STMT.toString()))
            {
                var temp = OptUtils.getTemp();
                ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node.getParent(),table));
                code = temp + ollirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
            }
            else if(!node.getParent().getKind().equals(ASSIGN_STMT.toString()))
            {
                var temp = OptUtils.getTemp();// get a new temp
                ollirType = ".V";
                code = temp + ollirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
            }

            else if(node.getParent().getKind().equals(ASSIGN_STMT.toString()))
            {
                var temp = OptUtils.getTemp();
                returnType = TypeUtils.getExprType(node.getParent(),table);
                ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node.getParent(),table));
                if(returnType.isArray())
                {
                    ollirType = ".array" + ollirType;
                }
                code = temp + ollirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
            }
            computation.append(typeFunction).append("(").append(node.getJmmChild(0).get("value")).append(",").append("\"").append(node.get("value")).append("\"");
            if(!params.isEmpty())
            {
                computation.append(",").append(params);
            }
            computation.append(")").append(ollirType).append(END_STMT);


        }
        return new OllirExprResult(code,computation);
    }






    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
