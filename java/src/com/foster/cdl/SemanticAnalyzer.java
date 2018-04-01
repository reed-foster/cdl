/*
SemanticAnalyzer.java - Reed Foster
Class for verification of semantics of parsed source code (i.e. "yes, it's valid syntax, but does it make sense?")
*/

package com.foster.cdl;

import java.util.*;

public class SemanticAnalyzer
{
    private Map<String, Component> components;
    private String topCompName;

    private DependencyGraph dependencyGraph;

    private Map<String, Set<Map<String, String>>> tempSignals; // holds attributes of temp signals that need to be declared
    private String currentComponent; // used by replaceCompoundID and addTempSigs for replacing compound identifiers

    /**
    * Constructor for semantic analysis of sources
    * Allows for multiple component definitions in one source string, however the root component must be passed first
    * @param source String containing one or more source files. All component dependencies must be included
    * @param top String name of top-level component
    */
    SemanticAnalyzer(String source, String top)
    {
        this.topCompName = top;
        this.dependencyGraph = new DependencyGraph();
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
        Component component = this.components.get(componentname);
        Set<Map<String, String>> subcomponents = component.getSubcomponents();
        if (subcomponents.isEmpty())
            return;
        for (Map<String, String> subcomponent : subcomponents)
        {
            String name = subcomponent.get("name");
            this.dependencyGraph.addEdge(component.name, name);
            this.orderDependencies(name);
        }
        if (!this.dependencyGraph.acyclic())
            circularError("Circular reference detected.");
    }

    private void replaceCompoundIDs()
    {
        for (Component component : this.components.values())
        {
            this.currentComponent = component.name;
            this.replaceCompoundID(component.ast);

        }
    }

    private void replaceCompoundID(Tree node)
    {
        for (int i = 0; i < node.numChildren(); i++)
        {
            Tree child = node.getChild(i);
            if (child.nodetype == Nodetype.BINARYOP && child.attributes.get("type").equals("."))
            {
                String portname = child.getChild(1).attributes.get("name"); // get the name of the identfier to the right of the period operator
                for (Component component : this.components)
                {
                    for (Map<String, String> port : component.getPorts())
                    {
                        if (port.get("name").equals(portname))
                        {
                            String name = c.name + "_" + port.get("name")
                            Map<String, String> signalattr = new HashMap<String, String>();
                            signalattr.put("name", name);
                            signalattr.put("type", port.get("type"));
                            if (port.containsKey("width"))
                            {
                                signalattr.put("width", port.get("width"));
                            }
                            this.tempSignals.get(this.currentComponent).add(signalattr); // need to modify so tempSignals differentiates between temp signals for each component, probably just add an additional parameter to the method
                            node.removeChild(i);
                            node.addChild(new Tree Nodetype.IDENTIFIER, signalattr); // replace compound identfier with temp. signal
                        }
                    }
                }
            }
            else if (!child.children.isEmpty())
            {
                for (Tree grandchild : child.getChildren())
                {
                    this.replaceCompoundID(grandchild);
                }
            }
        }
    }

    private void connectTempSigs()
    {
        for (String compname : this.components.keys())
        {
            this.currentComponent = compname;
            this.addTempSigs(this.components.get(compname).ast);
        }
    }

    private void addTempSigs(Tree node)
    {
        if (node.nodetype == Nodetype.ARCH)
        {
            for (Map<String, String> attributes : this.tempSignals.get(this.currentComponent))
            {
                node.addChild(new Tree(Nodetype.SIGDEC, attributes));
            }
        }
        else if (!node.children.isEmpty())
        {
            for (Tree child : node.getChildren())
            {
                this.addTempSigs(child);
            }
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