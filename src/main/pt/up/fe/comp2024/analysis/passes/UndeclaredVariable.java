
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
import java.util.Collections;
import java.util.List;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    private Boolean isStatic;

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
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.THIS_EXPR, this::visitThisExpr);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.IMPORT_DECLARATION, this::visitImportDecl);
        addVisit(Kind.PARAM, this::visitParam);
        addVisit(Kind.ARRAY_CREATION_EXPR, this::visitArrayCreationExpr);
        addVisit(Kind.ARRAY_LENGTH_EXPR, this::visitArrayLengthExpr);


    }

    private Void visitArrayLengthExpr(JmmNode arrayLengthExpr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(arrayLengthExpr.getChild(0),table);
        if(!arrayLengthExpr.get("value").equals("length"))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Variable " + arrayLengthExpr.get("value") + " is not an array", null));
        }
        if(!type.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Variable " + arrayLengthExpr.getJmmChild(0).get("value") + " is not an array", null));
        }
        return null;
    }
    private Void visitArrayCreationExpr(JmmNode arrayCreationExpr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(arrayCreationExpr.getChild(0),table);
        for(int i = 1; i < arrayCreationExpr.getNumChildren(); i++)
        {
            if(!type.equals(TypeUtils.getExprType(arrayCreationExpr.getChild(i),table)))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the array creation expression", null));
            }
        }
        return null;
    }
    private Void visitImportDecl(JmmNode importDecl, SymbolTable table)
    {
        var imports = table.getImports();
        var refactoredImports = new ArrayList<String>();
        var wholePathImports = new ArrayList<String>();
        //check if the import is complex if so only consider the last part of the import
        for(var s : imports)
        {
            if(s.contains("."))
            {
                var whole = wholePathImports.add(s);
                var aux = s.split("\\.");
                refactoredImports.add(aux[aux.length - 1]);
            }
            else {
                refactoredImports.add(s);
            }
        }
        //check in the refactoredImports list if the import is duplicated
        for(var s : refactoredImports)
        {
            int frequency = Collections.frequency(refactoredImports,s);
            if(frequency > 1)
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Import " + s + " is duplicated", null));
            }
        }
       return null;
    }

    private Void visitParam(JmmNode param, SymbolTable table) {
        List<Symbol> symbols = table.getParameters(currentMethod);
        List<String> symbolNames = symbols.stream().map(Symbol::getName).toList();

        List<String> paramNames = param.getObjectAsList("paramName",String.class); // assuming paramName is a list of strings
        //check duplicates
        for (String paramName : paramNames) {
            int frequency = Collections.frequency(symbolNames, paramName);
            if (frequency > 1) {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Parameter " + paramName + " is duplicated", null));
            }
        }

        //check if varargs is the last parameter declared
        for(var aux : param.getChildren())
        {
            if(aux.getKind().equals("VarArgsType"))
            {
                //must be the last one declared
                if(aux.getIndexOfSelf() != param.getNumChildren() - 1)
                {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Varargs must be the last parameter declared", null));
                }
            }
        }

        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table)
    {
        // in the fields and in the locals we cannot have varargs defined
        //fields
        if(varDecl.getParent().getKind().equals(Kind.CLASS_DECL.toString())){
            List<Symbol> symbols = table.getFields();
            List<String> symbolNames = symbols.stream().map(Symbol::getName).toList();
            int frequency = Collections.frequency(symbolNames,varDecl.get("name"));
            if(frequency > 1)
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Field " + varDecl.get("name") + " is duplicated", null));
            }
        }
        else
        {
            List<Symbol> symbols = table.getLocalVariables(currentMethod);
            List<String> symbolNames = symbols.stream().map(Symbol::getName).toList();
            int frequency = Collections.frequency(symbolNames,varDecl.get("name"));
            if(frequency > 1)
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Local Variable " + varDecl.get("name") + " is duplicated", null));
            }
        }
        if(varDecl.getChild(0).getKind().equals("VarArgsType"))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Varargs cannot be defined in the fields", null));
        }
        if(varDecl.getChild(0).getKind().equals(Kind.CLASS_TYPE.toString()))
        {
            if(!table.getImports().contains(varDecl.getChild(0).get("value")) && !varDecl.getChild(0).get("value").equals(table.getClassName()))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Class " + varDecl.getChild(0).get("value") + " not declared", null));
            }
        }


        return null;
    }
    private Void visitThisExpr(JmmNode thisExpr, SymbolTable table)
    {
        //check if the call to the this expr is in a static method if so raise an report
        if(isStatic)
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Cannot use this token in a static method", null));
        }
        return null;
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        isStatic = NodeUtils.getBooleanAttribute(method, "isStatic", "false");
        if(currentMethod.equals("main") && isStatic.equals(false))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Main method not declared static.",null));
        }
        if(!currentMethod.equals("main") && isStatic.equals(true))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method " + currentMethod + " declared static.",null));
        }
        TypeUtils.setCurrentMethod(currentMethod);
        TypeUtils.setStatic(NodeUtils.getBooleanAttribute(method, "isStatic", "false"));
        //check if there are any duplicated methods
        var frequency = Collections.frequency(table.getMethods(),currentMethod);
        if(frequency > 1)
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method " + currentMethod + " is duplicated", null));
        }
        return null;
    }

    private Void visitWhileStmt(JmmNode whileExpr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(whileExpr.getChild(0),table);
        if(!type.getName().equals("boolean") || type.isArray())
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the condition of the while statement", null));
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
        }
        for(int i = 1; i < arrayAssignStmt.getNumChildren(); i++)
        {
            Type type = TypeUtils.getExprType(arrayAssignStmt.getChild(i), table);
            if(!type.getName().equals(typeArray.getName()))
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
        // Permitimos as operações binarias entre dois elementos com o mesmo tipo exceto para arrays
        if(!leftType.equals(rightType) || (leftType.isArray()) || (rightType.isArray()) )
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
        var availableImports = "";
        var hasArray = false;
        int i = 0;
        var extended = table.getSuper();
        if(!table.getMethods().contains(expr.get("value")) && !imports.isEmpty() && !extended.isEmpty() && imports.contains(expr.getJmmChild(0).get("value")))
        {
            return null; // Assume the method is declared by the import
        }
        Type type = TypeUtils.getExprType(expr.getChild(0), table);
        if(imports.contains(type.getName()))
        {
            return null; // Assume the method is declared by the import
        }
        else if(table.getClassName().equals(type.getName()) && !extended.isEmpty())
        {
            return null;
        }

        // verify if the method is declared
        if(type.getName().equals(table.getClassName()) && (extended.isEmpty() || imports.isEmpty())) {

            if (!table.getMethods().contains(expr.get("value"))) {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method " + expr.get("value") + " not declared", null));

            } else {
                var optionalParams = table.getParametersTry(expr.get("value"));

                // check if the parameters are correct
                if (optionalParams.isPresent()) {
                    var params = optionalParams.get();
                    //vargs must be the last parameter of the function
                    if(!params.isEmpty()) {
                        if(expr.getNumChildren() <= 1)
                        {
                            addReport(Report.newError(Stage.SEMANTIC,0,0,"Invalid number of parameters",null));
                            return null;
                        }
                        for (i = 0; i < params.size(); i++) {
                            //can be var args or a list
                            if (params.get(i).getType().isArray()) {
                                if (expr.getJmmChild(i + 1).getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getJmmChild(i + 1).getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                                    for (int j = i + 1; j < expr.getNumChildren(); j++) {
                                        hasArray = true;
                                        if (!params.get(i).getType().getName().equals(TypeUtils.getExprType(expr.getJmmChild(j), table).getName())) {
                                            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the parameters of the method " + expr.get("value"), null));
                                            return null;
                                        }
                                    }
                                } else {
                                    var m = visit(expr.getJmmChild(i + 1), table);
                                }
                            } else {
                                if (!params.get(i).getType().equals(TypeUtils.getExprType(expr.getChild(i + 1), table))) {
                                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the parameters of the method " + expr.get("value"), null));
                                    return null;
                                }
                            }
                        }
                    }
                    else if(expr.getNumChildren() > 1)
                    {
                        addReport(Report.newError(Stage.SEMANTIC,0,0, "The method signature has more parameters than the method decl!",null));
                        return null;
                    }


                }
                if(i != expr.getNumChildren()-1 && !hasArray)
                {
                    addReport(Report.newError(Stage.SEMANTIC,0,0, "Invalid number of parameters",null));
                    return null;
                }
            }
        }
        else
        {
            addReport(Report.newError(Stage.SEMANTIC,0,0, "import variable not declared",null));
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
        }
        //index must be an integer
        if(!typeIndex.getName().equals("int"))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0 ,"Variable" + index.get("value") + " is not an integer", null));
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

        }

        return null;
    }


}
