/*
Tree.java - Reed Foster
Class for AST nodes
*/

package com.foster.cdl;

import java.util.*;

public class Tree
{   
    public final Nodetype nodetype;
    public final Map<String, String> attributes;
    private List<Tree> children;
    
    Tree(Nodetype nodetype, Map<String, String> attributes, List<Tree> children)
    {
        this.nodetype = nodetype;
        this.attributes = attributes;
        this.children = children;
    }

    Tree(Nodetype nodetype, List<Tree> children)
    {
        this(nodetype, new HashMap<String, String>(), children);
    }

    Tree(Nodetype nodetype, Map<String, String> attributes)
    {
        this(nodetype, attributes, new ArrayList<Tree>());
    }
    
    public Tree getChild(int index)
    {
        return children.get(index);
    }

    public List<Tree> getChildren()
    {
        return new ArrayList<Tree>(this.children);
    }

    public String visit(int depth)
    {
        String s = String.format("%s : %s.\n", this.nodetype.toString(), this.attributes.toString());
        for (Tree child : this.children)
        {
            for (int i = 0; i <= depth; i ++)
            {
                s += "  ";
            }
            s = s + String.format("%s", child.visit(depth + 1));
        }
        return s;
    }
}
