package com.foster.cdl;

public class LexicalAnalyzer
{
	public static void error(String message, int col, int line) throws SyntaxError
	{
		throw new SyntaxError(String.format("%s at col %d on line %d", message, col, line));
	}
}
