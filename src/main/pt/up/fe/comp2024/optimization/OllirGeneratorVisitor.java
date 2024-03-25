package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR_DECL, this::visitVarDecl);


        setDefaultVisit(this::defaultVisit);
    }


    private String visitVarDecl(JmmNode var, Void unused) {
        if(true){
            return "";
        }
        StringBuilder code = new StringBuilder();
        code.append(var.get("name"));
        code.append(".");
        code.append(var.getKind());
        code.append(";\n");
        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("methodName")).orElseThrow();

        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));

        }
        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));

        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        boolean isStatic = NodeUtils.getBooleanAttribute(node,"isStatic","false");

        if (isPublic) {
            code.append("public ");
        }

        if (isStatic)
        {
            code.append("static");
        }


        // name
        List<Symbol> parametersCode;
        if (!node.hasAttribute("methodName") || node.get("methodName").equals("main")){
            code.append("main");
            code.append("(args.array.String)"); //TODO: deixar de ser bruteforce quando trabalhar com os arrays
            parametersCode = Collections.emptyList();

        }
        else{
            //not main method

            var name = node.get("methodName");
            code.append(name);
            parametersCode = this.table.getParameters(name);
        }

        //get params
        StringBuilder params = new StringBuilder();
        for (int i = 0; i< parametersCode.size(); i++){
            String parName = parametersCode.get(i).getName();
            params.append(parName);
            params.append(OptUtils.toOllirType(parametersCode.get(i).getType()));
            if ( i == parametersCode.size()-1){}
            else{
                params.append(", ");
            }
        }

        code.append("(" + params + ")");



        // type
        if (node.getChildren().isEmpty()){
            code.append(".V");
        }
        else{
            var retType = OptUtils.toOllirType(node.getJmmChild(0));
            code.append(retType);
        }
        code.append(L_BRACKET);



        // rest of its children stmts
        var afterParam = 2;
        if (parametersCode.size() == 0){
            afterParam = 1;
        }

        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);

        }


        code.append(R_BRACKET);
        code.append(NL);
        return code.toString();
    }



    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        var superClass = this.table.getSuper();

        if(!superClass.isEmpty()){
            code.append(" extends " + superClass);
        }


        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        for (var imports : this.table.getImports()){ //TODO : fazer uma visitor para imports talvez seja necessário
            code.append("import " + imports +";\n");
        }
        StringBuilder code2 = new StringBuilder();
        node.getChildren().stream()
                .map(this::visit)
                .forEach(code2::append);

        return code.append(code2).toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
