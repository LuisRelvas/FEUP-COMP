package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.CompilerConfig;
import java.util.Map;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        String optimizationConfig = semanticsResult.getConfig().get("optimize");
        boolean applyOptimizations = optimizationConfig != null && optimizationConfig.equals("true");
        if(!applyOptimizations)
        {
            return JmmOptimization.super.optimize(semanticsResult);
        }

        boolean modificationsProp = true;
        boolean modificationsFold = true;
        int counter = 0;
        while(modificationsFold == true || modificationsProp == true ) {
            ConstantPropagationVisitor optimizationAst = new ConstantPropagationVisitor(semanticsResult.getSymbolTable());
            ConstantFoldingVisitor optimization = new ConstantFoldingVisitor(semanticsResult.getSymbolTable());
            optimizationAst.visit(semanticsResult.getRootNode());
            optimization.visit(semanticsResult.getRootNode());
            modificationsFold = optimization.getModifications();
            modificationsProp = optimizationAst.getModifications();
            System.out.println("the value of the modificationsProp is " + modificationsProp);
            System.out.println("the value of the modificationsFold is " + modificationsFold);
            counter++;
        }

        return JmmOptimization.super.optimize(semanticsResult);
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }
}
