package pt.up.fe.comp2024.optimization;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ConstantFoldingVisitor extends AJmmVisitor<Void, String> {
    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private int result = 0;

    private boolean resultBoolean = false;

    private boolean isBoolean = false;

    private boolean modifications = false;

    private Map<String, String> nameValue = new HashMap<String, String>();
    private final OllirExprGeneratorVisitor exprVisitor;

    public ConstantFoldingVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    private String currentMethod;
    @Override
    protected void buildVisitor()
    {
        addVisit(BINARY_EXPR,this::visitBinaryExpr);
        addVisit(VAR_REF,this::visitVarRef);
        addVisit(INTEGER_LITERAL,this::visitIntegerLiteral);
        addVisit(BOOLEAN_LITERAL, this::visitBooleanLiteral);
        addVisit(ASSIGN_STMT,this::visitAssignStmt);
        addVisit(RETURN_STMT,this::visitReturnStmt);
        addVisit(PARENTHESIS_EXPR, this::visitParenthesisExpr);
        addVisit(ARRAY_ACCESS_EXPR,this::visitArrayAcessExpr);
        addVisit(UNARY_EXPR,this::visitUnaryExpr);
        setDefaultVisit(this::defaultVisit);
    }

    public String visitArrayAcessExpr(JmmNode arrayAccessExpr, Void unused)
    {
        var aux = visit(arrayAccessExpr.getChild(1));
        if(!aux.isEmpty())
        {
            JmmNode newNode = new JmmNodeImpl(INTEGER_LITERAL.toString());
            newNode.put("value", aux);
            arrayAccessExpr.getChild(1).replace(newNode);
            modifications = true;
        }
        return aux;
    }


    public String visitUnaryExpr(JmmNode unaryExpr, Void unused)
    {
        var aux = visit(unaryExpr.getChild(0));
        if(aux.isEmpty())
        {
            return "";
        }
        var auxBoolean = false;
        if(aux.equals("true"))
        {
            auxBoolean = true;
        }
        else if(aux.equals("false"))
        {
            auxBoolean = false;
        }
        var returnString = !auxBoolean;
        return returnString+"";
    }
    public String visitParenthesisExpr(JmmNode parenthesisExpr, Void unused)
    {
        return visit(parenthesisExpr.getChild(0));
    }
    public String visitReturnStmt(JmmNode returnStmt, Void unused)
    {
        var aux = visit(returnStmt.getChild(0));
        if(aux.equals(""))
        {
            return "";
        }
        JmmNode newNode = new JmmNodeImpl("");
        if(returnStmt.getChild(0).getKind().equals(BINARY_EXPR.toString()))
        {
            if(!isBoolean) {
                newNode = new JmmNodeImpl(INTEGER_LITERAL.toString());
            }
            else if(isBoolean)
            {
                newNode = new JmmNodeImpl(BOOLEAN_LITERAL.toString());
            }
            newNode.put("value", aux);
            returnStmt.getChild(0).replace(newNode);
            modifications = true;
        }
        else if(returnStmt.getChild(0).getKind().equals(UNARY_EXPR.toString()))
        {
            newNode = new JmmNodeImpl(BOOLEAN_LITERAL.toString());
            newNode.put("value", aux);
            returnStmt.getChild(0).replace(newNode);
            modifications = true;
        }
        return "";
    }
        public String visitAssignStmt(JmmNode assignStmt, Void unused)
        {
            JmmNode newNode = new JmmNodeImpl("");
            if(assignStmt.getChild(0).getKind().equals(BINARY_EXPR.toString()))
            {
                var aux = visit(assignStmt.getChild(0));
                if(aux.equals(""))
                {
                    return "";
                }
                if(!isBoolean) {
                    newNode = new JmmNodeImpl(INTEGER_LITERAL.toString());
                }
                else if(isBoolean)
                {
                    newNode = new JmmNodeImpl(BOOLEAN_LITERAL.toString());
                }
                newNode.put("value", aux);
                assignStmt.getChild(0).replace(newNode);
                modifications = true;
            }
            return "";
        }
        public String visitVarRef(JmmNode varRef, Void unused)
        {
            //get the value of the variable and put it in the map
            if(nameValue.containsKey(varRef.get("value")))
            {
                return nameValue.get(varRef.get("value"));
            }
            return "";
        }

        public String visitIntegerLiteral(JmmNode integerLiteral, Void unused)
        {
            return integerLiteral.get("value");
        }

        public String visitBooleanLiteral(JmmNode booleanLiteral, Void unused)
        {
            return booleanLiteral.get("value");
        }

        public String visitBinaryExpr(JmmNode binaryOp, Void unused)
        {

            var left = visit(binaryOp.getChildren().get(0));
            var right = visit(binaryOp.getChildren().get(1));
            if(left.isEmpty() || right.isEmpty())
            {
                return "";
            }
            boolean leftBoolean = false;
            boolean rightBoolean = false;
            if(left.equals("true"))
            {
                isBoolean = true;
                leftBoolean = true;
            }
            else if(left.equals("false"))
            {
                isBoolean = true;
                leftBoolean = false;
            }
            if(right.equals("true"))
            {
                isBoolean = true;
                rightBoolean = true;
            }
            else if(right.equals("false"))
            {
                isBoolean = true;
                leftBoolean = false;
            }
            //get the result from the operation
            if(left.equals("") || right.equals(""))
            {
                return "";
            }
            if(binaryOp.get("op").equals(">") || binaryOp.get("op").equals("<"))
            {
                isBoolean = true;
            }
            switch(binaryOp.get("op"))
            {
                case "+":
                    result = Integer.parseInt(left) + Integer.parseInt(right);
                    break;
                case "-":
                    result = Integer.parseInt(left) - Integer.parseInt(right);
                    break;
                case "*":
                    result = Integer.parseInt(left) * Integer.parseInt(right);
                    break;
                case "/":
                    result = Integer.parseInt(left) / Integer.parseInt(right);
                    break;
                case "&&":
                    resultBoolean = leftBoolean && rightBoolean;
                    break;
                case "<":
                    resultBoolean = Integer.parseInt(left) < Integer.parseInt(right);
                    break;
                case ">":
                    resultBoolean = Integer.parseInt(left) > Integer.parseInt(right);
                    break;

            }
            if(binaryOp.getParent().getKind().equals(ASSIGN_STMT.toString()))
            {
                if(nameValue.containsKey(binaryOp.getParent().get("value")) && !isBoolean)
                {
                    nameValue.replace(binaryOp.getParent().get("value"), ""+result);
                }
                else if(nameValue.containsKey(binaryOp.getParent().get("value") )&& isBoolean)
                {
                    nameValue.replace(binaryOp.getParent().get("value"),""+resultBoolean);
                }
                else if(!nameValue.containsKey(binaryOp.getParent().get("value")) && !isBoolean)
                {
                    nameValue.put(binaryOp.getParent().get("value"), ""+result);
                }
                else if(!nameValue.containsKey(binaryOp.getParent().get("value"))&& isBoolean)
                {
                    nameValue.put(binaryOp.getParent().get("value"), ""+resultBoolean);
                }
            }
            String resultString = "";
            if(!isBoolean) {
                resultString = "" + result + "";
            }
            else if(isBoolean)
            {
                resultString = ""+ resultBoolean;
            }

            return resultString;
        }


    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return modifications+"";
    }


}
