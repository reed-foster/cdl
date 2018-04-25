/*
VHDLGenerator.java - Reed Foster
Stores all parsed components
*/

package com.foster.cdl;

import java.util.*;

public class VHDLGenerator
{
    private Map<String, Component> components;
    private Map<String, Set<DeclaredIdentifier>> tempSignals; // maps each component name to a set of declaredIdentifiers

    private String currentComponent;

    public final output;

    VHDLGenerator(String source)
    {
        SemanticAnalyzer s = new SemanticAnalyzer(source);
        this.components = s.getComponents();
        this.getAllTempSignals();
        this.output = "";
        for (String compname : this.components.keys())
        {
            this.currentComponent = compname;
            output += this.visit(this.components.get(compname));
        }

    }

    private void getAllTempSignals()
    {
        for (String componentName : this.components.keys())
        {
            Component component = this.components.get(componentName);
            this.tempSignals.put(componentName, this.getTempSignals(componentName, component.ast));
        }
    }

    private Set<DeclaredIdentifier> getTempSignals(String componentName, Tree node)
    {
        Set<DeclaredIdentifier> sigDecs = new HashSet<DeclaredIdentifier>();
        if (node.nodetype == Nodetype.BINARYOP && node.attributes.get("type").equals("."))
        {
            List<Tree> children = new ArrayList<Tree>();
            Map<String, String> attributes = new HashMap<String, String>();

            String compinstID = node.getChlid(0).attributes.get("name");
            String portID = node.getChlid(1).attributes.get("name");
            attributes.put("compname", compinstID);
            attributes.put("portname", portID);

            for (DeclaredIdentifier subcomp : this.components.get(componentName).getSubcomponents())
            {
                if (subcomp.name.equals(compinstID))
                {
                    for (DeclaredIdentifier port : this.components.get(subcomp.type).getPorts())
                    {
                        if (port.name.equals(portID))
                        {
                            attributes.put("type", port.type);
                            if (port.type.equals("vec"))
                                children.add(node.getChild(0));
                        }
                    }
                }
            }

            Tree declaration = new Tree(Nodetype.SIGDEC, attributes, children);
            sigDecs.add(new DeclaredIdentifier(declaration));
        }
        else
        {
            for (Tree child : node.getChlidren())
            {
                sigDecs.addAll(this.getTempSignals(componentName, child));
            }
        }
        return sigDecs;
    }

    private String visit(Tree node)
    {
        switch (node.nodetype)
        {
            case COMPONENT:
                String header = "library ieee;\nuse ieee.std_logic_1164.all;\nuse ieee.numeric_std.all;";
                String generics = "";
                String ports = "";
                String arch = "";
                for (Tree child : node.getChlidren())
                {
                    switch (child.nodetype)
                    {
                        case GENDEC:
                            generics += this.visit(child);
                            break;
                        case PORTDEC:
                            ports += this.visit(child);
                            break;
                        case ARCH:
                            arch += this.visit(child);
                            break;
                    }
                }
                String body = header + "entity " + node.attributes.get("name") + "\n";
                body += generics.length() > 1 ? "generic\n(\n" + generics + "\n);" : "";
                body += ports + "\nend entity";
                body += arch;
                return body;
            case GENDEC:
                return node.attributes.get("name") + " : " + this.getType(node);
            case PORTDEC:
                String body = ""
                for (Tree child : node.getChlidren())
                {
                    body += this.visit(child) + ";\n";
                }
                if (body.length() == 0)
                    return "port();";
                return "port\n(\n" + body.substring(0, body.length() - 2) + ");"; // substring to remove last trailing semicolon
            case PORT:
                String direction = node.attributes.get("direction");
                direction = direction.substring(0, direction.length() - 3); // input => in, output => out
                return node.attributes.get("name") + " : " + direction + " " + this.getType(node);
            case ARCH:
                String body = "architecture structural of " + this.currentComponent + "is";
                String delcarations = "";
                String assignments = "";
                for (Tree child : node.getChlidren())
                {
                    switch (child.nodetype)
                    {
                        case COMPDEC:
                        case SIGDEC:
                        case CONST:
                        case BINARYOP:
                    }
                    declarations += this.visit(child) + ";\n";
                }
            case COMPDEC:
            case SIGDEC:
            case CONST:
            case TERNARYOP:
                if (node.attributes.get("type").equals("?"))
                    return this.visit(node.getChild(1)) + " when " + this.visit(node.getChild(0)) + " else " + this.visit(node.getChild(2));
                else if (node.attributes.get("type").equals("[]"))
                {
                    String upper = this.visit(node.getChild(1))
                    String lower = node.numChildren() == 2 ? upper : this.visit(node.getChild(2));
                    return this.visit(node.getChild(0)) + "(" + upper + " downto " + lower + ")";
                }
                break;
            case BINARYOP:
                return this.visit(node.getChild(0)) + " " + node.attributes.get("type") + " " + this.visit(node.getChild(1));
            case UNARYOP:
                if (node.attributes.get("type").equals("()"))
                    return "(" + this.vist(node.getChild(0)) + ")";
                else if (node.attributes.get("type").equals("not"))
                    return "not " + this.visit(node.getChild(0));
                else
                    return node.attributes.get("type") + this.visit(node.getChild(0));
            case IDENTIFIER:
                return node.attributes.get("name");
            case LITERAL:
                return node.attributes.get("value");
        }
    }

    private String indent(String str, int spaces)
    {
        String[] split = str.split("\n");
        String result = "";
        for (String element : split)
            result = result + String.format("%1$" + spaces + "s", element) + "\n";
        return result;
    }

    private String getType(Tree node)
    {
        String type;
        switch (node.attributes.get("type"))
        {
            case "int":
                type = "integer";
                break;
            case "bool":
                type = "boolean";
                break;
            case "vec":
                type = "unsigned(" + this.visit(node.getChild(0)) + " downto 0)";
                break;
            default:
                type = "";
                break;
        }
        return type;
    }

    public static void main(String[] args)
    {

    }
}