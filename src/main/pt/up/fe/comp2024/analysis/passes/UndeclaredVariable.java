
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
        addVisit(Kind.VAR_REF, this::visitVarRef);
        addVisit(Kind.METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }


    private Void visitVarDecl(JmmNode varDecl, SymbolTable table)
    {
        if(varDecl.getChild(0).getKind().equals(Kind.CLASS_TYPE.toString()))
        {
            if(!table.getImports().contains(varDecl.getChild(0).get("value")) && !varDecl.getChild(0).get("value").equals(table.getClassName()))
            {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Class " + varDecl.getChild(0).get("value") + " not declared", null));
                return null;
            }
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table)
    {
        currentMethod = method.get("methodName");

        isStatic = NodeUtils.getBooleanAttribute(method, "isStatic", "false");

        TypeUtils.setCurrentMethod(currentMethod);

        TypeUtils.setStatic(NodeUtils.getBooleanAttribute(method, "isStatic", "false"));

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

    private Void visitMethodCallExpr(JmmNode expr, SymbolTable table) {
        if (expr == null || table == null) {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method call expression is null", null));
            return null;
        }

        var imports = table.getImports();
        var hasArray = false;
        int i = 0;
        var extended = table.getSuper();

        if (imports == null || extended == null) {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method call expression is null", null));
            return null;
        }

        if (!table.getMethods().contains(expr.get("value")) && !imports.isEmpty() && !extended.isEmpty() && imports.contains(expr.getJmmChild(0).get("value"))) {
            return null; // Assume the method is declared by the import
        }

        Type type = TypeUtils.getExprType(expr.getChild(0), table);
        if (type == null) {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method call expression is null", null));
            return null;
        }

        if (imports.contains(type.getName())) {
            return null; // Assume the method is declared by the import
        } else if (table.getClassName().equals(type.getName()) && !extended.isEmpty()) {
            return null;
        }

        // verify if the method is declared
        if (type.getName().equals(table.getClassName()) && (extended.isEmpty() || imports.isEmpty())) {
            if (!table.getMethods().contains(expr.get("value"))) {
                addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method " + expr.get("value") + " not declared", null));
                return null;
            } else {
                var optionalParams = table.getParametersTry(expr.get("value"));

                // check if the parameters are correct
                if (optionalParams.isPresent()) {
                    var params = optionalParams.get();
                    if (params == null) {
                        return null;
                    }

                    //vargs must be the last parameter of the function
                    if (!params.isEmpty() && expr.getNumChildren() > 1) {
                        for (i = 0; i < params.size(); i++) {
                            //can be var args or a list
                            if (params.get(i).getType().isArray()) {
                                //avoid the exception here
                                if (i >= expr.getNumChildren() - 1) {
                                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Invalid number of parameters", null));
                                    return null;
                                }

                                if (expr.getJmmChild(i + 1).getKind().equals(Kind.INTEGER_LITERAL.toString()) || expr.getJmmChild(i + 1).getKind().equals(Kind.BOOLEAN_LITERAL.toString())) {
                                    for (int j = i + 1; j < expr.getNumChildren(); j++) {
                                        hasArray = true;
                                        //avoid the exception here
                                        if (j >= expr.getNumChildren()) {
                                            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Invalid number of parameters", null));
                                            return null;
                                        }

                                        if (!params.get(i).getType().getName().equals(TypeUtils.getExprType(expr.getJmmChild(j), table).getName())) {
                                            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the parameters of the method " + expr.get("value"), null));
                                            return null;
                                        }
                                    }
                                } else {
                                    if (i >= expr.getNumChildren() - 1) {
                                        addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Invalid number of parameters", null));
                                        return null;
                                    }

                                    var m = visit(expr.getJmmChild(i + 1), table);
                                }
                            } else {
                                if (i >= expr.getNumChildren() - 1) {
                                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Invalid number of parameters", null));
                                    return null;
                                }

                                if (!params.get(i).getType().equals(TypeUtils.getExprType(expr.getChild(i + 1), table))) {
                                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Type mismatch in the parameters of the method " + expr.get("value"), null));
                                    return null;
                                }
                            }
                        }
                    } else if (expr.getNumChildren() > 1) {
                        addReport(Report.newError(Stage.SEMANTIC, 0, 0, "The method signature has more parameters than the method decl!", null));
                        return null;
                    }
                }

                if (i != expr.getNumChildren() - 1 && !hasArray) {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Invalid number of parameters", null));
                    return null;
                }
            }
        } else {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "import variable not declared", null));
            return null;
        }

        return null;
    }


}
