package com.foster.cdl;


public class TreeGenerator
{

	private Parser parser;
	private Lexer lexer;
	
	TreeGenerator(String source)
	{
		this.lexer = new Lexer(source);
		this.parser = new Parser(new Lexer(source));
	}
	
	public Tree generate()
	{
		return null;
	}
	

	
}
