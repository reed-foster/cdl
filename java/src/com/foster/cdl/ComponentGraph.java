/*
ComponentGraph.java - Reed Foster
Stores all parsed components
*/

package com.foster.cdl;

import java.util.*;

class ComponentGraph
{
    private Graph dependencyGraph;
    private Map<String, Component> components;
    private String topname;

    // for use with type-checking, keeps track of which component the current syntax tree being processed belongs to
    private String currentComponent;

    /**
    * Constructor
    * @param source String source, can contain multiple component defintions
    * @param top String name of top/root component in hierarchy
    */
    ComponentGraph(String source, String top)
    {
        this.dependencyGraph = new Graph();
        this.components = new HashMap<String, Component>();
        this.topname = top;
        // split multiple component definitions
        int start = 0;
        int end = 0;
        do
        {
            start = source.indexOf("component", end);
            end = source.indexOf("component", start + 1);
            end = end == -1 ? source.length() - 1 : end;
            Component c = new Component(source.substring(start, end));
            this.components.put(c.name, c);
        } while (source.indexOf("component", end) != -1);
        this.orderDependencies();
        this.checkCyclicity();
        this.verifyAllComponents();
    }

    /**
    * Wrapper method for throwing TypeErrors
    * @param message String message to be printed
    */
    private static void typeError(String message) throws TypeError
    {
        throw new TypeError(message);
    }

    /**
    * Wrapper method for throwing NameErrors
    * @param message String message to be printed
    */
    private static void nameError(String message) throws NameError
    {
        throw new NameError(message);
    }

    /**
    * Iteratively generate a graph of component dependencies.
    * Creates edge between componentName and all of its known subcomponents
    */
    private void orderDependencies()
    {
        for (Component component : this.components.values())
        {
            Set<Map<String, String>> subcomponents = component.getSubcomponents();
            if (subcomponents.isEmpty())
                continue;
            for (Map<String, String> subcomponent : subcomponents)
            {
                String name = subcomponent.get("type");
                if (!this.components.containsKey(name))
                    nameError(String.format("no component declaration for %s found", name));
                this.dependencyGraph.addEdge(component.name, name);
            }
        }
    }

    /**
    * Checks if there is a circular reference in component definitions
    * @throws CircularReferenceError if a cycle is detected in the component dependency graph
    */
    private void checkCyclicity() throws CircularReferenceError
    {
        if (!this.dependencyGraph.acyclic())
            throw new CircularReferenceError("Circular reference detected");
    }

    /**
    * Verifies that all used identifiers are defined and that types match for binary operators
    */
    private void verifyAllComponents()
    {
        for (Component component : this.components.values())
        {
            this.currentComponent = component.name;
            this.verifyComponent(component.ast);
        }
    }

    /**
    * Verifies that, within a single component, all used identifiers are defined and types match for binary operators
    * @param node reference to subtree to verify (initially called with the root node of the component's AST)
    */
    private void verifyComponent(Tree node)
    {
        if (node.nodetype == Nodetype.BINARYOP && node.attributes.get("type").equals("<="))
        {
            this.verifyExpression(node);
        }
        else
        {
            for (Tree child : node.getChildren())
                this.verifyComponent(child);
        }
    }

    /**
    * Verifies that, within a single expression, all used identifiers are defined and types match for binary operators
    * @param node reference to subtree to verify.
    */
    private String verifyExpression(Tree node)
    {
        switch (node.nodetype)
        {
            case TERNARYOP:
                if (node.attributes.get("type").equals("?")) // if conditional ternary operator
                {
                    String boolexprtype = verifyExpression(node.getChild(0));
                    String lefttype = verifyExpression(node.getChild(1));
                    String righttype = verifyExpression(node.getChild(2));
                    if (boolexprtype.equals("bool"))
                    {
                        if (lefttype.equals(righttype))
                            return lefttype;
                        // assigned types don't match
                        typeError("conditional assignment types don't match");
                    }
                    // boolexpr is not boolean
                    typeError("expression before \"?\" operator is not boolean");
                }
                else if (node.attributes.get("type").equals("[]")) // if splice operator
                {
                    List<Tree> children = node.getChildren();
                    String lefttype = verifyExpression(children.get(0));
                    String uppertype = verifyExpression(children.get(1));
                    String lowertype = uppertype;
                    if (children.size() == 3)
                        lowertype = verifyExpression(children.get(2)); // lower and upper bound are the same
                    if (lefttype.equals("vec"))
                    {
                        if (isIntegral(uppertype) && isIntegral(lowertype))
                            return "vec";
                        // bounds aren't integers
                        typeError("bounds of vector splice must be integers");
                    }
                    // non-vector object is being spliced
                    typeError("non-vector types cannot be spliced");
                }
                break;
            case BINARYOP:
                String lefttype = verifyExpression(node.getChild(0));
                String righttype = verifyExpression(node.getChild(1));
                switch (node.attributes.get("type"))
                {
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
                            return lefttype;
                        break;
                    case ".":
                        // compinstID.portID
                        String compinstid = node.getChild(0).attributes.get("name");
                        String portid = node.getChild(1).attributes.get("name");
                        for (Map<String, String> compdec : this.components.get(this.currentComponent).getSubcomponents())
                        {
                            if (compdec.get("name").equals(compinstid)) // first find compinstid's component type
                            {
                                String component = compdec.get("type");
                                for (Map<String, String> port : this.components.get(component).getPorts()) // now find which port portid refers to
                                {
                                    if (port.get("name").equals(portid))
                                        return port.get("type");
                                }
                                // component doesn't contain port
                                nameError(String.format("port %s not found in %s's interface.", portid, component));
                            }
                        }
                        // undeclared component
                        nameError(String.format("no component declaration for %s found", compinstid));
                        break;
                }
                // assigned types don't match
                typeError(String.format("%s is undefined for types %s and %s", node.attributes.get("type")));
                break;
            case UNARYOP:
                String type = verifyExpression(node.getChild(0));
                switch (node.attributes.get("type"))
                {
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
                }
                break;
            case IDENTIFIER:
                // if identifier is not in declared identifiers list, throw error
                // first try signals
                Map<Nodetype, Set<Map<String, String>>> declaredIdentifiers = this.components.get(this.currentComponent).getDeclaredIdentifiers();
                for (Nodetype n : declaredIdentifiers.keys())
                {
                    if (n == Nodetype.SIGDEC || n == Nodetype.PORT || n == Nodetype.GENDEC)
                    {
                        for (Map<String, String> declaration : declaredIdentifiers.get(n))
                        {
                            if (declaration.get("name").equals(node.attributes.get("value")))
                            {
                                return declaration.get("type");
                            }
                        }
                    }
                }
                // id isn't declared
                nameError(String.format("identifier %s is not declared", node.attributes.get("value")));
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
                typeError(String.format("%s is not a valid type", node.attributes.get("type")));
                break;
            default:
                // need to come up with a more informative error message, but if you get here you done messed up
                typeError("unexpected structure found within signal assignment");
                break;
        }
        return null;
    }

    /**
    * Helper method for type-checking
    * @return true if type = "int" or "uint", false otherwise
    */
    private static boolean isIntegral(String type)
    {
        return type.equals("int") || type.equals("uint");
    }

    /**
    * Helper method for type-checking
    * @return true if type is integral or = "vec", false otherwise
    */
    private static boolean isNumeric(String type)
    {
        return isIntegral(type) || type.equals("vec");
    }

    public static void main(String[] args)
    {
        String source = "component C1{port{}arch{C2 c2 = new C2();C3 c3 = new C3();}}\n" + 
                        "component C2{port{}arch{C4 c3 = new C4();}}\n" +
                        "component C3{port{}arch{}}";
        ComponentGraph cg = new ComponentGraph(source, "C1");
        System.out.println("Dependency test passed");
    }
}