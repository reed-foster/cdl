package com.foster.cdl;

public class LexicalAnalyzer
{
    public static void error(String message, int col, int line) throws SyntaxError
    {
        throw new SyntaxError(String.format("%s at col %d on line %d.", message, col, line));
    }

    public static void error(String message, String token, int line) throws SyntaxError
    {
        throw new SyntaxError(String.format("%s (%s) on line %d.", message, token, line));
    }

    public static void error(String message, int line) throws SyntaxError
    {
        throw new SyntaxError(String.format("%s on line %d.", message, line));
    }
}
