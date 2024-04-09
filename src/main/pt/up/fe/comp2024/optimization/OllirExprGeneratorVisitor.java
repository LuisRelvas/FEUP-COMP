package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    public String currentMethod;
    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    public void setCurrentMethod(String currentMethod) {
        this.currentMethod = currentMethod;
    }

    @Override
    protected void buildVisitor() {
        //addVisit(NEG_EXPR, this::visitNegExpr); ainda não está
        addVisit(VAR_REF, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(METHOD_CALL_EXPR, this::visitMethodInvocation);
        addVisit(NEW_OBJECT_EXPR, this::visitNewObjectExpr);
        addVisit(THIS_EXPR, this::visitThisExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitAssignStmt(JmmNode node, Void unused) {
        Type type = TypeUtils.getExprType(node, table);
        if (node.getJmmChild(0).getKind().equals(BINARY_EXPR.toString())) {
            StringBuilder computation = new StringBuilder();
            var OllirExpr = visit(node.getJmmChild(0));
            //Create a temporary variable with the OllirType
            String code = "";// variable name
            code += node.get("value");
            computation.append(node.get("value"));
            computation.append(ASSIGN);
            computation.append(OllirExpr.getComputation().substring(0, 4));

            return new OllirExprResult(code, computation);
        } else if (node.getJmmChild(0).getKind().equals(NEW_OBJECT_EXPR.toString())) {
            StringBuilder computation = new StringBuilder();
            //Create a temporary variable with the OllirType
            var OllirType = OptUtils.toOllirType(node.getJmmChild(0));
            String code = OptUtils.getTemp() + OllirType;

            return new OllirExprResult(code, computation);
        } else {
            StringBuilder computation = new StringBuilder();
            var OllirType = OptUtils.toOllirType(type);
            String code = node.get("value") + OllirType;
            return new OllirExprResult(code, computation);
        }
    }

    private OllirExprResult visitThisExpr(JmmNode thisNode, Void unused) {
        String code = "";
        StringBuilder computation = new StringBuilder();
        computation.append("this");
        computation.append(".");
        computation.append(table.getClassName());
        code = computation.toString();
        return new OllirExprResult(code);
    }

    private OllirExprResult visitNewObjectExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        var ollirType = OptUtils.toOllirType(node);
        var temp = OptUtils.getTemp();
        String code = "";
        computation.append(temp);
        computation.append(ollirType);
        computation.append(ASSIGN);
        computation.append(ollirType);
        computation.append(SPACE);
        computation.append("new ");
        computation.append("(");
        computation.append(node.get("value"));
        computation.append(")");
        computation.append(ollirType);
        computation.append(END_STMT);
        code = temp + ollirType; // variable name
        computation.append("invokespecial ( " + code + "," + "\"\""+ ")" + ".V");
        computation.append(END_STMT);
        return new OllirExprResult(code,computation);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        computation.append(ollirIntType);
        computation.append(SPACE);
        computation.append(node.get("value"));
        computation.append(ollirIntType);
        String code = node.get("value");
        return new OllirExprResult(code,computation);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));
        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String temp = OptUtils.getTemp();
        String code =  temp + resOllirType;
        StringBuilder computation = new StringBuilder();
        if(node.getJmmChild(0).getKind().equals(METHOD_CALL_EXPR.toString()))
        {
            computation.append(lhs.getComputation());
        }
        if(node.getJmmChild(1).getKind().equals(METHOD_CALL_EXPR.toString())) {
            computation.append(rhs.getComputation());
        }
        computation.append(code);
        computation.append(ASSIGN);
        // code to compute the children
        if(lhs.getCode().startsWith("tmp"))
        {
            computation.append("tmp"+ Integer.parseInt(lhs.getCode().substring(3)));
            computation.append(lhs.getComputation().substring(4,lhs.getComputation().indexOf(":")));
        }
        else
        {
            computation.append(lhs.getComputation().substring(0,lhs.getComputation().length()));
        }
        computation.append(node.get("op"));
        if(rhs.getCode().startsWith("tmp"))
        {
            computation.append(rhs.getComputation().substring(4,rhs.getComputation().indexOf(":")));
            computation.append(SPACE);
            computation.append("tmp" + Integer.parseInt(rhs.getCode().substring(3)));
            computation.append(rhs.getComputation().substring(4,rhs.getComputation().indexOf(":")));

        }
        else
        {
            computation.append(rhs.getComputation().substring(0,rhs.getComputation().length()));

        }
        computation.append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        var id = node.get("value");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);
        if(node.getParent().getKind().equals(METHOD_CALL_EXPR.toString()))
        {
            computation.append(id);
            computation.append(ollirType);
            computation.append(SPACE);
            String code = id;
            return new OllirExprResult(code,computation);
        }
        computation.append(ollirType);
        computation.append(SPACE);
        computation.append(id);
        computation.append(SPACE);
        computation.append(ollirType);
        String code = id;
        return new OllirExprResult(code,computation);
    }
    private OllirExprResult visitMethodInvocation(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        StringBuilder params = new StringBuilder();
        Type returnTypeMethods = new Type(null, false);
        var imp = visit(node.getJmmChild(0));
        String typeFunction = "";
        if(node.getJmmChild(0).getKind().equals(THIS_EXPR.toString()) || table.getMethods().contains(node.get("value")))
        {
            typeFunction = "invokevirtual";
        }
        else
        {
            typeFunction ="invokestatic";
        }


        //Handle the case where it can be in the imports
        if(table.getImports().contains(node.getJmmChild(0).get("value")))
        {
            computation.append(typeFunction);
            computation.append("(");
            computation.append(node.getJmmChild(0).get("value"));
            computation.append(",");
            computation.append("\"" + node.get("value") + "\"");
            //Exists params
            if(node.getNumChildren() > 1){
            for(int i = 1; i < node.getNumChildren();i++)
            {
                if(i == node.getNumChildren() - 1)
                {
                    var typeParam = TypeUtils.getExprType(node.getChild(i),table);
                    var OllirType = OptUtils.toOllirType(typeParam);
                    computation.append(",");
                    computation.append(node.getChild(i).get("value"));
                    computation.append(OllirType);
                }
                else {
                    computation.append(",");
                    var typeParam = TypeUtils.getExprType(node.getChild(i),table);
                    var OllirType = OptUtils.toOllirType(typeParam);
                    computation.append(node.getChild(i).get("value"));
                    computation.append(OllirType);
                    computation.append(",");
                }
            }
            }
            computation.append(") .V");
            String code = computation.toString();
            return new OllirExprResult(code,computation);
        }
        //Handle the other cases where its different from the main
        else if (!node.get("value").equals("main")) {
            returnTypeMethods = table.getReturnType(node.get("value"));
            var OllirTypeMethodCall = OptUtils.toOllirType(returnTypeMethods);
            var temp = OptUtils.getTemp();
            var OllirExprTemp = temp + OllirTypeMethodCall;

            String code = "";
            if (node.getNumChildren() >= 2) {
                computation.append(OllirExprTemp);
                computation.append(ASSIGN);
                computation.append(OllirTypeMethodCall);
                computation.append(SPACE);
                computation.append(typeFunction);
                computation.append("(");
                computation.append(imp.getComputation());
                computation.append(",");
                computation.append("\"" + node.get("value") + "\"");
                computation.append(",");
                for (int i = 1; i < node.getNumChildren(); i++) {
                    //params
                    if (i == node.getNumChildren() - 1) {
                        computation.append(visit(node.getChild(i)).getComputation().substring(0, visit(node.getChild(i)).getComputation().length()));
                    } else {
                        computation.append(visit(node.getChild(i)).getComputation().substring(0, visit(node.getChild(i)).getComputation().length()));
                        computation.append(",");
                    }
                }
                computation.append(")");
                computation.append(OllirTypeMethodCall);
                computation.append(END_STMT);
                code = temp + OllirTypeMethodCall;
                return new OllirExprResult(code, computation);
            }

            // code to compute the children
            computation.append(OllirExprTemp);
            computation.append(ASSIGN);
            computation.append(OllirTypeMethodCall);
            computation.append(SPACE);
            computation.append(typeFunction);
            computation.append("(");
            computation.append(imp.getCode());
            computation.append(",");
            computation.append("\"" + node.get("value") + "\"");
            computation.append(")");
            computation.append(OllirTypeMethodCall);
            computation.append(END_STMT);
            code = temp;
            return new OllirExprResult(code, computation);
        }
        System.out.println("Didnt enter");
        return new OllirExprResult("", computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
