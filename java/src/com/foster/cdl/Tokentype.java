/*
Tokentype.java - Reed Foster
Enum for Token types
*/

package com.foster.cdl;

public enum Tokentype
{
    ID, RESERVED, DECINTLITERAL, BININTLITERAL, HEXINTLITERAL, BINVECLITERAL, HEXVECLITERAL, BOOLLITERAL, // identifier and literals
    COLEQ, EQ, DOUBLEEQ, LT, GT, LTEQ, GTEQ, NE, // assignment and comparison operators
    ADD, SUB, MUL, DIV, MOD, EXP, AND, OR, XOR, NOT, COLON, QUESTION, // operators
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, // grouping
    EOF, EOL, COMMA, PERIOD // misc.
}
