package pt.up.fe.comp2024.analysis.passes;
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;

import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class InvalidOperations extends AnalysisVisitor {

    private String currentMethod;


    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("ReturnStmt", this::visitReturnStmt);
        addVisit("IfStmt", this::visitConditionStmt);
        addVisit("AssignStmt", this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        return null;
    }

    private Void visitConditionStmt(JmmNode conditionStmt, SymbolTable table)
    {
        JmmNode root = conditionStmt;
        JmmNode leftNode = conditionStmt.getChild(0);
        JmmNode rightNode = conditionStmt.getChild(1);
        List<String> types = new ArrayList<>();
        if(root.getChild(0).getKind().equals("BinaryExpr"))
        {
            types = getNodeBinaryExpr(leftNode.getChild(0),leftNode.getChild(1),table.getLocalVariables(currentMethod));
        }
        else
        {
            types.add(getExprType(leftNode,table.getLocalVariables(currentMethod)));
        }

        if(!checkTypes(types)){
            // Create error report
            var message = String.format("Invalid Operands in the Condition Stmt", types.get(0), types.get(1));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(conditionStmt),
                    NodeUtils.getColumn(conditionStmt),
                    message,
                    null)
            );
        }
        for(var i: types){
            if(i.equals("boolean"))
            {
                continue;
            }
            else
            {
                // Create error report
                var message = String.format("The Result of the Expr in the Condition Stmt must be Boolean" , types.get(0), types.get(1));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(conditionStmt),
                        NodeUtils.getColumn(conditionStmt),
                        message,
                        null)
                );
            }
        }
        return null;
    }

    private boolean checkIfImported(String typeAssign, String type, SymbolTable table)
    {
        List<String> imports = new ArrayList<>();
        imports = table.getImports();
        String extended = table.getSuper();

        for(String s : imports)
        {
            if(s.equals(typeAssign) && extended.equals(typeAssign) )
            {
                System.out.println("Found");
                return true;
            }
        }
        System.out.println("Not Found");
        return false;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table)
    {
        List<Symbol> locals = new ArrayList<>();
        String typeAssign = "";
        locals = table.getLocalVariables(currentMethod);
        String varName = assignStmt.get("ID");
        for(Symbol s: locals)
        {
           if(s.getName().equals(varName))
           {
               typeAssign = s.getType().getName();
           }
        }
        if(assignStmt.getChild(0).getKind().equals("BinaryExpr"))
        {
            JmmNode leftNode = assignStmt.getChild(0).getChild(0);
            JmmNode rightNode = assignStmt.getChild(0).getChild(1);
            List<String> types = getNodeBinaryExpr(leftNode, rightNode, locals);
            // Check if all the types are equal to the first type
            if(!checkTypes(types))
            {
                // Create error report
                var message = String.format("Invalid Operands in the Assign Stmt", typeAssign, types.get(0));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null)
                );
            }
            if(!types.get(0).equals(typeAssign))
            {
                // Create error report
                var message = String.format("Invalid Types between the Assign Variable and the Expression", typeAssign, types.get(0));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null)
                );
            }
        }
        else
        {
            String type = getExprType(assignStmt.getChild(0),locals);
            if(!type.equals(typeAssign))
            {
                if(!checkIfImported(typeAssign, type, table)){
                // Create error report
                var message = String.format("Invalid Operands in the Assign Stmt", typeAssign, type);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null)
                    );
                }
            }
        }

        return null;

    }

    private String getExprType(JmmNode expr, List<Symbol> locals)
    {
        if(expr.getKind().equals("IntegerExpr"))
        {
            return "int";
        }
        if(expr.getKind().equals("BooleanExpr"))
        {
            return "boolean";
        }

        if(expr.getKind().equals("VarRef"))
        {
            String varName = expr.get("value");
            for(Symbol s : locals)
            {
                if(s.getName().equals(varName))
                {
                    if(s.getType().isArray())
                    {
                        return String.format("%s[]", s.getType().getName());
                    }
                    else
                    {
                        return s.getType().getName();
                    }
                }
            }
        }
        if(expr.getKind().equals("ArrayAccessExpr"))
        {
            String varName = expr.getChild(0).get("value");
            for(Symbol s : locals)
            {
                if(s.getName().equals(varName))
                {
                    if(s.getType().isArray())
                    {
                        if(expr.getChild(1).getKind().equals("IntegerExpr"))
                        {
                            return s.getType().getName();
                        }
                        else if(expr.getChild(1).getKind().equals("VarRef"))
                        {
                            String arrayIndex = expr.getChild(1).get("value");
                            for(Symbol s2 : locals)
                            {
                                if(s2.getName().equals(arrayIndex))
                                {
                                    if(s2.getType().getName().equals("int"))
                                    {
                                        return s.getType().getName();
                                    }
                                    else
                                    {
                                        var message = String.format("Array index is not an integer", arrayIndex);
                                        addReport(Report.newError(
                                                Stage.SEMANTIC,
                                                NodeUtils.getLine(expr),
                                                NodeUtils.getColumn(expr),
                                                message,
                                                null)
                                        );
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        return String.format("%s[]", s.getType().getName());
                    }
                }
            }
        }
        if(expr.getKind().equals("NewObjectExpr"))
        {
            return expr.get("value");
        }
        return null;
    }

    private List<String> getNodeBinaryExpr(JmmNode leftNode, JmmNode rightNode, List<Symbol> locals)
    {
        List<String> save = new ArrayList<>(); //save the types of the left and right nodes
        if(leftNode.getKind().equals("BinaryExpr"))
        {
            save.addAll(getNodeBinaryExpr(leftNode.getChild(0),leftNode.getChild(1),locals));
        }
        else
        {
            save.add(getExprType(leftNode, locals));
        }
        if(rightNode.getKind().equals("BinaryExpr"))
        {
            save.addAll(getNodeBinaryExpr(rightNode.getChild(0),rightNode.getChild(1),locals));
        }
        else
        {
            save.add(getExprType(rightNode, locals));
        }

        return save;
    }

    private Boolean checkTypes(List<String> types)
    {
        for (int i = 1; i < types.size(); i++)
        {
            if (!types.get(0).equals(types.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        List<Symbol> locals = new ArrayList<>();
        locals = table.getLocalVariables(currentMethod);
        var returnType = table.getReturnType(currentMethod);
        List<String> types = new ArrayList<>();
        if (returnStmt.getChild(0).getKind().equals("BinaryExpr")) {
            JmmNode leftNode = returnStmt.getChild(0).getChild(0);
            JmmNode rightNode = returnStmt.getChild(0).getChild(1);
            types = getNodeBinaryExpr(leftNode, rightNode, locals);
            if(!checkTypes(types))
            {
                // Create error report
                var message = String.format("Invalid Operands in the Return Stmt", returnType.getName(), types.get(0));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(returnStmt),
                        NodeUtils.getColumn(returnStmt),
                        message,
                        null)
                );
            }
        } else {
            types.add(getExprType(returnStmt.getChild(0), locals));
            if(!returnType.getName().equals(types.get(0)))
            {
                // Create error report
                var message = String.format("Return type is different from the method return type", returnType.getName(), types.get(0));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(returnStmt),
                        NodeUtils.getColumn(returnStmt),
                        message,
                        null)
                );
            }
        }
        return null;
    }
}
