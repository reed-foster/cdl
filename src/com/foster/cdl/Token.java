/*
Token.java - Reed Foster
Class for Token objects
*/

package com.foster.cdl;

public class Token
{   
    public final Tokentype type;
    public final String value;
    
    Token(Tokentype type, String value)
    {
        this.type = type;
        this.value = value;
    }
    
    public String toString()
    {
        return String.format("(%s, \"%s\")", this.type, this.value);
    }
}
