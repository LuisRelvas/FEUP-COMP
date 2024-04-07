package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.Arrays;
import java.util.Set;

public enum Kind {
    PROGRAM,
    CLASS_DECL,
    IMPORT_DECLARATION,
    VAR_DECL,
    TYPE,
    METHOD_DECL,
    PARAM,
    ASSIGN_STMT,

    UNARY_EXPR,
    ARRAY_LENGTH_EXPR,
    RETURN_STMT,
    BINARY_EXPR,

    PARENTHESIS_EXPR,

    INTEGER_LITERAL,

    METHOD_CALL_EXPR,

    ARRAY_CREATION_EXPR,
    BOOLEAN_LITERAL,
    IF_STMT,

    NEW_OBJECT_EXPR,
    VAR_EXPR,

    THIS_EXPR,
    NEW_ARRAY_EXPR,

    WHILE_STMT,
    ARRAY_ASSIGN_STMT,
    ARRAY_ACCESS_EXPR,

    INT_TYPE,
    BOOLEAN_TYPE,

    CLASS_TYPE,
    STRING_TYPE,
    VAR_REF, IMPORT_DECL(), EXPR_STMT(), VAR_REF_EXPR();

    public static final Set<Kind> TYPES = Set.of(INT_TYPE, BOOLEAN_TYPE, CLASS_TYPE, STRING_TYPE);
    private static final Set<Kind> STATEMENTS = Set.of(ASSIGN_STMT, RETURN_STMT);
    private static final Set<Kind> EXPRESSIONS = Set.of(BINARY_EXPR, INTEGER_LITERAL,VAR_REF,VAR_EXPR);

    private final String name;

    private Kind(String name) {
        this.name = name;
    }

    private Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    public static Kind fromString(String kind) {

        for (Kind k : Kind.values()) {
            if (k.getNodeName().equals(kind)) {
                return k;
            }
        }
        throw new RuntimeException("Could not convert string '" + kind + "' to a Kind");
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    /**
     * @return true if this kind represents a statement, false otherwise
     */
    public boolean isStmt() {
        return STATEMENTS.contains(this);
    }

    /**
     * @return true if this kind represents an expression, false otherwise
     */
    public boolean isExpr() {
        return EXPRESSIONS.contains(this);
    }

    public boolean isType() {return TYPES.contains(this);}

    /**
     * Tests if the given JmmNode has the same kind as this type.
     *
     * @param node
     * @return
     */
    public boolean check(JmmNode node) {
        return node.getKind().equals(getNodeName());
    }

    /**
     * Performs a check and throws if the test fails. Otherwise, does nothing.
     *
     * @param node
     */
    public void checkOrThrow(JmmNode node) {
        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Performs a check on all kinds to test and returns false if none matches. Otherwise, returns true.
     *
     * @param node
     * @param kindsToTest
     * @return
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {

        for (Kind k : kindsToTest) {

            // if any matches, return successfully
            if (k.check(node)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Performs a check an all kinds to test and throws if none matches. Otherwise, does nothing.
     *
     * @param node
     * @param kindsToTest
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            // throw if none matches
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }
}
