/*
Component.java - Reed Foster
Class for storage of component information
*/

package com.foster.cdl;

import java.util.*;

public class Component
{
    public static final Set<Nodetype> DECLAREDIDENTIFIERNODES = new HashSet<Nodetype>(Arrays.asList(new Nodetype[] {Nodetype.PORT, Nodetype.GENDEC, Nodetype.SIGDEC, Nodetype.CONST, Nodetype.COMPDEC}));

    public final String name;
    public final Tree ast;
    private Map<Nodetype, Set<DeclaredIdentifier>> declaredIdentifiers; // each map of maps maps names of declared identifiers (keys) to their attributes (values; from ast)

    Component(String source)
    {
        Parser p = new Parser(new Lexer(source));
        this.ast = p.parse();
        this.name = this.ast.attributes.get("name");
        this.declaredIdentifiers = new HashMap<Nodetype, Set<DeclaredIdentifier>>();
        for (Nodetype n : DECLAREDIDENTIFIERNODES)
            this.declaredIdentifiers.put(n, new HashSet<DeclaredIdentifier>());
        this.getIdentifiers(ast);
    }

    /**
    * Initializes signals, ports, generics, and subcomponents class fields.
    * These fields are used to verify that all identifiers that are used are signals, ports, or generics, and that types are usec properly
    * @param node current node of component AST. when called this should be the root node of type Nodetype.COMPONENT
    * @throws NameError when an identifier is declared more than once
    */
    private void getIdentifiers(Tree node) throws NameError
    {
        if (DECLAREDIDENTIFIERNODES.contains(node.nodetype))
        {
            Set<DeclaredIdentifier> declarations = this.declaredIdentifiers.get(node.nodetype);
            for (DeclaredIdentifier declaration : declarations)
            {
                if (declaration.name.equals(node.attributes.get("name")))
                    throw new NameError(String.format("(%s) declared multiple times", node.attributes.get("name")));
            }
            declarations.add(new DeclaredIdentifier(node));
            this.declaredIdentifiers.put(node.nodetype, declarations);
        }
        else
        {
            for (Tree child : node.getChildren())
                this.getIdentifiers(child);
        }
    }

    public Set<DeclaredIdentifier> getSignals()
    {
        return this.declaredIdentifiers.get(Nodetype.SIGDEC);
    }

    public Set<DeclaredIdentifier> getPorts()
    {
        return this.declaredIdentifiers.get(Nodetype.PORT);
    }

    public Set<DeclaredIdentifier> getGenerics()
    {
        return this.declaredIdentifiers.get(Nodetype.GENDEC);
    }

    public Set<DeclaredIdentifier> getConstants()
    {
        return this.declaredIdentifiers.get(Nodetype.CONST);
    }

    public Set<DeclaredIdentifier> getSubcomponents()
    {
        return this.declaredIdentifiers.get(Nodetype.COMPDEC);
    }

    public Map<Nodetype, Set<DeclaredIdentifier>> getDeclaredIdentifiers()
    {
        Map<Nodetype, Set<DeclaredIdentifier>> declaredIDs = new HashMap<Nodetype, Set<DeclaredIdentifier>>();
        for (Nodetype n : DECLAREDIDENTIFIERNODES)
            declaredIDs.put(n , new HashSet<DeclaredIdentifier>(this.declaredIdentifiers.get(n)));
        return declaredIDs;
    }

    public String toString()
    {
        String s = "Component: " + this.ast.attributes.get("name");
        s += String.format("\n  Tree:\n    %s\n  Signals:", this.ast.visit(2));
        for (DeclaredIdentifier signal : this.getSignals())
            s += String.format("\n    name: %s, type: %s", signal.name, signal.type) + (signal.type.equals("vec") ? String.format(", width : %s", signal.declaration.attributes.get("width")) : "");
        s += "\n  Generics:";
        for (DeclaredIdentifier generic : this.getGenerics())
            s += String.format("\n    name: %s, type: %s", generic.name, generic.type) + (generic.type.equals("vec") ? String.format(", width : %s", generic.declaration.attributes.get("width")) : "");
        s += "\n  Ports:";
        for (DeclaredIdentifier port : this.getPorts())
            s += String.format("\n    name: %s, type: %s, direction: %s", port.name, port.type, port.declaration.attributes.get("direction")) + (port.type.equals("vec") ? String.format(", width : %s", port.declaration.attributes.get("width")) : "");
        s += "\n  Subcomponents:";
        for (DeclaredIdentifier subcomponent : this.getSubcomponents())
            s += String.format("\n    name: %s, type: %s", subcomponent.name, subcomponent.type);
        return s;
    }

    public static void main(String[] args)
    {
        String source = "component C1{vec[3] gen;port{input vec[3] foo; input uint za; input bool re; output int bar;}arch{signal bool banana; C2 monkey = new C2();}}";
        Component c = new Component(source);
        System.out.println(c);
    }
}