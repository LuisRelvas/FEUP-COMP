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

    private static Boolean isStatic;
    private static final String BOOLEAN_TYPE_NAME = "boolean";

    private static final String STRING_TYPE_NAME = "String";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static void setCurrentMethod(String methodName) {
        currentMethod = methodName;
    }

    public static void setStatic(Boolean isSta) {
        isStatic = isSta;
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
            case METHOD_CALL_EXPR -> getMethodCallExprType(expr,table);
            case ARRAY_CREATION_EXPR -> getArrayExprType(expr,table);
            case ARRAY_ACCESS_EXPR -> getArrayAccessExprType(expr,table);
            case THIS_EXPR -> new Type(table.getClassName(), false);
            case NEW_ARRAY_EXPR -> new Type(getExprType(expr.getChild(0),table).getName(),true);
            case ARRAY_LENGTH_EXPR -> new Type(INT_TYPE_NAME, false);
            case UNARY_EXPR -> getExprType(expr.getChild(0),table);
            case PARENTHESIS_EXPR -> getExprType(expr.getChild(0),table);
            case ARRAY_ASSIGN_STMT -> getAssignType(expr,table);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }


    private static Type getMethodCallExprType(JmmNode methodCallExpr, SymbolTable table)
    {
        var returnType = new Type("int", false);
        if(table.getMethods().contains(methodCallExpr.get("value")))
        {
            returnType = table.getReturnType(methodCallExpr.get("value"));
        }
        //if we dont know the method, we assume it is a method from the imports and give the correct type
        else if(!table.getImports().isEmpty() && !table.getSuper().isEmpty() )
        {
            returnType = getExprType(methodCallExpr.getParent(),table);
        }
        return returnType;
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
        //check if the array has parent
        if(!arrayExpr.getParent().hasAttribute("value"))
        {
            //check the type of the elements inside the array
            var type = getExprType(arrayExpr.getChildren().get(0),table);
            for(int i = 1; i < arrayExpr.getNumChildren(); i++)
            {
                var typeChild = getExprType(arrayExpr.getChildren().get(i),table);
                if(!type.getName().equals(typeChild.getName()))
                {
                    throw new RuntimeException("Array type is not the same as the parent type");
                }
            }
            return new Type(type.getName(), true);
        }
        else {
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
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");
        // Search the attributes that are going to be in the binary expression
        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private static Type getAssignType(JmmNode assign, SymbolTable table)
    {
        var left = assign.get("value");
        var optionalLocals = table.getLocalVariablesTry(currentMethod);
        var fields = table.getFields();
        var optionalParams = table.getParametersTry(currentMethod);
        var imports = table.getImports();
        if(optionalLocals.isPresent())
        {
            var locals = optionalLocals.get();
            for(Symbol s : locals)
            {
                if (s.getName().equals(left))
                {
                    return s.getType();
                }
            }
        }
        for(Symbol s : fields)
        {
            if (s.getName().equals(left))
            {
                return s.getType();
            }
        }
        if(optionalParams.isPresent()) {
            var params = optionalParams.get();
            for (Symbol s : params) {
                if (s.getName().equals(left)) {
                    return s.getType();
                }
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

        // Get the method name
        var kind = Kind.fromString(varRefExpr.getKind());
        var varName = varRefExpr.get("value");
        var imports = table.getImports();
        //always check the last substring of the imports list;
        for(String importItem : imports)
        {
            int lastDotIndex = importItem.lastIndexOf('.');
            if(lastDotIndex != -1)
            {
                String lastSubstring = importItem.substring(lastDotIndex + 1);
                if(lastSubstring.equals(varName))
                {
                    return new Type(importItem, false);
                }
            }
        }
        var definedAsDeclaration = getVarDeclType(varRefExpr,table);
        var optionalParameters = table.getParametersTry(currentMethod);
        if(optionalParameters.isPresent()) {
            var parameters = optionalParameters.get();
            for (Symbol s : parameters) {
                if (s.getName().equals(varName)) {
                    return s.getType();
                }
            }
        }
        if(!currentMethod.equals("main"))
        {
            //look in the fields
            var fields = table.getFields();
            for(Symbol s : fields)
            {
                if(s.getName().equals(varName))
                {
                    return s.getType();
                }
            }
        }
        if (definedAsDeclaration != null)
        {
            return definedAsDeclaration;
        }
        else
        {
            return null;
        }
    }

    private static Type getVarDeclType(JmmNode varDecl, SymbolTable table) {
        var optionalLocals = table.getLocalVariablesTry(currentMethod);
        var fields = table.getFields();
        var optionalParams = table.getParametersTry(currentMethod);
        var imports = table.getImports();
        var varName = varDecl.get("value");
        if(optionalLocals.isPresent()) {
            var locals = optionalLocals.get();
            for (Symbol s : locals) {
                if (s.getName().equals(varName)) {
                    return s.getType();
                }
            }
        }
        if(optionalParams.isPresent()) {
            var params = optionalParams.get();
            for (Symbol s : params) {
                if (s.getName().equals(varName)) {
                    return s.getType();
                }
            }
        }
        for(Symbol s : fields)
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
