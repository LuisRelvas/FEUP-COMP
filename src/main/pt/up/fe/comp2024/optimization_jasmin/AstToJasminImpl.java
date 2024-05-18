package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast2jasmin.AstToJasmin;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp2024.optimization.ConstantFoldingVisitor;
import pt.up.fe.comp2024.optimization.ConstantPropagationVisitor;

import java.util.Collections;

public class AstToJasminImpl implements AstToJasmin {
    @Override
    public JasminResult toJasmin(JmmSemanticsResult semanticsResult) {

        var generator = new JasminGeneratorVisitor(semanticsResult.getSymbolTable());
        var code = generator.visit(semanticsResult.getRootNode());

        return new JasminResult(semanticsResult, code, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        ConstantPropagationVisitor optimizationAst = new ConstantPropagationVisitor(semanticsResult.getSymbolTable());
        ConstantFoldingVisitor optimization = new ConstantFoldingVisitor(semanticsResult.getSymbolTable());
        optimizationAst.visit(semanticsResult.getRootNode());
        optimization.visit(semanticsResult.getRootNode());
        return AstToJasmin.super.optimize(semanticsResult);
    }
}
