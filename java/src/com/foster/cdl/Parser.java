package com.foster.cdl;

public class Parser extends LexicalAnalyzer
{
	private Token currenttok;
	private Lexer lexer;
	
	Parser(Lexer lexer)
	{
		this.lexer = lexer;
		this.currenttok = this.lexer.getNextToken();
	}
	
	private void eat(Tokentype type)
	{
		if (this.currenttok.type == type)
			this.currenttok = lexer.getNextToken();
		else
			error("Unexpected Token")
			
	}
}
