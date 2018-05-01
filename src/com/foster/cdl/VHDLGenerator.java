/*
VHDLGenerator.java - Reed Foster
Stores all parsed components
*/

package com.foster.cdl;

import java.util.*;

public class VHDLGenerator
{
    private Map<String, Component> components;
    private Map<String, String> componentInterfaces;
    private Map<String, Set<DeclaredIdentifier>> tempSignals; // maps each component name to a set of declaredIdentifiers
    private Map<String, Set<String>> usedPorts; // maps each component instance to a set of all ports that are used

    private String currentComponent;

    VHDLGenerator(String source)
    {
        SemanticAnalyzer s = new SemanticAnalyzer(source);
        this.components = s.getComponents();
        this.componentInterfaces = new HashMap<String, String>();
        this.tempSignals = new HashMap<String, Set<DeclaredIdentifier>>();
        this.usedPorts = new HashMap<String, Set<String>>();
        this.getAllTempSignals();
    }

    public String getVHDL()
    {
        for (String compname : this.components.keySet()) // first, generate all the entity declarations
        {
            Tree ast = this.components.get(compname).ast;
            this.componentInterfaces.put(compname, this.getEntity(ast));
        }
        String output = "";
        for (String compname : this.components.keySet())
        {
            this.currentComponent = compname;
            Tree ast = this.components.get(compname).ast;
            output += this.componentInterfaces.get(compname) + "\n\n" + this.getArch(ast);
        }
        return output;
    }

    private void getAllTempSignals()
    {
        for (String componentName : this.components.keySet())
        {
            Component component = this.components.get(componentName);
            Set<DeclaredIdentifier> tempSignals = this.getTempSignals(componentName, component.ast);
            this.tempSignals.put(componentName, tempSignals);
            Set<String> usedPorts = new HashSet<String>();
            for (DeclaredIdentifier tempSignal : tempSignals)
                usedPorts.add(tempSignal.name);
            this.usedPorts.put(componentName, usedPorts);
        }
    }

    /**
    * Creates a Set of DeclaredIdentifiers for all subcomponent ports that are used as signals
    *
    */
    private Set<DeclaredIdentifier> getTempSignals(String componentName, Tree node)
    {
        Set<DeclaredIdentifier> sigDecs = new HashSet<DeclaredIdentifier>();
        if (node.nodetype == Nodetype.BINARYOP && node.attributes.get("type").equals("."))
        {
            List<Tree> children = new ArrayList<Tree>();
            Map<String, String> attributes = new HashMap<String, String>();

            String compinstID = node.getChild(0).attributes.get("name");
            String portID = node.getChild(1).attributes.get("name");
            attributes.put("compname", compinstID);
            attributes.put("name", portID);

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
            for (Tree child : node.getChildren())
            {
                sigDecs.addAll(this.getTempSignals(componentName, child));
            }
        }
        return sigDecs;
    }

    private String getEntity(Tree node)
    {
        if (node.nodetype != Nodetype.COMPONENT)
            return null;
        String generics = "";
        String ports = "";
        for (Tree child : node.getChildren())
        {
            switch (child.nodetype)
            {
                case GENDEC:
                    generics += node.attributes.get("name") + " : " + this.getType(node) + ";\n";
                    break;
                case PORTDEC:
                    String body = "";
                    for (Tree grandchild : child.getChildren())
                    {
                        String direction = grandchild.attributes.get("direction");
                        direction = direction.substring(0, direction.length() - 3); // input => in, output => out
                        body += grandchild.attributes.get("name") + " : " + direction + " " + this.getType(grandchild) + ";\n";
                    }
                    if (body.length() != 0)
                        ports = indent("port\n(\n" + indent(body.substring(0, body.length() - 2)) + "\n);") + "\n"; // substring to remove last trailing semicolon
                    break;
                }
        }
        String entity = "entity " + node.attributes.get("name") + " is\n";
        entity += generics.length() > 1 ? indent("generic\n(\n" + generics + "\n);") + "\n" : "";
        entity += ports;
        entity += "end " + node.attributes.get("name") + ";";
        return entity;
    }

    private String getArch(Tree node)
    {
        String arch = "";
        if (node.nodetype == Nodetype.ARCH)
            arch += this.visit(node);
        else if (node.nodetype == Nodetype.COMPONENT)
        {
            for (Tree child : node.getChildren())
                arch += this.getArch(child);
        }
        return arch;
    }

    // should only be called on Arch (need to refactor names)
    private String visit(Tree node)
    {
        switch (node.nodetype)
        {
            case ARCH:
                String declarations = "";
                String assignments = "";
                for (Tree child : node.getChildren())
                {
                    switch (child.nodetype)
                    {
                        case COMPDEC:
                            // add component declaration to declarations
                            String interfaceName = child.attributes.get("type");
                            String componentInterface = this.componentInterfaces.get(interfaceName);
                            int firstnewline = componentInterface.indexOf('\n');
                            int lastnewline = componentInterface.lastIndexOf('\n');
                            declarations += "component " + interfaceName + "\n" + componentInterface.substring(firstnewline + 1, lastnewline) + "\nend component;\n";
                            
                            // add component instantiation to assignments
                            String instanceName = child.attributes.get("name");
                            Set<DeclaredIdentifier> ports = this.components.get(interfaceName).getPorts();
                            Set<DeclaredIdentifier> generics = this.components.get(interfaceName).getGenerics();
                            String portMap = "";
                            for (DeclaredIdentifier port : ports)
                            {
                                portMap += port.name + " => ";
                                portMap += this.usedPorts.get(instanceName).contains(port.name) ? instanceName + "_" + port.name : "open";
                                portMap += ",\n";
                            }
                            String genericMap = "";
                            for (Tree genericAssign : child.getChildren())
                            {
                                genericMap += this.visit(genericAssign.getChild(0)) + " => " + this.visit(genericAssign.getChild(1)) + ",\n";
                            }
                            portMap = portMap.length() > 0 ? "port map\n(\n" + portMap.substring(0, portMap.length() - 2) + "\n)\n" : "";
                            genericMap = genericMap.length() > 0 ? "generic map\n(\n" + genericMap.substring(0, genericMap.length() - 2) + "\n)\n" : "";
                            assignments += instanceName + " : " + interfaceName + "\n" + genericMap + portMap + ";\n";
                            break;
                        case SIGDEC:
                        case CONST:
                            declarations += this.visit(child) + ";\n";
                            break;
                        case BINARYOP:
                            assignments += this.visit(child) + ";\n";
                            break;
                        default:
                            // shouldn't ever get here
                            break;
                    }
                }
                for (DeclaredIdentifier tempSignal : this.tempSignals.get(this.currentComponent))
                {
                    declarations += "signal " + tempSignal.declaration.attributes.get("compname") + "_" + tempSignal.name + " : " + this.getType(tempSignal.declaration);
                }
                return "architecture structural of " + this.currentComponent + " is\n" + indent(declarations) + "\nbegin\n" + indent(assignments) + "\nend structural;";
            case SIGDEC:
                return "signal " + node.attributes.get("name") + " : " + this.getType(node);
            case CONST:
                return "constant " + node.attributes.get("name") + " : " + this.getType(node) + " := " + this.visit(node.getChild(0));
            case TERNARYOP:
                if (node.attributes.get("type").equals("?"))
                    return this.visit(node.getChild(1)) + " when " + this.visit(node.getChild(0)) + " else " + this.visit(node.getChild(2));
                else if (node.attributes.get("type").equals("[]"))
                {
                    String upper = this.visit(node.getChild(1));
                    String lower = node.numChildren() == 2 ? upper : this.visit(node.getChild(2));
                    return this.visit(node.getChild(0)) + "(" + upper + " downto " + lower + ")";
                }
                break;
            case BINARYOP:
                String sep = node.attributes.get("type");
                if (sep.equals("."))
                    sep = "_";
                return this.visit(node.getChild(0)) + " " + sep + " " + this.visit(node.getChild(1));
            case UNARYOP:
                if (node.attributes.get("type").equals("()"))
                    return "(" + this.visit(node.getChild(0)) + ")";
                else if (node.attributes.get("type").equals("not"))
                    return "not " + this.visit(node.getChild(0));
                else
                    return node.attributes.get("type") + this.visit(node.getChild(0));
            case IDENTIFIER:
                return node.attributes.get("name");
            case LITERAL:
                return node.attributes.get("value");
        }
        return "";
    }

    private static String indent(String str)
    {
        return indent(str, 4);
    }

    private static String indent(String str, int spaces)
    {
        String indentStr = new String(new char[spaces]).replace("\0", " ");
        String[] split = str.split("\n");
        String result = "";
        for (String element : split)
            result += indentStr + element + "\n";
        return result.substring(0, result.length() - 1);
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
                String width = this.visit(node.getChild(0));
                String newWidth;
                try
                {
                    newWidth = new Integer(Integer.valueOf(width) - 1).toString();
                }
                catch (NumberFormatException e)
                {
                    newWidth = "(" + width + " - 1)";
                }
                type = "unsigned(" + newWidth + (newWidth.equals("0") ? ")" : " downto 0)");
                break;
            default:
                type = "";
                break;
        }
        return type;
    }

    public static void main(String[] args)
    {
        String source = "component XorGate{"+
                            "port{"+
                                "input vec[1] a;"+
                                "input vec[1] b;"+
                                "output vec[1] sum;"+
                            "}"+
                            "arch{"+
                                "sum <= a xor b;"+
                            "}"+
                        "}"+
                        "component AndGate{"+
                            "port{"+
                                "input vec[1] a;"+
                                "input vec[1] b;"+
                                "output vec[1] prod;"+
                            "}"+
                            "arch{"+
                                "prod <= a and b;"+
                            "}"+
                        "}"+
                        "component HalfAdder{"+
                            "port{"+
                                "input vec[1] a;"+
                                "input vec[1] b;"+
                                "input vec[1] ci;"+
                                "output vec[1] sum;"+
                                "output vec[1] co;"+
                            "}"+
                            "arch{"+
                                "XorGate XOR = new XorGate();"+
                                "AndGate AND = new AndGate();"+
                                "XOR.a <= a;"+
                                "XOR.b <= b;"+
                                "AND.a <= a;"+
                                "AND.b <= b;"+
                                "sum <= XOR.sum;"+
                                "co <= AND.prod;"+
                            "}"+
                        "}";

        VHDLGenerator gen = new VHDLGenerator(source);
        System.out.println(gen.getVHDL());
    }
}