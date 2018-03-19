/*
ComponentTree.java - Reed Foster
Class for component dependency tree
*/

package com.foster.cdl;

import java.util.*;

public class ComponentTree
{
    private List<ComponentTree> children;
    private Component component;

    ComponentTree(Component component, List<ComponentTree> children)
    {
        this.component = component;
        this.children = children;
    }

    ComponentTree(Component component)
    {
        this(component, new ArrayList<ComponentTree>());
    }

    public void addChild(ComponentTree child)
    {
        this.children.add(child);
    }

    public List<ComponentTree> getChildren()
    {
        return new ArrayList<ComponentTree>(this.children);
    }

    /**
    * Wrapper for hasChildUtil; searches for descendents of the root node containing component
    * @param component Component to search for
    */
    public boolean hasChild(Component component)
    {
        for (ComponentTree child : this.children)
        {
            if (hasChildUtil(child, component))
                return true;
        }
        return false;
    }

    /**
    * Helper method for hasChild
    * @param node current ComponentTree to search
    * @param component Component to search for
    */
    private static boolean hasChildUtil(ComponentTree node, Component component)
    {
        if (node.component.name.compareTo(component.name) == 0)
            return true;
        for (ComponentTree child : node.children)
        {
            if (hasChildUtil(child, component))
                return true;
        }
        return false;
    }
}