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
    * Wrapper method for throwing IndexError
    * @param message String message to be printed
    */
    private static void indexError(String message) throws IndexError
    {
        throw new IndexError(message);
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
            this.verifyExpressions(component.ast);
            this.verifyWidths(new HashMap<String, String>(), this.topname); // since top component can't have generics, call verifyWidths with an empty genericMap
        }


    }

    private void verifyWidths(Map<String, Map<String, String>> genericMap, String componentName)
    {
        List<Tree> subcomponents = getSubcomponentTrees(this.components.get(componentName).ast);
        for (Tree subcomponent : subcomponents)
        {
            Map<String, Map<String, String>> newGenericMap = new HashMap<String, Map<String, String>>();
            for (Tree genericAssign : subcomponent.getChildren())
            {
                String key = genericAssign.getChild(0).attributes.get("name");
                Map<String, String> value = evaluateConstantExpression(genericMap, genericAssign.getChild(1));
                newGenericMap.put(key, value);
            }
            this.verifyWidths(newGenericMap, subcomponent.attributes.get("name"));
        }
    }

    /**
    * Recursively visit each subtree of an expression, replacing identifiers with values retrieved from genericMap
    * @param genericMap Map<String, Map<String, String>> of all generic's names and their assigned values in the particular component instance
    * @param node Tree reference to subtree to evaluate (initally called with root node of expression)
    * @return Map<String, String> result of evaluation of the expression, containing the value, type, and width (for vecs)
    * width default to 0 for nonvector types
    * value string in decimal representation of the value (0 for false, 1 for true)
    * type is a member of the set {bool, int, uint, vec}
    */
    private Map<String, String> evaluateConstantExpression(Map<String, Map<String, String>> genericMap, Tree node)
    {
        switch(node.nodetype)
        {
            case TERNARYOP:
                if (node.attributes.get("type").equals("?"))
                {
                    Map<String, String> bool = evaluateConstantExpression(genericMap, node.getChild(0));
                    return bool.get("value").equals("0") ? evaluateConstantExpression(genericMap, node.getChild(1)) : evaluateConstantExpression(genericMap, node.getChild(2));
                }
                else if (node.attributes.get("type").equals("[]"))
                {
                    Map<String, String> vec = evaluateConstantExpression(genericMap, node.getChild(0));
                    Map<String, String> upper = evaluateConstantExpression(genericMap, node.getChild(1));
                    Map<String, String> lower = upper;
                    if (node.numChildren() == 3)
                        lower = evaluateConstantExpression(genericMap, node.getChild(2));
                    if (!(isIntegral(upper.get("type") && lower.get("type"))))
                        typeError("bounds of vector splice must be integers");
                    int vecwidth = Integer.parseInt(vec.get("width"));
                    int upperInt = Integer.parseInt(upper.get("value"));
                    int lowerInt = Integer.parseInt(lower.get("value"));
                    upperInt = upperInt < 0 ? vecwidth + upperInt : upperInt; // if bounds for splicing are negative, they refer to number of places before end of vector
                    lowerInt = lowerInt < 0 ? vecwidth + lowerInt : lowerInt;
                    if (upperInt > vecwidth - 1 || lowerInt > vecwidth - 1 || upperInt < 0 || lowerInt < 0)
                        indexError(String.format("bounds of vector splicing (%d, %d) out of range", upperInt, lowerInt));
                    int width = upperInt - lowerInt;
                    width = upperInt < lowerInt : -width : width;
                    width++; // increment because counting (e.g. you have ten fingers, but 10(th finger) - 1(st finger) is 9)
                    String value;
                    if (upperInt < lowerInt)
                        value = vec.get("value").substring(upperInt, lowerInt + 1);
                    else
                        value = vec.get("value").substring(lowerInt, upperInt + 1);
                    Map<String, Sting> result = new HashMap<String, String>();
                    result.put("type", "vec");
                    result.put("value", value);
                    result.put("width", Integer.toString(width));
                    return result;
                }
                break;
            case BINARYOP:
                Map<String, String> left = evaluateConstantExpression(genericMap, node.getChild(0));
                Map<String, String> right = evaluateConstantExpression(genericMap, node.getChild(1));
                if ((left.get("width").equals(right.get("width"))))
                    return evaluateBinaryOp(left, right, node.attributes.get("type"));
                break;
            case UNARYOP:
                return evaluateUnaryOp(evaluateConstantExpression(genericMap, node.getChild(0)), node.attributes.get("type"));
            case IDENTIFIER: // must be generic because verifyDeclarations already checked this
                return genericMap.get(node.attributes.get("name"));
            case LITERAL:
                Map<String, String> result = new HashMap<String, String>();
                int radix = 0;
                switch (node.attributes.get("type").substring(0, 3))
                {
                    case "DEC":
                        radix = 10;
                        break;
                    case "HEX":
                        radix = 16;
                        break;
                    case "BIN":
                        radix = 2;
                        break;
                }
                if (radix != 0)
                {
                    result.put("value", Long.toString(Long.parseLong(node.attributes.get("value"), radix))); // parse string as a long, then convert it back into a string
                    result.put("type", node.attributes.get("type").substring(3, 6).toLowerCase()); // type is in characters 3 - 5 of tokentype (which in turn is the "type" field of the literal)
                    if (result.get("type").equals("vec"))
                    {
                        // (radix == 2 ? 1 : 4) is the coefficient of the length of the string; if the string is a hex vector
                        // then the total number of chars will be 1/4 the number of bits required to represent the value
                        result.put("width", Integer.toString(node.attributes.get("value").length() * (radix == 2 ? 1 : 4)));
                    }
                    else
                        result.put("width", "0");
                }
                else // radix is only still 0 if literal is boolean
                {
                    result.put("value", node.attributes.get("value"),equals("true") ? "1" : "0");
                    result.put("type", "bool");
                    result.put("width", "0");
                }
                return result;
        }
        // ya done goofed, shouldn't ever be here
    }

    /**
    * Helper method for evaluating binary operations
    * @param left Map<String, String> value, type, and width of left argument
    * @param right Map<String, String> value, type, and width of right argument
    * @param operator String operator (as it appears in source code - e.g. "xor", "|", "+", "-")
    */
    private static Map<String, String> evaluateBinaryOp(Map<String, String> left, Map<String, String> right, String operator)
    {
        Map<String, String> result = new HashMap<String, String>();
        String type = "";
        switch (operator)
        {
            case "<": case ">": case "<=": case ">=": case "==": case "!=":
                type = "bool";
                break;
            case "and": case "or": case "nand": case "nor": case "xor": case "xnor": case "+": case "-": case "*": case "/": case "%": case "**": case "^": case "&": case "|":
                type = left.get("type");
                break;
        }
        result.put("type", type);
        result.put("width", left.get("width"));
        long a = Long.parseLong(left.get("value"));
        long b = Long.parseLong(right.get("value"));
        long value = 0;
        switch (operator)
        {
            case "&":
                if (left.get("type").equals("vec")) // right must be a vec too
                    value = Long.parseLong(left.get("value") + right.get("value"));
                    break;
                // fall through; & is a boolean operator
            case "and":
                value = a & b;
                break;
            case "or": case "|":
                value = a | b;
                break;
            case "nand":
                value = ~(a & b);
                break;
            case "nor":
                value = ~(a | b);
                break;
            case "xor": case "^":
                value = a ^ b;
                break;
            case "xnor":
                value = ~(a ^ b);
                break;
            case "<":
                value = a < b ? 1 : 0;
                break;
            case ">":
                value = a > b ? 1 : 0;
                break;
            case "<=":
                value = a <= b ? 1 : 0;
                break;
            case ">=":
                value = a >= b ? 1 : 0;
                break;
            case "==":
                value = a == b ? 1 : 0;
                break;
            case "!=":
                value = a != b ? 1 : 0;
                break;
            case "+":
                value = a + b;
                break;
            case "-":
                value = a - b;
                break;
            case "*":
                value = a * b;
                break;
            case "/":
                value = a / b;
                break;
            case "%":
                value = a % b;
                break;
            case "**":
                value = 1;
                for (long i = 0; i < b; i++)
                    value *= a;
                break;
        }
        result.put("value", Long.toString(value));
        return result;
    }

    /**
    * Helper method for evaluating unary operations
    * @param left Map<String, String> value, type, and width of argument
    * @param operator String operator (as it appears in source code - e.g. "not", "!", "-")
    */
    private static Map<String, String> evaluateUnaryOp(Map<String, String> operand, String operator)
    {
        Map<String, String> result = new HashMap<String, String>();
        result.put("type", operand.get("type"));
        result.put("width", operand.get("width"));
        long value = Long.parseLong(operand.get("value"));
        switch (operator)
        {
            case "!": case "not":
                value = ~value;
                break;
            case "-":
                value = -value;
                break;
            case "()":
                break;
        }
        result.put("value", Long.toString(value));
        return result;
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
    * Verifies that an expression contains only generics and/or constants
    * @param node reference to expression to check
    * @param allowGenerics boolean if false, method will return false if encountering generics; otherwise it will not return false
    */
    private boolean expressionIsConstant(Tree node, boolean allowGenerics)
    {
        if (node.nodetype == Nodetype.LITERAL)
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
                if (!this.expressionIsConstant(child, allowGenerics))
                    return false;
            }
            return true;
        }
    }

    /**
    * Verifies that, within a single component, all used identifiers are defined and types match for binary operators
    * @param node reference to subtree to verify (initially called with the root node of the component's AST)
    */
    private void verifyExpressions(Tree node)
    {
        if (node.nodetype == Nodetype.BINARYOP && node.attributes.get("type").equals("<="))
        {
            this.verifyExpression(node);
        }
        else if (node.nodetype == Nodetype.PORT || node.nodetype == Nodetype.SIDGEC || node.nodetype == Nodetype.GENDEC)
        {
            if (node.attributes.get("type").equals("vec"))
            {
                if (!this.expressionIsConstant(node.getChild(0), node.nodetype != Nodetype.GENDEC)) // pass true if node is not GENDEC; prevents generics from containing other generics
                    typeError(String.format("declarations of %s of type vector contains a non-constant width", node.attributes.get("name")));
                this.verifyExpression(node.getChild(0));
            }
        }
        else if (node.nodetype == Nodetype.COMPDEC) //todo add checking for constant expressions; also remove verifyDeclarations method
        {
            if (node.getChildren().isEmpty())
                return;
            for (Tree genericAssign : node.getChildren())
            {
                if (!this.expressionIsConstant(genericAssign, true))
                    typeError(String.format("component instantiation for %s contains non-constant generic assignments", node.attributes.get("name")));
                this.verifyExpression(genericAssign);
            }
        }
        else
        {
            for (Tree child : node.getChildren())
                this.verifyExpressions(child);
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
                switch (node.attributes.get("type"))
                {
                    case "and": case "or": case "nand": case "nor": case "xor": case "xnor":
                        // bitise operator
                        if (lefttype.equals("vec") && righttype.equals("vec"))
                            return "vec";
                        break;
                    case "<": case ">": case "<=": case ">=": case "==": case "!=":
                        // relational operator
                        if (isNumeric(lefttype) && isNumeric(righttype))
                            return "bool";
                        break;
                    case "+": case "-": case "*": case "/":
                        // arithmetic valid for vectors
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
                    case "&":
                        // boolean and
                        if (lefttype.equals("bool") && righttype.equals("bool"))
                            return lefttype;
                        // concatenation
                        else if (lefttype.equals("vec") && righttype.equals("vec"))
                            return lefttype;
                        break;
                    case "^": case "|":
                        // boolean operator
                        if (lefttype.equals("bool") && righttype.equals("bool"))
                            return lefttype;
                        break;
                    case ".":
                        // compound identifier
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
            case LITERAL:
                switch (node.attributes.get("type"))
                {
                    case "DECINTLITERAL":
                    case "BININTLITERAL":
                    case "HEXINTLITERAL":
                        return "int";
                    case "BINVECLITERAL":
                    case "HEXVECLITERAL":
                        return "vec";
                    case "BOOLLITERAL":
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