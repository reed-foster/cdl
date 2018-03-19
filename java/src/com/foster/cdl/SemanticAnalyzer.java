/*
SemanticAnalyzer.java - Reed Foster
Class for verification of semantics of parsed source code (i.e. "yes, it's valid syntax, but does it make sense?")
*/

package com.foster.cdl;

import java.util.*;

public class SemanticAnalyzer
{
    private Map<String, Component> components;

    private Set<String> visitedcomponentnames;

    private ComponentTree dependencytree;

    /**
    * Constructor for semantic analysis of sources
    * Allows for multiple component definitions in one source string, however the root component must be passed first
    *
    */
    SemanticAnalyzer(String source)
    {
        this.components = new HashMap<String, Component>();
        this.splitMultiComponentSource(source);
    }

    private static void circularError(String message) throws CircularReferenceError
    {
        throw new CircularReferenceError(message);
    }

    public void verify()
    {
        return;
    }

    private void splitMultiComponentSource(String source)
    {
        int startindex = 0;
        int endindex = 0;
        do
        {
            startindex = source.indexOf("component", endindex);
            endindex = source.indexOf("component", startindex + 1);
            endindex = endindex == -1 ? source.length() - 1 : endindex;
            Component c = new Component(source.substring(startindex, endindex));
            this.components.put(c.name, c);
        } while (source.indexOf("component", endindex) != -1);
        return;
    }

    private void orderDependencies(String componentname)
    {

        // should make a tree; iterate through component list, adding each component (unless it's already in the tree) and its subcomponents (regardless of whether or not it's already in the tree)
        // if the component to add isn't in the tree but has subcomponents already in the tree, then there's a problem; throw an error 
        this.visitedcomponentnames.add(componentname);
        Component component = this.components.get(componentname);
        Set<Map<String, String>> subcomponents = component.getSubcomponents();
        if (subcomponents.isEmpty())
        {
            return;
        }
        for (Map<String, String> subcomponent : subcomponents)
        {
            String name = subcomponent.get("name");
            if (this.visitedcomponentnames.contains(name))
            {
                circularError(String.format("Nested Components Detected: %s is defined as both a child and parent of %s.", name, componentname));
            }
            this.orderDependencies(name);
        }
    }

    private void sigAssignTypeCheck()
    {
        for (Component component : this.components.values())
        {
            
        }
    }

    public static void main(String[] args)
    {
        String source = "component C1{port{input int foo; output vec[3] bar;}arch{C2 c2 = new C2();C3 c3 = new C3();}}\n" + 
                        "component C2{port{}arch{C3 c3 = new C3();}}";
    }
}