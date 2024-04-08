package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.INTEGER_LITERAL;
import static pt.up.fe.comp2024.ast.Kind.TYPE;
import static pt.up.fe.comp2024.ast.Kind.*;

public class OptUtils {
    private static int tempNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {


        TYPES.contains(typeNode.getKind());

        String typeName = typeNode.get("value");

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type)
    {
        return toOllirType(type.getName());
    }

    private static String toOllirType(String typeName) {
        if(typeName.equals("io"))
        {
            return ""; // io is not a type
        }
        String type = "." + switch (typeName) {
                                case "int" -> "i32";
                                case "boolean" -> "bool";
                                case "String" -> "array.String";
                                case "void" -> "V";
                                default -> throw new NotImplementedException(typeName);
        };

        return type;
    }



}
