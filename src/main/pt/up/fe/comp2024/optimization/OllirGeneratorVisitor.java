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
import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

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

    private String currentMethod;
    int counter_IF = -1;
    int counter_WHILE = -1;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECLARATION, this::visitImport);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(EXPR_STMT, this::visitExpr);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(BLOCK_STMT,this::visitBlockStmt);
        addVisit(WHILE_STMT,this::visitWhileStmt);
        addVisit(ARRAY_ASSIGN_STMT,this::visitArrayAssignStmt);
        setDefaultVisit(this::defaultVisit);
    }



    private String visitArrayAssignStmt(JmmNode arrayAssignStmt, Void unused)
    {
        StringBuilder computation = new StringBuilder();
        String code = "";
        var type = OptUtils.toOllirType(getExprType(arrayAssignStmt,table));
        var index = exprVisitor.visit(arrayAssignStmt.getJmmChild(0));
        var assign = exprVisitor.visit(arrayAssignStmt.getJmmChild(1));
        computation.append(index.getComputation());
        computation.append(assign.getComputation());
        computation.append(arrayAssignStmt.get("value")).append("[").append(index.getCode()).append("]").append(type).append(SPACE).append(ASSIGN).append(SPACE).append(type).append(SPACE).append(assign.getCode()).append(END_STMT);
        return computation.toString();
    }
    private String visitWhileStmt(JmmNode whileStmt, Void unused)
    {
        String code = "";
        int aux;
        StringBuilder computation  = new StringBuilder();
        var boolExpr = exprVisitor.visit(whileStmt.getJmmChild(0));
        counter_WHILE = getCounterWhileStmt(counter_WHILE);
        aux = counter_WHILE;
        computation.append("goto ").append("while_cond_").append(aux).append(END_STMT);
        computation.append("while_body_").append(aux).append(":").append(NL);
        var stmt = visit(whileStmt.getJmmChild(1));
        computation.append(stmt);
        computation.append("while_cond_").append(aux).append(":").append(NL);
        computation.append(boolExpr.getComputation());
        computation.append("if( ").append(boolExpr.getCode()).append(" )").append("goto while_body_").append(aux).append(END_STMT);
        return computation.toString();
    }

    private String visitBlockStmt(JmmNode blockStmt, Void unused)
    {
        StringBuilder code = new StringBuilder();
        for (var child : blockStmt.getChildren()) {
            var childCode = visit(child);
            code.append(childCode);
        }
        return code.toString();
    }

    private int getCounterWhileStmt(int counter)
    {
        counter += 1;
        return counter;
    }

    private int getCounterIfStmt(int counter)
    {
        counter += 1;
        return counter;
    }

    private String visitIfStmt(JmmNode ifStmt, Void unused)
    {
        StringBuilder computation = new StringBuilder();
        int aux;
        //Primeira child é a boolean expr
        var expr = exprVisitor.visit(ifStmt.getJmmChild(0));
        //Block stmt do else
        counter_IF = getCounterIfStmt(counter_IF);
        aux =  counter_IF;
        var stmt = visit(ifStmt.getJmmChild(2));
        computation.append(expr.getComputation());
        computation.append("if( ").append(expr.getCode()).append(" )").append("goto").append(SPACE).append("if_then_").append(aux).append(END_STMT);
        computation.append(stmt).append("goto").append(SPACE).append("if_end_").append(aux).append(END_STMT);
        computation.append("if_then_").append(aux).append(":").append(NL);
        //Block stmt do if
        computation.append(visit(ifStmt.getJmmChild(1)));
        computation.append("if_end_").append(aux).append(":").append(NL);
        return computation.toString();
    }

    private String visitVarDecl(JmmNode var, Void unused) {
        //fields
        if(var.getParent().getKind().equals(CLASS_DECL.toString()))
        {
            return visitField(var, unused);
        }
        StringBuilder code = new StringBuilder();
        code.append(var.get("name"));
        code.append(".");
        code.append(var.getKind());
        code.append(";\n");
        return code.toString();
    }

    private String visitExpr(JmmNode node, Void unused) {

        var expr = exprVisitor.visit(node.getJmmChild(0));

        StringBuilder code = new StringBuilder();
        code.append(expr.getComputation());
        /*
        code.append(expr.getCode());
        */

        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {
        var lhs = node.get("value");
        var utilsType = getExprType(node,table);
        var ollirType = OptUtils.toOllirType(utilsType);
        var rhs = exprVisitor.visit(node.getChild(0));
        //check where it is defined
        var optionalFields = table.getFields();
        var optionalParams = table.getParametersTry(currentMethod);
        var optionalLocals = table.getLocalVariablesTry(currentMethod);
        var inFields = false;
        var inParams = false;
        var inLocals = false;
        for(Symbol s : optionalFields)
        {
            if(s.getName().equals(lhs))
            {
                ollirType = OptUtils.toOllirType(s.getType());
                inFields = true;
            }
        }
        if(optionalParams.isPresent())
        {
            for(Symbol s: optionalParams.get())
            {
                if(s.getName().equals(lhs))
                {
                    ollirType = OptUtils.toOllirType(s.getType());
                    inParams = true;
                }
            }
        }
        if(optionalLocals.isPresent())
        {
            for(Symbol s: optionalLocals.get())
            {
                if(s.getName().equals(lhs))
                {
                    ollirType = OptUtils.toOllirType(s.getType());
                    inLocals = true;
                }
            }
        }
        if(inLocals || inParams)
        {
            String code = "";

            StringBuilder computation = new StringBuilder();

            computation.append(rhs.getComputation());
            computation.append(lhs).append(ollirType).append(SPACE).append(ASSIGN);
            computation.append(SPACE).append(ollirType).append(SPACE).append(rhs.getCode()).append(END_STMT);
            code = computation.toString();
            return code.toString();
        }
        else if(inFields)
        {
            String code = "";
            StringBuilder computation = new StringBuilder();
            computation.append(rhs.getComputation());
            computation.append("putfield");
            computation.append(SPACE);
            computation.append("(");
            computation.append("this");
            computation.append(",");
            computation.append(lhs).append(ollirType);
            computation.append(",");
            computation.append(rhs.getCode());
            computation.append(")");
            computation.append(".V").append(END_STMT);
            code = computation.toString();
            return code.toString();
        }

        return "";
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("methodName")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();
        var type = OptUtils.toOllirType(retType);


        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }


        if(expr.getComputation().startsWith("tmp"))
        {

            code.append(expr.getComputation());
            code.append("ret");
            code.append(SPACE);
            code.append(type);
            code.append(expr.getCode());
        }
        else
        {
            code.append("ret");
            code.append(SPACE);
            code.append(type);
            code.append(SPACE);
            code.append(expr.getCode());
        }
        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {
        String code = "";
        StringBuilder computation = new StringBuilder();
        String currentMethod = node.getParent().get("methodName");
        var params = table.getParameters(currentMethod);
        int i = 0;
        for(Symbol s: params)
        {
            if(i == params.size()-1)
            {
                computation.append(s.getName());
                if (s.getType().isArray())
                {
                    computation.append(".array");
                }

                computation.append(OptUtils.toOllirType(s.getType()));

            }
            else
            {
                computation.append(s.getName());
                computation.append(OptUtils.toOllirType(s.getType()));
                computation.append(",");
                computation.append(SPACE);
                i+=1;

            }
        }
        code = computation.toString();

        return code;
    }

    private String visitField(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = ".field public " + id + typeCode + END_STMT;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        //vamos dar set do current method que estamos a explorar
        TypeUtils.setCurrentMethod(node.get("methodName"));
        currentMethod = node.get("methodName");
        exprVisitor.setCurrentMethod(node.get("methodName"));
        var afterParam = 0;

        StringBuilder code = new StringBuilder(".method ");

        //Atribuição do public e do static que vem da gramatica
        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("methodName");
        code.append(name);
        //we are dealing with a main method : Parameters known
        if(name.equals("main"))
        {
            code.append("(args.array.String).V");
        }
        else if(!table.getParameters(node.get("methodName")).isEmpty())
        {
            //the first child always declared in a default method is the type of the function
            afterParam = 1;
            code.append("(");
            for(int i = afterParam; PARAM.check(node.getJmmChild(i));i++)
            {
                if (i > 1) {
                    code.append(", ");
                }
                code.append(visit(node.getJmmChild(i)));
                afterParam++;

                if (i + 1 == node.getNumChildren()) {
                    break;
                }
            }
            code.append(")");
        }
        else if(table.getParameters(node.get("methodName")).isEmpty())
        {
            code.append("(");
            code.append(")");
        }

        // type
        if(node.getNumChildren() > 0) {
            var retType = OptUtils.toOllirType(node.getJmmChild(0));
            if(retType.equals(".V"))
            {
                retType = "";

            } // main
            code.append(retType);
        }
        code.append(L_BRACKET);


        // return void
        boolean voidRet = true;

        // rest of its children stmts
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            // local
            if (VAR_DECL.check(child)) {
                continue;
            }
            // check if there is a return statement
            if (RETURN_STMT.check(child)) {
                voidRet = false;
            }
            var childCode = visit(child);
            code.append(childCode);
        }

        if (voidRet) {
            code.append("ret.V ;\n");
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }



    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        // extends
        code.append(" extends ");
        if (!table.getSuper().isEmpty()) {
            code.append(table.getSuper());
        } else {
            code.append("Object ");
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

    private String visitImport(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        var imports = node.getObjectAsList("value" , String.class);
        StringBuilder importName = new StringBuilder();
        if(imports.size() > 1)
        {
            for(int i = 0; i < imports.size(); i++)
            {
                if(i == imports.size() - 1)
                {
                    importName.append(imports.get(i));
                }
                else {
                    importName.append(imports.get(i));
                    importName.append(".");}
            }
        }
        else
        {
            importName.append(imports.get(0));
        }
        code.append("import ");
        code.append(importName);
        code.append(END_STMT);

        return code.toString();
    }


    private String visitProgram(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
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
