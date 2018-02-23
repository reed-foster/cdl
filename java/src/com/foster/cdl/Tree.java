package com.foster.cdl;

import java.util.List;

public class Tree
{	
	public final Nodetype nodetype;
	public final List<String> attributes;
	private List<Tree> children;
	
	Tree(Nodetype nodetype, List<String> attributes, List<Tree> children)
	{
		this.nodetype = nodetype;
		this.attributes = attributes;
		this.children = children;
	}
	
	public Tree getChild(int index)
	{
		return children.get(index);
	}
}
