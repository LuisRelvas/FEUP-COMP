package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.sql.Array;
import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var checkImport = root.getJmmChild(0);
        // Loop to check if there are any imports before the Class Declaration
        // Only accepts Imports and goes to the class declaration;
        for(int i = 0; i < root.getNumChildren(); i++)
        {
            if(root.getChild(i).getKind().equals("ImportDeclaration"))
            {
                continue;
            }
            else if(root.getChild(i).getKind().equals("ClassDecl"))
            {
                checkImport = root.getChild(i);
                break;
            }
            else
            {
                throw new IllegalArgumentException("Neither Import or Class Declaration");
            }
        }
        var classDecl = checkImport;
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);

        String className = classDecl.get("name");
        // Puts in an Array all the imports before the class declaration
        var imports = buildImports(root);
        var methods = buildMethods(classDecl);
        var fields = buildFields(classDecl);
        var extended = buildExtended(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(imports,className,extended, methods, fields, returnTypes, params, locals);
    }

    private static List<String> buildImports(JmmNode root)
    {
        List<String> imports = new ArrayList<>();

        for(int i = 0; i < root.getNumChildren(); i++)
        {
            if(root.getChild(i).getKind().equals("ImportDeclaration"))
            {
                imports.add(root.getChild(i).get("value"));
            }
        }

        return imports;
    }

    private static String buildExtended(JmmNode classDecl)
    {
        var checkIfImportExists = buildImports(classDecl.getParent());

        if(!classDecl.hasAttribute("ext") || classDecl.get("ext").isBlank())
        {
            return "";
        }

        String extension = classDecl.get("ext");

        for(int i = 0; i < checkIfImportExists.size(); i++)
        {
            if(checkIfImportExists.get(i).contains(extension))
            {
                return extension;
            }
        }

        throw new IllegalArgumentException("You cant extend a class that you didnt import");
    }
    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();
        for (var node : classDecl.getChildren(METHOD_DECL)) {
            boolean isArray = false;
            String varType = "";
            if(!node.get("methodName").equals("main")) {
                var returnType = node.getChild(0);
                if (returnType.getKind().equals("ArrayType")) {
                    varType = returnType.getChild(0).get("value");
                    isArray = true;
                }
                else if(returnType.hasAttribute("value"))
                {
                    varType = returnType.get("value");
                }
                map.put(node.get("methodName"),new Type(varType,isArray));
            }
        }
        return map;
    }


    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();
        String paramType = "";

        for (JmmNode method : classDecl.getChildren(METHOD_DECL)) {
            List<Symbol> params = new ArrayList<>();
            for (JmmNode param : method.getChildren(PARAM)) {
                List<String> paramNameString = param.getObjectAsList("paramName",String.class);
                List<JmmNode> typeNodes = param.getChildren();
                for (int i = 0; i < paramNameString.size(); i++) {
                    String paramName = paramNameString.get(i);
                    if(typeNodes.get(i).hasAttribute("value"))
                    {
                        paramType = typeNodes.get(i).get("value");
                    }
                    else if(typeNodes.get(i).getKind().equals("ArrayType"))
                    {
                        paramType = typeNodes.get(i).getChild(0).get("value");
                    }
                    boolean isArray = typeNodes.get(i).getKind().equals("ArrayType");
                    params.add(new Symbol(new Type(paramType, isArray), paramName));
                }
            }
            map.put(method.get("methodName"), params);
        }
        return map;
    }




    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("methodName"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        List<String> methods = new ArrayList<>();

            for(var k : classDecl.getChildren())
            {
                if(k.hasAttribute("methodName"))
                {
                    methods.add(k.get("methodName"));
                }
            }
        return methods;
    }

    private static List<Symbol> buildFields(JmmNode classDecl)
    {
        List <Symbol> symbols = new ArrayList<>();

        boolean isArray = false;

        for(int i = 0; i < classDecl.getNumChildren(); i++)
        {
            JmmNode child = classDecl.getChild(i);
            String varType = "";
            if(child.getKind().equals("VarDecl"))
            {
                String varName = child.get("name");
                if(child.getChild(0).hasAttribute("value"))
                {
                    varType = child.getChild(0).get("value");
                }
                else if(child.getChild(0).getKind().equals("ArrayType")) {
                    isArray = true;
                    varType = child.getChild(0).getChild(0).get("value");
                }
                symbols.add(new Symbol(new Type(varType,isArray),varName));
            }
        }
        return symbols;
    }


    private static Type getType(JmmNode varDecl) {
        String varType = "";
        boolean isArray = false;
        {
            if(varDecl.getChild(0).hasAttribute("value")){
                varType = varDecl.getChild(0).get("value");}
            else if(varDecl.getChild(0).getKind().equals("ArrayType"))
            {
                varType = varDecl.getChild(0).getChild(0).get("value");
                isArray = true;
            }
        }
        return new Type(varType,isArray);
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(getType(varDecl), varDecl.get("name")))
                .toList();
    }

}
