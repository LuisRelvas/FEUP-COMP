package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {


    private final List<String> importDeclaration;
    private final String className;
    private final String extended;
    private final List<String> methods;
    private final List<Symbol> fields;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;

    public JmmSymbolTable(
                            List<String> importDeclaration,
                            String className,
                            String extended,
                          List<String> methods,
                          List<Symbol> fields,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals) {
        this.importDeclaration = importDeclaration;
        this.className = className;
        this.extended = extended;
        this.methods = methods;
        this.fields = fields;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
    }

    @Override
    public List<String> getImports() {
        return importDeclaration;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return extended;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public String getExtended() {return extended; }

    @Override
    public Type getReturnType(String methodSignature) {
        if(methodSignature.equals("main")){
            return new Type("void", false);
        } else {
            //System.out.println("MethodName is " + returnTypes.get(methodSignature).getName() + returnTypes.get(methodSignature).isArray());
            return new Type(returnTypes.get(methodSignature).getName(), returnTypes.get(methodSignature).isArray());
        }
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return Collections.unmodifiableList(params.get(methodSignature));
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return Collections.unmodifiableList(locals.get(methodSignature));
    }


}
