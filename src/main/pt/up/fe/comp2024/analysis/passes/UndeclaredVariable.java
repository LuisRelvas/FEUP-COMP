package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.VAR_REF, this::visitVarRef);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
        addVisit(Kind.METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(Kind.WHILE_STMT,this::visitWhileStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        TypeUtils.setCurrentMethod(currentMethod);
        return null;
    }

    private Void visitWhileStmt(JmmNode whileExpr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(whileExpr.getChild(0),table);
        if(!type.getName().equals(Kind.BOOLEAN_LITERAL.toString()) || type.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the condition of the while statement", null));
        }
        return null;
    }

    private Void visitArrayAssignStmt(JmmNode arrayAssignStmt, SymbolTable table)
    {
        Type typeArray = new Type("int",false);
        String array = arrayAssignStmt.get("ID");
        for(Symbol s: table.getLocalVariables(currentMethod))
        {
            if(s.getName().equals(array))
            {
                typeArray = new Type(s.getType().getName(),false);
            }
        }
        for(Symbol s: table.getParameters(currentMethod))
        {
            if(s.getName().equals(array))
            {
                typeArray = new Type(s.getType().getName(),false);
            }
        }
        for(Symbol s: table.getFields())
        {
            if(s.getName().equals(array))
            {
                typeArray = new Type(s.getType().getName(),false);
            }
        }
        // First Child of ArrayAssign must be the Index
        Type typeIndex = TypeUtils.getExprType(arrayAssignStmt.getChild(0), table);
        if(!typeIndex.getName().equals("int") || typeIndex.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC,0,0,"Type mismatch in the Assignment of the Array " + array, null));
        }
        for(int i = 1; i < arrayAssignStmt.getNumChildren(); i++)
        {
            Type type = TypeUtils.getExprType(arrayAssignStmt.getChild(i), table);
            if(!type.equals(typeArray))
            {
                addReport(Report.newError(Stage.SEMANTIC,0,0,"Type mismatch in the Assignment of the Array " + array, null));
            }
        }
        return null;
    }

    private Void visitBinaryExpr(JmmNode expr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(expr, table);
        JmmNode leftNode = expr.getChildren().get(0);
        JmmNode rightNode = expr.getChildren().get(1);

        // Check if leftNode or rightNode is a BinaryExpr
        if (leftNode.getKind().equals("BinaryExpr")) {
            visitBinaryExpr(leftNode, table);
        }
        if (rightNode.getKind().equals("BinaryExpr")) {
            visitBinaryExpr(rightNode, table);
        }

        Type leftType = TypeUtils.getExprType(leftNode, table);
        Type rightType = TypeUtils.getExprType(rightNode, table);
        if(!leftType.getName().equals("int") || !rightType.getName().equals("int"))
        {
            addReport(Report.newError(Stage.SEMANTIC,0,0,"Type mismatch in the Binary Expression " + rightType.getName() + " with " + leftType.getName(), null));
        }
        if(!leftType.equals(rightType))
        {
            addReport(Report.newError(Stage.SEMANTIC,0,0,"Type mismatch in the Binary Expression " + rightType.getName() + " with " + leftType.getName(), null));
        }
        return null;
    }

    private Void visitIfStmt(JmmNode ifStmt, SymbolTable table)
    {
        JmmNode condition = ifStmt.getChild(0);
        Type type;
        if(condition.getKind().equals("BinaryExpr"))
        {
            type = TypeUtils.getExprType(condition,table);
            if(!type.getName().equals("boolean"))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the condition of the if statement", null));
            }
        }
        return null;
    }
    private Void visitVarRef(JmmNode expr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(expr,table);
        if(type == null)
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Variable " + expr.get("value") + " not declared", null));}
        return null;
    }

    private Void visitMethodCallExpr(JmmNode expr, SymbolTable table)
    {
        var imports = table.getImports();
        var extended = table.getSuper();
        Type type = TypeUtils.getExprType(expr.getChild(0), table);
        // verify if the method is declared
        if(type.getName().equals(table.getClassName()) && (extended.isEmpty() || imports.isEmpty()))
        {
            if(!table.getMethods().contains(expr.get("value")))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method " + expr.get("value") + " not declared", null));
            }
            // check if the parameters are correct
            if(table.getParameters(expr.get("value")).isEmpty())
            {
                if(expr.getNumChildren() > 1)
                {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method " + expr.get("value") + " has no parameters", null));
                }
            }
            else
            {
                var paramList = table.getParameters(expr.get("value"));
                for (int i = 0; i < paramList.size(); i++) {
                    if (!paramList.get(i).getType().equals(TypeUtils.getExprType(expr.getChild(i + 1), table))) {
                        addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the parameters of the method " + expr.get("value"), null));
                    }
                }
            }
        }


        return null;
    }

    private Void visitAssignStmt(JmmNode assign, SymbolTable table)
    {
        String varAssigned = assign.get("ID");
        // Check if there is a BinaryExpr
        JmmNode expr = assign.getChild(0);
        var extended = table.getSuper();
        var imports = table.getImports();
        Type typeExpr = TypeUtils.getExprType(expr, table);
        Type typeAssign = TypeUtils.getExprType(assign, table);
        if(expr.getKind().equals(Kind.METHOD_CALL_EXPR.toString()))
        {
            if(expr.get("value").equals("varargs"))
            {
                for(int i = 0; i < expr.getNumChildren(); i++)
                {
                    if(!TypeUtils.getExprType(expr.getChild(i),table).equals(typeAssign))
                    {
                        addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the assignment of var " + varAssigned, null));
                    }
                }
            }
        }
        else if(!typeExpr.equals(typeAssign) && (!imports.contains(typeExpr.getName()) && !imports.contains(typeAssign.getName())))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the assignment of var " + varAssigned, null));
        }
        else if(imports.contains(typeExpr.getName()) && !imports.contains(typeAssign.getName()))
        {
            if(!extended.contains(typeExpr.getName()))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the assignment of " + varAssigned + " because it isn't extended.", null));
            }
        }
        else if (!imports.contains(typeExpr.getName()) && imports.contains(typeAssign.getName()))
        {
            if(!extended.contains(typeAssign.getName()))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the assignment of " + varAssigned + " because it isn't extended.", null));
            }
        }


        return null;
    }

    private Void visitArrayAccessExpr(JmmNode expr, SymbolTable table)
    {
        JmmNode array = expr.getChild(0);
        JmmNode index = expr.getChild(1);
        Type typeArray = TypeUtils.getExprType(array,table);
        Type typeIndex = TypeUtils.getExprType(index,table);

        if(!typeArray.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Variable " + array.get("value") + " is not an array", null));
        }
        if(!typeIndex.getName().equals("int"))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0 ,"Variable" + index.get("value") + " is not an integer", null));
        }
        return null;
    }

    private Void visitReturnStmt(JmmNode expr, SymbolTable table)
    {
        Type typeMethod = table.getReturnType(currentMethod);
        JmmNode childExpr = expr.getChild(0);
        var returnType = table.getReturnType(currentMethod);
        if(childExpr.getKind().equals(Kind.METHOD_CALL_EXPR.toString()))
        {
            // The first child of childExpr must be the class name
            var k = TypeUtils.getExprType(childExpr.getChild(0),table);
            //k must be a valid class name
            if (table.getImports().contains(k.getName()))
            {
                return null; // Assume the method is declared by the import
            }
            else if (!k.getName().equals(table.getClassName())) {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Class not declared " + k.getName(), null));
            }
            else
            {
                var typeMethodCalled = table.getReturnType(childExpr.get("value"));
                if(!typeMethodCalled.equals(returnType) && !table.getImports().contains(typeMethodCalled.getName()))
                {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the return statement", null));
                }
            }
        }
        if(childExpr.getKind().equals(Kind.BINARY_EXPR.toString()))
        {
            JmmNode leftNode = childExpr.getChild(0);
            JmmNode rightNode = childExpr.getChild(1);
            var returnExprTypes = traverseAst(leftNode,rightNode,table);
            for(int i = 0; i < returnExprTypes.size(); i++)
            {
                if(!returnType.equals(returnExprTypes.get(i)))
                {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the return statement " + returnExprTypes.get(i).getName() + " with " + returnType.getName(), null));
                }
            }
        }
        if(childExpr.getKind().equals(Kind.ARRAY_ACCESS_EXPR.toString()))
        {
            if(returnType.isArray())
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the return statement", null));

            }
            Type type = TypeUtils.getExprType(childExpr.getChild(0),table);
            if(!type.getName().equals(returnType.getName())) {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the return statement", null));
            }
        }
        if(childExpr.getKind().equals(Kind.VAR_REF.toString()))
        {
            Type type = TypeUtils.getExprType(childExpr,table);
            if(!type.equals(returnType))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the return statement", null));
            }
        }
        return null;
    }

    private List<Type> traverseAst(JmmNode leftNode, JmmNode rightNode, SymbolTable table) {
        List<Type> typesfinal = new ArrayList<>();

        if (leftNode.getKind().equals(Kind.BINARY_EXPR.toString())) {
            typesfinal.addAll(traverseAst(leftNode.getChild(0), leftNode.getChild(1), table));
        }
        else if (leftNode.getKind().equals(Kind.VAR_REF.toString())) {
            Type typeLeft = TypeUtils.getExprType(leftNode, table);
            typesfinal.add(typeLeft);
        }
        else if (leftNode.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
            Type typeLeft = new Type("int", false);
            typesfinal.add(typeLeft);
        }
        else if(leftNode.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            Type typeLeft = new Type("boolean", false);
            typesfinal.add(typeLeft);
        }

        if (rightNode.getKind().equals(Kind.BINARY_EXPR.toString())) {
            typesfinal.addAll(traverseAst(rightNode.getChild(0), rightNode.getChild(1), table));
        }
        else if (rightNode.getKind().equals(Kind.VAR_REF.toString())) {
            Type typeRight = TypeUtils.getExprType(rightNode, table);
            typesfinal.add(typeRight);
        }
        else if (rightNode.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
            Type typeRight = new Type("int", false);
            typesfinal.add(typeRight);
        }
        else if(rightNode.getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
            Type typeRight = new Type("boolean", false);
            typesfinal.add(typeRight);
        }

        return typesfinal;
    }




}
