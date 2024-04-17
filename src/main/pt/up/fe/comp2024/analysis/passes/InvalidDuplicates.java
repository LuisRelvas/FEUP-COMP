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



public class InvalidDuplicates extends AnalysisVisitor {

    private String currentMethod;

    private Boolean isStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.PARAM, this::visitParam);


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
                return null;
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
                return null;
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
                return null;
            }
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("methodName");
        TypeUtils.setCurrentMethod(currentMethod);
        TypeUtils.setStatic(NodeUtils.getBooleanAttribute(method, "isStatic", "false"));
        //check if there are any duplicated methods
        var frequency = Collections.frequency(table.getMethods(),currentMethod);
        if(frequency > 1)
        {
            addReport(Report.newError(Stage.SEMANTIC, 0, 0, "Method " + currentMethod + " is duplicated", null));
            return null;
        }
        return null;
    }


}
