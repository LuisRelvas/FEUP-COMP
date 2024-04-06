package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.passes.UndeclaredVariable;


public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";

    private static String currentMethod;
    private static final String BOOLEAN_TYPE_NAME = "boolean";

    private static final String STRING_TYPE_NAME = "String";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static void setCurrentMethod(String methodName) {
        currentMethod = methodName;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded


        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case  BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF -> getVarExprType(expr,table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case VAR_DECL -> getVarDeclType(expr,table);
            case ASSIGN_STMT -> getAssignType(expr,table);
            case NEW_OBJECT_EXPR -> new Type(expr.get("value"), false);
            case METHOD_CALL_EXPR -> new Type(expr.get("value"), false);
            case ARRAY_CREATION_EXPR -> getArrayExprType(expr,table);
            case ARRAY_ACCESS_EXPR -> getArrayAccessExprType(expr,table);
            case THIS_EXPR -> new Type(table.getClassName(), false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getArrayAccessExprType(JmmNode arrayAccessExpr, SymbolTable table)
    {
        var expr = arrayAccessExpr.getChildren().get(0);
        var typeArray = getExprType(expr, table);
        var index = arrayAccessExpr.getChildren().get(1);
        var typeIndex = getExprType(index,table);
        if(!typeIndex.getName().equals(INT_TYPE_NAME) || typeIndex.isArray())
        {
            throw new RuntimeException("Index type is not an integer");
        }
        return new Type(typeArray.getName(), false);
    }
    private static Type getArrayExprType(JmmNode arrayExpr, SymbolTable table) {
        var typeParent  = getAssignType(arrayExpr.getParent(),table);
        for(int i = 0; i < arrayExpr.getNumChildren(); i++)
        {
            var type = getExprType(arrayExpr.getChildren().get(i),table);
            if(!type.getName().equals(typeParent.getName()))
            {
                throw new RuntimeException("Array type is not the same as the parent type");
            }
        }
        return new Type(typeParent.getName(), true);
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");
        // Search the attributes that are going to be in the binary expression
        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "<", ">", "<=", ">=", "==", "!=" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private static Type getAssignType(JmmNode assign, SymbolTable table)
    {
        var left = assign.get("ID");
        var locals = table.getLocalVariables(currentMethod);
        var fields = table.getFields();
        var params = table.getParameters(currentMethod);
        var imports = table.getImports();
        for(Symbol s : locals)
        {
            if (s.getName().equals(left))
            {
                return s.getType();
            }
        }
        for(Symbol s : fields)
        {
            if (s.getName().equals(left))
            {
                return s.getType();
            }
        }
        for (Symbol s : params)
        {
            if (s.getName().equals(left))
            {
                return s.getType();
            }
        }
        for (String s: imports)
        {
            if (s.equals(left))
            {
                return new Type(left, false);
            }
        }
        return null;
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {

        // TODO: Simple implementation that needs to be expanded
        // Get the method name
        var kind = Kind.fromString(varRefExpr.getKind());
        var varName = varRefExpr.get("value");
        var defined = getVarDeclType(varRefExpr,table);
        if (defined != null)
        {
            return defined;
        }
        else
        {
            return null;
        }
    }

    private static Type getVarDeclType(JmmNode varDecl, SymbolTable table) {
        var locals = table.getLocalVariables(currentMethod);
        var fields = table.getFields();
        var params = table.getParameters(currentMethod);
        var imports = table.getImports();
        var varName = varDecl.get("value");
        for(Symbol s : locals)
        {
            if (s.getName().equals(varName))
            {
                return s.getType();
            }
        }
        for(Symbol s : fields)
        {
            if (s.getName().equals(varName))
            {
                return s.getType();
            }
        }
        for (Symbol s : params)
        {
            if (s.getName().equals(varName))
            {
                return s.getType();
            }
        }
        for (String s: imports)
        {
            if (s.equals(varName))
            {
                return new Type(varName, false);
            }
        }
        return null;

    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
