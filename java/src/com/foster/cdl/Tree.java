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
    
    /**
    * Default Tree constructor
    */
    Tree(Nodetype nodetype, Map<String, String> attributes, List<Tree> children)
    {
        this.nodetype = nodetype;
        this.attributes = attributes;
        this.children = children;
    }

    /**
    * Tree constructor for nodes without attributes (like port declarations)
    */
    Tree(Nodetype nodetype, List<Tree> children)
    {
        this(nodetype, new HashMap<String, String>(), children);
    }

    /**
    * Tree constructor for nodes with no children (like identifiers)
    */
    Tree(Nodetype nodetype, Map<String, String> attributes)
    {
        this(nodetype, attributes, new ArrayList<Tree>());
    }

    /**
    * Modifier method for adding children to the tree
    * @param child subtree to be added
    */
    public void addChild(Tree child)
    {
        this.children.add(child);
    }

    /**
    * Modifier method for removing children from the tree
    * @param index int which child to remove (children are stored in a list so insertion order is preserved)
    */
    public void removeChild(int index)
    {
        this.children.remove(index);
    }

    /**
    * Get the size of the child list
    */
    public int numChildren()
    {
        return this.children.size();
    }
    
    /**
    * Returns the child at the specified index (children are stored in a list so insertion order is preserved)
    */
    public Tree getChild(int index)
    {
        return children.get(index);
    }

    /**
    * Returns a new ArrayList containing all the children
    */
    public List<Tree> getChildren()
    {
        return new ArrayList<Tree>(this.children);
    }

    /**
    * Returns a string representation of the tree, with each generation of children indented 2 spaces
    */
    public String toString()
    {
        return this.visit(0);
    }

    /**
    * Helper method for toString()
    */
    private String visit(int depth)
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
