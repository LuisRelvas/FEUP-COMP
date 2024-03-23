package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
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
        addVisit("VarRef", this::visitVarRefExpr);
        addVisit("ExprStmt", this::visitExprStmt);
    }


    private Void visitExprStmt(JmmNode exprStmt, SymbolTable table)
    {
        var expr = exprStmt.getChildren().get(0);
        List<String> methodsDeclared = new ArrayList<>();
        String extended = table.getSuper();
        boolean check;
        check = false;

        if(expr.getKind().equals("MethodCallExpr"))
        {
            methodsDeclared = table.getMethods();
            for(var i: methodsDeclared)
            {
                if(i.equals(expr.get("value")))
                {
                    check = true;
                }
            }
        }
        if(extended != "" )
        {
            check = true;
        }
        if(check == false)
        {
            var message = String.format("Method '%s' does not exist.", expr.get("value"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(exprStmt),
                    NodeUtils.getColumn(exprStmt),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("value");

        // Var is a field, return
        if (table.getFields().stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }


}
