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



public class InvalidOperations extends AnalysisVisitor {

    private String currentMethod;

    private Boolean isStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.THIS_EXPR, this::visitThisExpr);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.PARAM, this::visitParam);
        addVisit(Kind.ARRAY_CREATION_EXPR, this::visitArrayCreationExpr);
        addVisit(Kind.ARRAY_LENGTH_EXPR, this::visitArrayLengthExpr);


    }

    private Void visitArrayLengthExpr(JmmNode arrayLengthExpr, SymbolTable table)
    {
        Type type = TypeUtils.getExprType(arrayLengthExpr.getChild(0),table);
        if(!arrayLengthExpr.get("value").equals("length"))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Not Implemented Yet " + arrayLengthExpr.get("value"), null));
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
        return null;
    }

    private Void visitParam(JmmNode param, SymbolTable table) {
        //check if varargs is the last parameter declared
        for(var aux : param.getChildren())
        {
            if(aux.getKind().equals("VarArgsType"))
            {
                //must be the last one declared
                if(aux.getIndexOfSelf() != param.getNumChildren() - 1)
                {
                    addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Varargs must be the last parameter declared", null));
                    return null;
                }
            }
        }
        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table)
    {
        if(varDecl.getChild(0).getKind().equals("VarArgsType"))
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Varargs cannot be defined in the fields", null));
            return null;
        }
        return null;
    }

    private Void visitThisExpr(JmmNode thisExpr, SymbolTable table)
    {
        //check if the call to the this expr is in a static method if so raise an report
        if(isStatic)
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Cannot use this token in a static method", null));
            return null;
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        isStatic = NodeUtils.getBooleanAttribute(method, "isStatic", "false");
        TypeUtils.setCurrentMethod(currentMethod);
        TypeUtils.setStatic(NodeUtils.getBooleanAttribute(method, "isStatic", "false"));
        return null;
    }




}
