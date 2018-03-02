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
        this(nodetype, new Map<String, String>(), children);
    }

    Tree(Nodetype nodetype, Map<String, String> attributes)
    {
        this(nodetype, attributes, new List<Tree>());
    }
    
    public Tree getChild(int index)
    {
        return children.get(index);
    }
}
