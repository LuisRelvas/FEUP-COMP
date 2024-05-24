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
import pt.up.fe.comp.TestUtils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class InvalidTypes extends AnalysisVisitor {

    private String currentMethod;

    private Boolean isStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.ARRAY_CREATION_EXPR, this::visitArrayCreationExpr);
        addVisit(Kind.ARRAY_LENGTH_EXPR, this::visitArrayLengthExpr);


    }

    private Void visitArrayLengthExpr(JmmNode arrayLengthExpr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(arrayLengthExpr.getChild(0),table);
        if(!type.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Variable " + arrayLengthExpr.getJmmChild(0).get("value") + " is not an array", null));
            return null;
        }
        return null;
    }
    private Void visitArrayCreationExpr(JmmNode arrayCreationExpr, SymbolTable table)
    {
        if(arrayCreationExpr.getNumChildren() == 0)
        {
            return null;
        }
        Type type = TypeUtils.getExprType(arrayCreationExpr.getChild(0),table);

        for(int i = 1; i < arrayCreationExpr.getNumChildren(); i++)
        {
            if(!type.equals(TypeUtils.getExprType(arrayCreationExpr.getChild(i),table)))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the array creation expression", null));
                return null;
            }
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        isStatic = NodeUtils.getBooleanAttribute(method, "isStatic", "false");
        if(currentMethod.equals("main") && isStatic.equals(false))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Main method not declared static.",null));
            return null;
        }
        if(!currentMethod.equals("main") && isStatic.equals(true))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method " + currentMethod + " declared static.",null));
            return null;
        }
        TypeUtils.setCurrentMethod(currentMethod);
        TypeUtils.setStatic(NodeUtils.getBooleanAttribute(method, "isStatic", "false"));
        return null;
    }

    private Void visitWhileStmt(JmmNode whileExpr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(whileExpr.getChild(0),table);
        if(!type.getName().equals("boolean") || type.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the condition of the while statement", null));
            return null;
        }
        return null;
    }

    private Void visitArrayAssignStmt(JmmNode arrayAssignStmt, SymbolTable table)
    {
        String array = arrayAssignStmt.get("value");
        Type typeArray = TypeUtils.getExprType(arrayAssignStmt, table);
        // First Child of ArrayAssign must be the Index
        Type typeIndex = TypeUtils.getExprType(arrayAssignStmt.getChild(0), table);
        if(!typeIndex.getName().equals("int") || typeIndex.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC,0,0,"Type mismatch in the Assignment of the Array " + array, null));
            return null;
        }
        for(int i = 1; i < arrayAssignStmt.getNumChildren(); i++)
        {
            Type type = TypeUtils.getExprType(arrayAssignStmt.getChild(i), table);
            if(!table.getImports().contains(type.getName())) {
                if (!type.getName().equals(typeArray.getName())) {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the Assignment of the Array " + array, null));
                    return null;
                }
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
        // Permitimos as operações binarias entre dois elementos com o mesmo tipo exceto para arrays
        if(!leftType.equals(rightType) || (leftType.isArray()) || (rightType.isArray()) )
        {
            addReport(Report.newError(Stage.SEMANTIC,0,0,"Type mismatch in the Binary Expression " + rightType.getName() + " with " + leftType.getName(), null));
            return null;
        }
        if(!leftType.equals(rightType))
        {
            addReport(Report.newError(Stage.SEMANTIC,0,0,"Type mismatch in the Binary Expression " + rightType.getName() + " with " + leftType.getName(), null));
            return null;
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
                return null;
            }
        }
        return null;
    }

    private Void visitAssignStmt(JmmNode assign, SymbolTable table) {
        String varAssigned = assign.get("value");
        var lhsType = TypeUtils.getExprType(assign,table);
        var rhsType = TypeUtils.getExprType(assign.getJmmChild(0),table);
        if(!lhsType.equals(rhsType))
        {
            //if two classes are imported we assume that it is correct the assignment
            if(table.getImports().contains(lhsType.getName()) && table.getImports().contains(rhsType.getName()))
            {
                return null;
            }
            //check the class that is imported and the class defined
            else if(table.getImports().contains(lhsType.getName()) && table.getClassName().equals(rhsType.getName()))
            {
                if(table.getSuper().equals(lhsType.getName()))
                {
                    return null;
                }
                else
                {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the assignment of the variable " + varAssigned, null));
                    return null;
                }
            }
            else if(table.getImports().contains(rhsType.getName()) && table.getClassName().equals(lhsType.getName()))
            {
                if(table.getSuper().equals(rhsType.getName()))
                {
                    return null;
                }
                else
                {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the assignment of the variable " + varAssigned, null));
                    return null;
                }
            }
            else if(table.getImports().contains(lhsType.getName()) && !rhsType.getName().equals(table.getClassName()))
            {
                return null;
            }
            else if(table.getImports().contains(rhsType.getName()) && !lhsType.getName().equals(table.getClassName()))
            {
                return null;
            }
            else if(rhsType.getName().equals(table.getClassName()) && !table.getSuper().isEmpty())
            {
                return null;
            }
            else if(lhsType.getName().equals(table.getClassName()) && !table.getSuper().isEmpty())
            {
                return null;
            }
            else
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the assignment of the variable " + varAssigned, null));
                return null;
            }

        }
        if (isStatic) {
            //check if the value that is being assigned is a static field as we dont have static types we only need to check if the variable that it is accessing is in the fields, otherwise it is in the params or in the locals
            List<Symbol> fields = table.getFields();
            List<String> fieldNames = fields.stream().map(Symbol::getName).toList();
            var optionalLocals = table.getLocalVariablesTry(currentMethod);
            var optionalParams = table.getParametersTry(currentMethod);
            //check in the locals
            if(optionalLocals.isPresent())
            {
                var locals = optionalLocals.get();
                for(Symbol s: locals)
                {
                    if(s.getName().equals(varAssigned))
                    {
                        return null; //it is in the locals
                    }
                }
            }
            if(optionalParams.isPresent())
            {
                var params = optionalParams.get();
                for(Symbol s: params)
                {
                    if(s.getName().equals(varAssigned))
                    {
                        return null; //it is in the params
                    }
                }
            }
            if (fieldNames.contains(varAssigned)) {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Cannot assign a value to a non static field in a static method", null));
                return null;
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
        //isArray must be set to True
        if(!typeArray.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Variable " + array.get("value") + " is not an array", null));
            return null;
        }
        //index must be an integer
        if(!typeIndex.getName().equals("int"))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0 ,"Variable" + index.get("value") + " is not an integer", null));
            return null;
        }
        return null;
    }

    private Void visitReturnStmt(JmmNode expr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(expr.getJmmChild(0), table);
        if(!table.getImports().isEmpty()) {
            //check if the return type is the class imported
            if(!table.getSuper().isEmpty())
            {
                return null;
            }
        }
        //check if the return type is the same as the method return type
        if(table.getImports().contains(type.getName()))
        {
            return null;
        }
        if(!type.equals(table.getReturnType(currentMethod)))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, " is not an integer", null));
            return null;
        }

        return null;
    }



}
