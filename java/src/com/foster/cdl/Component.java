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
        this.checkTypes(ast);
        this.replaceSubcompPorts(ast);
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
                    if (boolexprtype.compareTo("bool") == 0)
                    {
                        if (lefttype.compareTo(righttype) == 0)
                            return lefttype;
                        // assigned types don't match
                    }
                    // boolexpr is not boolean
                }
                else if (node.attributes.get("type").compareTo("[]"))
                {
                    
                }
                break;
            case BINARYOP:
                String lefttype = expressionType(node.getChild(0));
                String righttype = expressionType(node.getChild(1));
                if (lefttype.compareTo(righttype) == 0)
                {
                    if (true) // check operator matches lefttype
                    switch (node.attributes.get("type"))
                        case "!":
                        case "<":
                        case ">":
                        case "<=":
                        case ">=":
                        case "==":
                        case "!=":
                        case "+":
                        case "-":
                        case "*":
                        case "**":
                        case "/":
                        case "%":
                        case "^":
                        case "&":
                        case "|":
                        case ".":
                        return lefttype;
                }
                // assigned types don't match
                break;
            case UNARYOP:
                String type = expressionType(node.getChild(0));
                switch (node.attributes.get("type"))
                    case "!":
                        if (type.compareTo("bool") == 0)
                            return type;
                        break;
                    case "-":
                        if (type.compareTo("int") == 0 || type.compareTo("uint") == 0 || type.compareTo("vec") == 0)
                            return type;
                        break;
                    case "not":
                        if (type.compareTo("vec") == 0)
                            return type;
                        break;
                    case "()":
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
            s += String.format("\n    name: %s, type: %s", signal.get("name"), signal.get("type")) + (signal.get("type").compareTo("vec") == 0 ? String.format(", width : %s", signal.get("width")) : "");
        s += "\n  Generics:";
        for (Map<String, String> generic : this.getGenerics())
            s += String.format("\n    name: %s, type: %s", generic.get("name"), generic.get("type")) + (generic.get("type").compareTo("vec") == 0 ? String.format(", width : %s", generic.get("width")) : "");
        s += "\n  Ports:";
        for (Map<String, String> port : this.getPorts())
            s += String.format("\n    name: %s, type: %s, direction: %s", port.get("name"), port.get("type"), port.get("direction")) + (port.get("type").compareTo("vec") == 0 ? String.format(", width : %s", port.get("width")) : "");
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