/*
Component.java - Reed Foster
Class for storage of component information
*/

package com.foster.cdl;

import java.util.*;

public class Component
{
    public static final Set<Nodetype> DECLAREDIDENTIFIERNODES = new HashSet<Nodetype>(Arrays.asList(new Nodetype[] {Nodetype.PORT, Nodetype.GENDEC, Nodetype.SIGDEC, Nodetype.COMPDEC}));

    public final String name;
    public final Tree ast;
    private Map<Nodetype, Set<Map<String, String>>> declaredIdentifiers;

    Component(String source)
    {
        Parser p = new Parser(new Lexer(source));
        this.ast = p.parse();
        this.name = this.ast.attributes.get("name");
        this.declaredIdentifiers = new HashMap<Nodetype, Set<Map<String, String>>>();
        for (Nodetype n : DECLAREDIDENTIFIERNODES)
            this.declaredIdentifiers.put(n, new HashSet<Map<String, String>>());
        this.getIdentifiers(ast);
    }

    private static void typeError(String message) throws TypeError
    {
        throw new TypeError(message);
    }

    private static void nameError(String message) throws NameError
    {
        throw new NameError(message);
    }

    private void checkTypes(Tree node)
    {
        if (node.nodetype == Nodetype.BINARYOP && node.attributes.get("type").compareTo("<=") == 0)
        {
            //child0 width and type must match child1 width and type
            String leftname;
        }
        else
        {
            for (Tree child : node.getChildren())
                this.checkTypes(child);
        }
    }

    private static boolean isIntegral(String type)
    {
        return type.equals("int") || type.equals("uint");
    }

    private static boolean isNumeric(String type)
    {
        return isIntegral(type) || type.equals("vec");
    }

    private String expressionType(Tree node) // checks type consistency and usage of undeclared identifiers
    {
        switch (node.nodetype)
        {
            case TERNARYOP:
                //if ternaryq do this. need to add stuff for ternary splice
                if (node.attributes.get("type").compareTo("?"))
                {
                    String boolexprtype = expressionType(node.getChild(0));
                    String lefttype = expressionType(node.getChild(1));
                    String righttype = expressionType(node.getChild(2));
                    if (boolexprtype.equals("bool"))
                    {
                        if (lefttype.compareTo(righttype) == 0)
                            return lefttype;
                        // assigned types don't match
                    }
                    // boolexpr is not boolean
                }
                else if (node.attributes.get("type").compareTo("[]"))
                {
                    List<String> children = node.getChildren();
                    String lefttype = expressionType(children.get(0));
                    String uppertype = expressionType(children.get(1));
                    String lowertype = uppertype;
                    if (children.size() == 3)
                        lowertype = expressionType(children.get(2));
                    if (lefttype.equals("vec"))
                    {
                        if (isIntegral(uppertype) && isIntegral(lowertype))
                            return "vec";
                        // bounds aren't integers
                    }
                    // non-vector object is being spliced
                }
                break;
            case BINARYOP:
                String lefttype = expressionType(node.getChild(0));
                String righttype = expressionType(node.getChild(1));
                switch (node.attributes.get("type"))
                    case "<":
                    case ">":
                    case "<=":
                    case ">=":
                    case "==":
                    case "!=":
                        if (isNumeric(lefttype) && isNumeric(righttype))
                            return "bool";
                        break;
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                        if (isNumeric(lefttype) && isNumeric(righttype))
                            return lefttype;
                        break;
                    case "%":
                        if (isIntegral(lefttype) && isIntegral(righttype))
                            return lefttype;
                        break;
                    case "**":
                        if (isNumeric(lefttype) && isIntegral(righttype))
                            return lefttype;
                        break;
                    case "^":
                    case "&":
                    case "|":
                        if (lefttype.equals("bool") && righttype.equals("bool"))
                            return "bool";
                        break;
                    case ".":
                        //return error, should've been replaced with temp string in dependency checking
                        return righttype; //not correct, but leave it for now
                        break;
                // assigned types don't match
                break;
            case UNARYOP:
                String type = expressionType(node.getChild(0));
                switch (node.attributes.get("type"))
                    case "!":
                        if (type.equals("bool"))
                            return type;
                        break;
                    case "-":
                        if (isNumeric(type))
                            return type;
                        break;
                    case "not":
                        if (type.equals("vec"))
                            return type;
                        break;
                    case "()":
                        return type;
                break;
            case IDENTIFIER:
                // if identifier is not in declared identifiers list, throw error
                for (Set<Map<String, String>> declaredidset : this.declaredIdentifiers.values())
                {
                    for (Map<String, String> declaredid : declaredidset)
                    {
                        if (declaredid.get("name").compareTo(node.attributes.get("value")))
                            return declaredid.get("type");
                    }
                }
                //id isn't declared
                nameError(String.format("Identifier %s is not declared", node.attributes.get("value")));
                break;
            case CONSTANT:
                switch (node.attributes.get("type"))
                {
                    case "DECINTCONST":
                    case "BININTCONST":
                    case "HEXINTCONST":
                        return "int";
                    case "BINVECCONST":
                    case "HEXVECCONST":
                        return "vec";
                    case "BOOLCONST":
                        return "bool";
                }
                typeError(String.format("%s is not a valid type"))
                break;
        }
        return null;
    }

    /**
    * Initializes signals, ports, generics, and subcomponents class fields.
    * These fields are used to verify that all identifiers that are used are signals, ports, or generics, and that types are usec properly
    * @param node current node of component AST. when called this should be the root node of type Nodetype.COMPONENT
    */
    private void getIdentifiers(Tree node)
    {
        if (DECLAREDIDENTIFIERNODES.contains(node.nodetype))
        {
            Set<Map<String, String>> declarations = this.declaredIdentifiers.get(node.nodetype);
            declarations.add(new HashMap<String, String>(node.attributes));
            this.declaredIdentifiers.put(node.nodetype, declarations);
        }
        else
        {
            for (Tree child : node.getChildren())
                this.getIdentifiers(child);
        }
    }

    public Set<Map<String, String>> getSignals()
    {
        return this.declaredIdentifiers.get(Nodetype.SIGDEC);
    }

    public Set<Map<String, String>> getPorts()
    {
        return this.declaredIdentifiers.get(Nodetype.PORT);
    }

    public Set<Map<String, String>> getGenerics()
    {
        return this.declaredIdentifiers.get(Nodetype.GENDEC);
    }

    public Set<Map<String, String>> getSubcomponents()
    {
        return this.declaredIdentifiers.get(Nodetype.COMPDEC);
    }

    public String toString()
    {
        String s = "Component: " + this.ast.attributes.get("name");
        s += String.format("\n  Tree:\n    %s\n  Signals:", this.ast.visit(2));
        for (Map<String, String> signal : this.getSignals())
            s += String.format("\n    name: %s, type: %s", signal.get("name"), signal.get("type")) + (signal.get("type").equals("vec") ? String.format(", width : %s", signal.get("width")) : "");
        s += "\n  Generics:";
        for (Map<String, String> generic : this.getGenerics())
            s += String.format("\n    name: %s, type: %s", generic.get("name"), generic.get("type")) + (generic.get("type").equals("vec") ? String.format(", width : %s", generic.get("width")) : "");
        s += "\n  Ports:";
        for (Map<String, String> port : this.getPorts())
            s += String.format("\n    name: %s, type: %s, direction: %s", port.get("name"), port.get("type"), port.get("direction")) + (port.get("type").equals("vec") ? String.format(", width : %s", port.get("width")) : "");
        s += "\n  Subcomponents:";
        for (Map<String, String> subcomponent : this.getSubcomponents())
            s += String.format("\n    name: %s, type: %s", subcomponent.get("name"), subcomponent.get("type"));
        return s;
    }

    public static void main(String[] args)
    {
        String source = "component C1{vec[3] gen;port{input vec[3] foo; input uint za; input bool re; output int bar;}arch{signal bool banana; C2 monkey = new C2();}}";
        Component c = new Component(source);
        System.out.println(c);
    }
}