package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

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
        addVisit(THIS_EXPR, this::visitThisExpr);
        addVisit(PARENTHESIS_EXPR, this::visitParenthesisExpr);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(UNARY_EXPR,this::visitUnaryExpr);
        setDefaultVisit(this::defaultVisit);
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
        computation.append("invokespecial ( " + code + "," + "\"\""+ ")" + ".V");
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

            computation.append(lhs.getComputation());
            computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        var temp = OptUtils.getTemp();
        System.out.println(temp);
        String code = temp + resOllirType;
        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

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
            String code = id + ollirType;
            return new OllirExprResult(code,computation);
        }
        else if(inFields)
        {
            var temp = OptUtils.getTemp();
            Type type = TypeUtils.getExprType(node, table);
            String ollirType = OptUtils.toOllirType(type);
            String code = temp + ollirType;
            computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append("getfield").append(SPACE).append("(").append("this").append(",").append(node.get("value")).append(ollirType).append(")").append(ollirType).append(END_STMT);
            return new OllirExprResult(code,computation);
        }
        return new OllirExprResult("");
    }
    private OllirExprResult visitMethodInvocation(JmmNode node, Void unused) {
        String code = "";
        StringBuilder computation = new StringBuilder();
        String typeFunction = "";
        StringBuilder params = new StringBuilder(); // to store the parameters
        boolean hasArgs = false;
        Type returnType = new Type("int", false);
        var ollirType = "";
        var lhs = visit(node.getJmmChild(0));
        computation.append(lhs.getComputation());
        if(!node.getJmmChild(0).getKind().equals(THIS_EXPR.toString()))
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

        if(typeFunction.equals("invokevirtual")) {
            for (int i = 1; i < node.getNumChildren(); i++) {
                var rhs = visit(node.getJmmChild(i));
                computation.append(rhs.getComputation());
                params.append(rhs.getCode());
                if(i != node.getNumChildren() - 1)
                {
                    params.append(",");
                }

                hasArgs = true;
            }
            if((node.getParent().getKind().equals(EXPR_STMT.toString()) && node.getParent().getParent().getKind().equals(ASSIGN_STMT.toString()))|| node.getParent().getKind().equals(ASSIGN_STMT.toString()) || node.getChild(0).getKind().equals(THIS_EXPR.toString())) {
                var temp = OptUtils.getTemp();
                ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node, table));
                code = temp + ollirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append(typeFunction).append("(").append(lhs.getCode()).append(",").append("\"").append(node.get("value")).append("\"");
            }
            else
            {
                ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node, table));
                computation.append(code).append(SPACE).append(typeFunction).append("(").append(lhs.getCode()).append(",").append("\"").append(node.get("value")).append("\"");
            }
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


                params.append(rhs.getCode());
                if(i != node.getNumChildren() - 1)
                {
                    params.append(",");
                }
            }
            if(node.getParent().getKind().equals(EXPR_STMT.toString()))
            {
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
                ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node.getParent(),table));
                code = temp + ollirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
            }
            computation.append(typeFunction).append("(").append(node.getJmmChild(0).get("value")).append(",").append("\"").append(node.get("value")).append("\"");
            if(!params.isEmpty())
            {
                computation.append(",").append(params);
            }
            computation.append(")").append(".V").append(END_STMT);


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
