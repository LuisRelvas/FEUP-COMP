package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ConstantPropagationVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";

    //create a map to store the values of the variables
    private Map<String, Integer> nameValue = new HashMap<String, Integer>();

    private int result = 0;


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public ConstantPropagationVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    private String currentMethod;
    @Override
    protected void buildVisitor()
    {
        addVisit(METHOD_DECL,this::visitMethodDecl);
        addVisit(ASSIGN_STMT,this::visitAssignStmt);
        addVisit(BINARY_EXPR,this::visitBinaryExpr);
        addVisit(VAR_REF, this::visitVarRef);
        addVisit(INTEGER_LITERAL,this::visitIntegerLiteral);
        setDefaultVisit(this::defaultVisit);
    }


    public String visitAssignStmt(JmmNode assignStmt, Void unused)
    {
        //in here I am going to save all the values from the assignments and put them in a map to be more easy to access them
        var varName = assignStmt.get("value");
        var varValue = visit(assignStmt.getChildren().get(0));

        //append to the map the varName and the varValue
        if(!assignStmt.getChild(0).getKind().equals(BINARY_EXPR.toString()))
        {
            nameValue.put(varName, Integer.parseInt(varValue));
        }
        return "";
    }
    public String visitMethodDecl(JmmNode methodDecl, Void unused)
    {
        currentMethod = methodDecl.get("methodName");
        for (JmmNode child : methodDecl.getChildren()) {
            visit(child);
        }
        return "";
    }


    public String visitVarRef(JmmNode varRef, Void unused)
    {
        //get the value of the varRef from the map
        if(nameValue.containsKey(varRef.get("value")))
        {
            //criar um novo no com o valor da variavel e um kind diferente
            JmmNode newNode = new JmmNodeImpl(INTEGER_LITERAL.toString());
            newNode.put("value", Integer.toString(nameValue.get(varRef.get("value"))));
            //dar replacement na AST com o valor da variavel
            varRef.replace(newNode);
            return Integer.toString(nameValue.get(varRef.get("value")));
        }
        else
        {
            return varRef.get("value");
        }
    }

    public String visitIntegerLiteral(JmmNode integerLiteral, Void unused)
    {
        return integerLiteral.get("value");
    }


    public String visitBinaryExpr(JmmNode binaryOp, Void unused)
    {
        var left = visit(binaryOp.getChildren().get(0));
        var right = visit(binaryOp.getChildren().get(1));

        return left + binaryOp.get("op") + right;
    }


    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}