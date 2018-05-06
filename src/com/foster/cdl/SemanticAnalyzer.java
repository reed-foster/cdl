/*
SemanticAnalyzer.java - Reed Foster
Stores and verifies all parsed components

TODO:
    check for duplicate port/arch declarations
    check all identifiers used in generic assingment are actual generic names
    check that all generic names are used in generic assignment
*/

package com.foster.cdl;

import java.util.*;

class SemanticAnalyzer
{
    private Graph dependencyGraph;
    private Map<String, Component> components;
    private Map<String, Boolean> hasPortDec;
    private Map<String, Boolean> hasArchDec;
    private String topname;

    // for use with type-checking, keeps track of which component the current syntax tree being processed belongs to
    private String currentComponent;

    /**
    * Constructor
    * @param source String source, can contain multiple component defintions
    */
    SemanticAnalyzer(String source)
    {
        this.dependencyGraph = new Graph();
        this.components = new HashMap<String, Component>();
        this.hasPortDec = new HashMap<String, Boolean>();
        this.hasArchDec = new HashMap<String, Boolean>();
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
            this.hasPortDec.put(c.name, false);
            this.hasArchDec.put(c.name, false);
        } while (source.indexOf("component", end) != -1);
        this.orderDependencies(); // adds edges between each dependency in this.dependencyGraph
        this.checkCyclicity();
        this.topname = this.components.size() == 1 ? this.components.keySet().iterator().next() : this.dependencyGraph.rootVertex();
        this.verifyAllComponents();
    }

    /**
    * Accessor method for all components
    * @return this.components
    */
    public Map<String, Component> getComponents()
    {
        return this.components;
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
    * Wrapper method for throwing PortErrors
    * @param message String message to be printed
    */
    private static void portError(String message) throws PortError
    {
        throw new PortError(message);
    }

    /**
    * Wrapper method for throwing ArchErrors
    * @param message String message to be printed
    */
    private static void archError(String message) throws ArchError
    {
        throw new ArchError(message);
    }

    /**
    * Iteratively generate a graph of component dependencies.
    * Creates edge between componentName and all of its known subcomponents
    */
    private void orderDependencies()
    {
        for (Component component : this.components.values())
        {
            Set<DeclaredIdentifier> subcomponents = component.getSubcomponents();
            if (subcomponents.isEmpty())
                continue;
            for (DeclaredIdentifier subcomponent : subcomponents)
            {
                String name = subcomponent.type;
                if (!this.components.containsKey(name))
                    nameError(String.format("no component declaration for (%s) found", name));
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
    *  + check root component has no generics
    *  + check all identifiers used are declared
    *  + check all expressions in declarations use generics or constants
    *  + check types for all expressions are valid
    * Future:
    *  + check width for all expressions - recurse through all instances (would need to do fancy stuff to check using only component definition)
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
            this.verifyPortArch(component.ast);
            this.verifyIdentifiers(component.ast);
            this.verifyConstantExpressions(component.ast);
            this.verifyTypes(component.ast);
        }
    }

    /**
    * Verifies that there are no duplicate port or architecture definitions in a component
    * @param node Tree reference to subtree to be checked; called on root node of component ast
    * @throws PortError if duplicate port statements are found, ArchError if duplicate arch statements are found
    */
    private void verifyPortArch(Tree node)
    {
        if (node.nodetype == Nodetype.PORTDEC)
        {
            if (this.hasPortDec.get(this.currentComponent))
                portError(String.format("multiple port definitions found in component (%s)", this.currentComponent));
            else
                this.hasPortDec.put(this.currentComponent, true);
        }
        else if (node.nodetype == Nodetype.ARCH)
        {
            if (this.hasArchDec.get(this.currentComponent))
                archError(String.format("multiple arch definitions found in component (%s)", this.currentComponent));
            else
                this.hasArchDec.put(this.currentComponent, true);
        }
        else
        {
            for (Tree child : node.getChildren())
                this.verifyPortArch(child);
        }
    }

    /**
    * Checks that all identifiers that are used are declared
    * @param node Tree reference to subtree to be verified (initially called with root node of AST)
    */
    private void verifyIdentifiers(Tree node)
    {
        if (node.nodetype == Nodetype.BINARYOP && node.attributes.get("type").equals("."))
        {
            // compound identifier
            String compinstID = node.getChild(0).attributes.get("name");
            String portID = node.getChild(1).attributes.get("name");
            for (DeclaredIdentifier subcomp : this.components.get(this.currentComponent).getSubcomponents())
            {
                if (subcomp.name.equals(compinstID))
                {
                    for (DeclaredIdentifier port : this.components.get(subcomp.type).getPorts())
                    {
                        if (port.name.equals(portID))
                            return;
                    }
                    nameError(String.format("no defintion for port (%s) found in component (%s)", portID, subcomp.name));
                }
            }
            nameError(String.format("component (%s) not delcared", compinstID));
        }
        else if (node.nodetype == Nodetype.IDENTIFIER)
        {
            // identifier
            Map<Nodetype, Set<DeclaredIdentifier>> declaredIDs = this.components.get(this.currentComponent).getDeclaredIdentifiers();
            for (Nodetype n : declaredIDs.keySet())
            {
                if (n == Nodetype.SIGDEC || n == Nodetype.PORT || n == Nodetype.GENDEC || n == Nodetype.CONST)
                {
                    for (DeclaredIdentifier declaration : declaredIDs.get(n))
                    {
                        if (declaration.name.equals(node.attributes.get("name")))
                            return;
                    }
                }
            }
            nameError(String.format("undeclared identifier (%s)", node.attributes.get("name")));
        }
        else
        {
            for (Tree child : node.getChildren())
                this.verifyIdentifiers(child);
        }
    }

    /**
    * Checks that all expressions that should be constant
    * (i.e. those in declarations of vector types and those in generic assignments) are indeed constant
    * @param node Tree reference to subtree to be verified (initially called with root node of AST)
    */
    private void verifyConstantExpressions(Tree node)
    {
        if (node.nodetype == Nodetype.SIGDEC || node.nodetype == Nodetype.PORT || node.nodetype == Nodetype.CONST || node.nodetype == Nodetype.GENDEC)
        {
            if (node.attributes.get("type").equals("vec"))
            {
                if (!this.expressionIsConstant(node.getChild(0), node.nodetype != Nodetype.GENDEC))
                    typeError(String.format("declarations of %s of type vector contains a non-constant width", node.attributes.get("name")));
            }
        }
        else if (node.nodetype == Nodetype.COMPDEC)
        {
            for (Tree child : node.getChildren())
            {
                if (!this.expressionIsConstant(child.getChild(1), true))
                    typeError(String.format("component instantiation for %s contains non-constant generic assignments", node.attributes.get("name")));
            }
        }
        else
        {
            for (Tree child : node.getChildren())
            {
                this.verifyConstantExpressions(child);
            }
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
        {
            return true;
        }
        else if (node.nodetype == Nodetype.IDENTIFIER)
        {
            if (!allowGenerics)
                return false;
            for (DeclaredIdentifier gendec : this.components.get(this.currentComponent).getGenerics())
            {
                if (gendec.name.equals(node.attributes.get("value")))
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
    * Finds all expressions in component definition and verifies that their types are correct
    * @param node Tree reference to subtree to be verified (initially called with root node of AST)
    */
    private void verifyTypes(Tree node)
    {
        switch (node.nodetype)
        {
            case GENDEC:
            case PORT:
            case SIGDEC:
            case CONST:
                if (node.attributes.get("type").equals("vec"))
                {
                    String type = this.verifyExpressionType(node.getChild(0));
                    if (!isIntegral(type))
                        typeError(String.format("type (%s) not valid for vector width declaration", type));
                }
                return;
            case COMPDEC:
                for (Tree genericAssign : node.getChildren())
                {
                    String lhsType;
                    String genericName = genericAssign.getChild(0).attributes.get("name");
                    Set<DeclaredIdentifier> generics = this.components.get(node.attributes.get("type")).getGenerics();
                    for (DeclaredIdentifier generic : generics)
                    {
                        if (generic.name.equals(genericName))
                        {
                            lhsType = generic.type;
                            String rhsType = this.verifyExpressionType(genericAssign.getChild(1));
                            if (!lhsType.equals(rhsType))
                                typeError(String.format("generic assignment (%s) and (%s) types don't match", lhsType, rhsType));
                            return;
                        }
                    }
                }
                return;
            case BINARYOP: // should only be assignment
                if (node.attributes.get("type").equals("<="))
                {
                    String lhsType = this.verifyExpressionType(node.getChild(0));
                    String rhsType = this.verifyExpressionType(node.getChild(1));
                    if (!lhsType.equals(rhsType))
                        typeError(String.format("signal assignment (%s) and (%s) types don't match", lhsType, rhsType));
                    return;
                }
                break;
                // don't know how you could get here
            default:
                for (Tree child : node.getChildren())
                {
                    this.verifyTypes(child);
                }
        }
    }

    /**
    * Helper method for verifyTypes; returns the type of an expression
    * @param node Tree reference to node of subtree to find the type of (initially called with root node of expression)
    */
    private String verifyExpressionType(Tree node)
    {
        switch (node.nodetype)
        {
            case TERNARYOP:
                if (node.attributes.get("type").equals("?"))
                {
                    String arg1 = this.verifyExpressionType(node.getChild(0));
                    String arg2 = this.verifyExpressionType(node.getChild(1));
                    String arg3 = this.verifyExpressionType(node.getChild(2));
                    if (arg1.equals("bool"))
                    {
                        if (arg2.equals(arg3))
                            return arg2;
                        typeError(String.format("conditional assignment types (%s) and (%s) don't match", arg2, arg3));
                    }
                    typeError(String.format("expression before (?) operator is not boolean"));
                }
                else if (node.attributes.get("type").equals("[]"))
                {
                    String vectype = this.verifyExpressionType(node.getChild(0));
                    if (vectype.equals("vec"))
                    {
                        String upperType = this.verifyExpressionType(node.getChild(1));
                        String lowerType = upperType;
                        if (node.numChildren() == 3)
                            lowerType = this.verifyExpressionType(node.getChild(2));
                        if (isIntegral(upperType) && isIntegral(lowerType))
                        {
                            return "vec";
                        }
                        typeError("non-integer bounds cannot be used for splice operations");
                    }
                    typeError(String.format("non-vector types (%s) cannot be spliced", vectype));
                }
                break;
            case BINARYOP:
                String lhsType, rhsType;
                if (node.attributes.get("type").equals("."))
                {
                    String compinstID = node.getChild(0).attributes.get("name");
                    String portID = node.getChild(1).attributes.get("name");
                    for (DeclaredIdentifier subcomp : this.components.get(this.currentComponent).getSubcomponents())
                    {
                        if (subcomp.name.equals(compinstID))
                        {
                            for (DeclaredIdentifier port : this.components.get(subcomp.type).getPorts())
                            {
                                if (port.name.equals(portID))
                                    return port.type;
                            }
                        }
                    }
                    // won't get here because there is a definition for compinstID.portID because declaredIDs have already been checked
                }
                lhsType = this.verifyExpressionType(node.getChild(0));
                rhsType = this.verifyExpressionType(node.getChild(1));
                switch (node.attributes.get("type"))
                {
                    case "and": case "or": case "nand": case "nor": case "xor": case "xnor":
                        // bitise operator
                        if (lhsType.equals("vec") && rhsType.equals("vec"))
                            return "vec";
                        break;
                    case "<": case ">": case "<=": case ">=": case "=": case "!=":
                        // relational operator
                        if (isNumeric(lhsType) && isNumeric(rhsType))
                            return "bool";
                        break;
                    case "+": case "-": case "*": case "/":
                        // arithmetic valid for vectors
                        if (isNumeric(lhsType) && isNumeric(rhsType))
                            return lhsType;
                        break;
                    case "%":
                        if (isIntegral(lhsType) && isIntegral(rhsType))
                            return lhsType;
                        break;
                    case "**":
                        if (isNumeric(lhsType) && isIntegral(rhsType))
                            return lhsType;
                        break;
                    case "&":
                        // boolean and
                        if (lhsType.equals("bool") && rhsType.equals("bool"))
                            return lhsType;
                        // concatenation
                        else if (lhsType.equals("vec") && rhsType.equals("vec"))
                            return lhsType;
                        break;
                    case "^": case "|":
                        // boolean operator
                        if (lhsType.equals("bool") && rhsType.equals("bool"))
                            return lhsType;
                        break;
                    case ".":
                        // wut, how'd we get here
                }
                typeError(String.format("operator (%s) is undefined for types (%s) and (%s)", node.attributes.get("type"), lhsType, rhsType));
                break;
            case UNARYOP:
                String type = this.verifyExpressionType(node.getChild(0));
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
                String name = node.attributes.get("name");
                Map<Nodetype, Set<DeclaredIdentifier>> declaredIDs = this.components.get(this.currentComponent).getDeclaredIdentifiers();
                for (Nodetype n : declaredIDs.keySet())
                {
                    if (n == Nodetype.SIGDEC || n == Nodetype.PORT || n == Nodetype.GENDEC || n == Nodetype.CONST)
                    {
                        for (DeclaredIdentifier declaredID : declaredIDs.get(n))
                        {
                            if (declaredID.name.equals(name))
                                return declaredID.type;
                        }
                    }
                }
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
                break;
        }
        return null;
    }

    /**
    * Helper method for type-checking
    * @return true if type = "int", false otherwise
    */
    private static boolean isIntegral(String type)
    {
        return type.equals("int");
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
        SemanticAnalyzer cg = new SemanticAnalyzer(source);
        System.out.println("Dependency test passed");
    }
}