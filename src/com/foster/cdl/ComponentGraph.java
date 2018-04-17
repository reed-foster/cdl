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
    */
    ComponentGraph(String source)
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
        this.topname = this.dependencyGraph.rootVertex();
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
    * Wrapper method for throwing GenericErrors
    * @param message String message to be printed
    */
    private static void genericError(String message) throws GenericError
    {
        throw new GenericError(message);
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
    * Verifies semantics of all component definitions:
    *  + check type/width in assignments
    *  + verify all used identifiers are declared
    *  + root component has no generics
    *  + all generic assigments use generics or constants
    *  + all splice operations use generics or constants
    */
    private void verifyAllComponents()
    {
        // check root component has no generics
        if (!this.components.get(this.topname).getGenerics().isEmpty())
            genericError(String.format("top component %s cannot contain generics", this.topname));
        // verify all components
        for (Component component : this.components.values())
        {
            this.currentComponent = component.name;
            this.verifyAssignments(component.ast);
            this.verifyDeclarations(component.ast);
            this.verifyWidths(new HashMap<String, String>(), this.topname); // since top component can't have generics, call verifyWidths with an empty genericMap
        }


    }

    private void verifyWidths(Map<String, Tuple<String>> genericMap, String componentName)
    {
        List<Tree> subcomponents = getSubcomponentTrees(this.components.get(componentName).ast);
        for (Tree subcomponent : subcomponents)
        {
            Map<String, Tuple<String>> newGenericMap = new HashMap<String, Tuple<String>>();
            for (Tree genericAssign : subcomponent.getChildren())
            {
                String key = genericAssign.getChild(0).attributes.get("name");
                Tuple<String> value = evaluateConstantExpression(genericMap, genericAssign.getChild(1));
                newGenericMap.put(key, value);
            }
            this.verifyWidths(newGenericMap, subcomponent.attributes.get("name"));
        }
    }

    /**
    * Recursively visit each subtree of an expression, replacing identifiers with values retrieved from genericMap
    * @param genericMap Map<String, Tuple<String>> of all generic's names and their assigned values in the particular component instance
    * @param node Tree reference to subtree to evaluate (initally called with root node of expression)
    * @return Tuple<String> result of evaluation of the expression, containing the value (field a) and type (field b)
    */
    private Tuple<String> evaluateConstantExpression(Map<String, Tuple<String>> genericMap, Tree node)
    {
        switch(node.nodetype)
        {
            case TERNARYOP:
                if (node.attributes.get("type").equals("?"))
                {
                    Tuple<String> boolean = evaluateConstantExpression(genericMap, node.getChild(0));
                    return boolean.equals("false") ? evaluateConstantExpression(genericMap, node.getChild(1)) : evaluateConstantExpression(genericMap, node.getChild(2));
                }
                if (node.attributes.get("type").equals("[]"))
                {
                    // this one'll be a little tricky to evaluate
                    // we're also gonna check types for splice bounds here because verifyExpression() didn't have access to generics data
                }
                break;
            case BINARYOP:
                switch (node.attributes.get("type"))
                {
                    case "and":
                    case "or":
                    case "nand":
                    case "nor":
                    case "xor":
                    case "xnor":
                    case "<":
                    case ">":
                    case "<=":
                    case ">=":
                    case "==":
                    case "!=":
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                    case "%":
                    case "**":
                    case "^":
                    case "&":
                    case "|":
                }
            case UNARYOP:
            case IDENTIFIER: // must be generic because verifyDeclarations already checked this
            case CONSTANT:
        }
        // ya done goofed, shouldn't ever be here
    }

    /**
    * Helper method to retrieve the Tree representation of all subcomponents instantiated in the component whose AST is supplied to the method
    * @param node Tree reference to subtree from which to retrieve subcomponent instances (initially called with root node of AST)
    */
    private static List<Tree> getSubcomponentTrees(Tree node)
    {
        if (node.nodetype == Nodetype.ARCH)
        {
            List<Tree> subcomponents = new ArrayList<Tree>();
            for (Tree child : node.getChildren())
            {
                if (child.nodetype == Nodetype.COMPDEC)
                {
                    subcomponents.add(child);
                }
            }
            return subcomponents;
        }
        else
        {
            for (Tree child : node.getChildren())
                return getSubcomponentTrees(child);
        }
    }


    /**
    * Verifies that generics/constants are used for all vector declarations and component generic assignments
    * @param node reference to subtree to verify (initially called with the root node of the component's AST)
    */
    private void verifyDeclarations(Tree node)
    {
        if (node.nodetype == Nodetype.SIGDEC || node.nodetype == Nodetype.PORT || node.nodetype == Nodetype.GENDEC)
        {
            if (node.attributes.get("type").equals("vec"))
            {
                if (node.getChildren().isEmpty())
                    return;
                for (Tree child : node.getChildren())
                {
                    if (!this.expressionIsConstant(child))
                        typeError(String.format("declarations of %s of type vector contains a non-constant width", node.attributes.get("name")));
                }
            }
        }
        else if (node.nodetype == Nodetype.COMPDEC)
        {
            if (node.getChildren().isEmpty())
                return;
            for (Tree genericAssign : node.getChildren())
            {
                if (!this.expressionIsConstant(genericlist))
                    typeError(String.format("component instantiation for %s contains non-constant generic assignments", node.attributes.get("name")));
            }
        }
        else
        {
            for (Tree child : node.getChildren())
                this.verifyDeclarations(child);
        }
    }

    /**
    * Verifies that an expression contains only generics and/or constants
    * @param node reference to expression to check
    */
    private boolean expressionIsConstant(Tree node)
    {
        if (node.nodetype == Nodetype.CONSTANT)
            return true;
        else if (node.nodetype == Nodetype.IDENTIFIER)
        {
            for (Map<String, String> gendec : this.components.get(this.currentComponent).getGenerics())
            {
                if (gendec.get("name").equals(node.attributes.get("value")))
                    return true;
            }
            return false;
        }
        else // expressions can only contain constants, identifiers, or unary, binary, and ternary operators
        {
            for (Tree child : node.getChildren())
            {
                if (!this.expressionIsConstant(child))
                    return false;
            }
            return true;
        }
    }

    /**
    * Verifies that, within a single component, all used identifiers are defined and types match for binary operators
    * @param node reference to subtree to verify (initially called with the root node of the component's AST)
    */
    private void verifyAssignments(Tree node)
    {
        if (node.nodetype == Nodetype.BINARYOP && node.attributes.get("type").equals("<="))
        {
            this.verifyExpression(node);
        }
        else
        {
            for (Tree child : node.getChildren())
                this.verifyAssignments(child);
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
                    String type = verifyExpression(children.get(0));
                    if (lefttype.equals("vec"))
                        return "vec";
                    typeError("non-vector types cannot be spliced");
                }
                break;
            case BINARYOP:
                String lefttype = verifyExpression(node.getChild(0));
                String righttype = verifyExpression(node.getChild(1));
                switch (node.attributes.get("type")) // need to add bitwise operators (e.g. "or", "and", etc.)
                {
                    case "and":
                    case "or":
                    case "nand":
                    case "nor":
                    case "xor":
                    case "xnor":
                        if (lefttype.equals("vec") && righttype.equals("vec"))
                            return "vec";
                        break;
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