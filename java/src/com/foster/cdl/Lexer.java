/*
Lexer.java - Reed Foster
Tokenizes source code
*/

package com.foster.cdl;

import java.util.*;

public class Lexer extends LexicalAnalyzer
{
    // Initialize Reserved Keyword Sets
    public static final Set<String> PORTDIR = arrayToSet(new String[] {"input", "output"});
    public static final Set<String> TYPE = arrayToSet(new String[] {"int", "uint", "vec", "bool"});
    public static final Set<String> BITWISEOP = arrayToSet(new String[] {"and", "or", "not", "nand", "nor", "xor", "xnor"});
    public static final Set<String> RESERVEDIDS = arrayToSet(new String[] {"component", "port", "arch", "signal", "variable", "new"}).addAll(TYPE).addAll(BITWISEOP).addAll(PORTDIR);

    private static Set<String> arrayToSet(String[] array)
    {
        return new HashSet<String>(Arrays.asList(array));
    }

    private String source;

    private int pos;
    private int line;
    private int col;
    private char currentchar;

    Lexer(String source)
    {
        this.source = source;
        this.pos = this.line = this.col = 0;
        this.currentchar = this.source.charAt(this.pos);
    }

    /**
    * Public get method for current line (used internally for error messages in Parser)
    * @return this.line;
    */
    public int getline()
    {
        return this.line;
    }

    /**
    * Retrieves the next character from source and advances the character pointer
    * @param count specifies the amount by which to increment the character pointer
    * @return character at this.pos after incrementing or 0 if past the end of source
    */
    private void advance(int count)
    {
        this.pos += count;
        this.col += count;
        if (this.pos > this.source.length() - 1)
        {
            this.currentchar = 0;
            return;
        }
        this.currentchar = this.source.charAt(this.pos);
    }

    /**
    * Overloaded advance method
    * @return this.advance(1)
    */
    private void advance()
    {
        this.advance(1);
    }

    /**
    * Retrieves the next character without advancing the character pointer
    * @return character at (this.pos + 1) or 0 if past the end of source
    */
    private char peek()
    {
        int peekpos = this.pos + 1;
        if (peekpos > this.source.length() - 1)
            return 0;
        return this.source.charAt(peekpos);
    }

    private static boolean isAlpha(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isNum(char c)
    {
        return (c >= '0' && c <= '9');
    }
    
    private static boolean isSpace(char c)
    {
        return (c == ' ' || c == '\t' || c == '\n');
    }

    /**
    * Parses vector literals
    * @return Token for (bin|hex)vec constant
    */
    private Token getVec(Tokentype type)
    {
        if (type == Tokentype.HEXVECCONST)
            this.advance(2);
        else if (type == Tokentype.BINVECCONST)
            this.advance();
        else
            error("Internal Error: Invalid Vector Type", this.col, this.line);

        int stringend = this.source.indexOf("\"", this.pos);
        String value = this.source.substring(this.pos, stringend);
        this.advance(stringend - this.pos + 1); //skip over vector
        return new Token(type, value);

    }

    /**
    * Parses integer literals
    * @return Token for (bin|hex|dec)int constant
    */
    private Token getInt(Tokentype type)
    {
        if (type == Tokentype.HEXINTCONST || type == Tokentype.BININTCONST)
            this.advance(2);
        else if (type != Tokentype.DECINTCONST)
            error("Internal Error: Invalid Integer Type", this.col, this.line);

        int stringend = this.pos;
        char upperbound = type == Tokentype.BININTCONST ? '1' : '9';
        while ((this.source.charAt(stringend) >= '0' && this.source.charAt(stringend) <= upperbound)
             || (type == Tokentype.HEXINTCONST && isAlpha(this.source.charAt(stringend))))
            stringend++;
        String value = this.source.substring(this.pos, stringend);
        this.advance(stringend - this.pos + 1);
        return new Token(type, value);
    }
    
    /**
    * Parses alphanumeric strings
    * @return Token for alphanumeric string
    */
    private Token getId()
    {
        int stringend = this.pos;
        while (isAlpha(this.source.charAt(stringend)) || isNum(this.source.charAt(stringend)))
            stringend++;
        String value = this.source.substring(this.pos, stringend);
        this.advance(stringend - this.pos + 1);
        if (value.compareTo("true") == 0 || value.compareTo("false") == 0)
            return new Token(Tokentype.BOOLCONST, value);
        if (Lexer.RESERVEDIDS.contains(value))
            return new Token(Tokentype.RESERVED, value);
        return new Token(Tokentype.ID, value);
    }

    /**
    * Rerieves the next token from source
    * @return next Token
    */
    public Token getNextToken()
    {
        while (this.currentchar != 0)
        {
            // skip whitespace
            while (isSpace(this.currentchar))
            {
                if (this.currentchar == '\n')
                {
                    this.line++;
                    this.col = 0;
                }
                this.advance();
            }

            // skip comments
            if (this.currentchar == '/' && this.peek() == '*')
            {
                this.advance(2);
                while (!(this.currentchar == '*' && this.peek() == '/'))
                    this.advance();
            }
            if (this.currentchar == '/' && this.peek() == '/')
            {
                while (this.currentchar != '\n')
                    this.advance();
                continue;
            }

            // tokenize fixed-length strings
            switch (this.currentchar)
            {
                case ':':
                    if (this.peek() == '=')
                    {
                        this.advance(2);
                        return new Token(Tokentype.COLEQ, ":=");
                    }
                    else
                    {
                        this.advance();
                        return new Token(Tokentype.COLON, ":");
                    }

                case '=':
                    if (this.peek() == '=')
                    {
                        this.advance(2);
                        return new Token(Tokentype.DOUBLEEQ, "==");
                    }
                    else
                    {
                        this.advance();
                        return new Token(Tokentype.EQ, "=");
                    }

                case '<':
                    if (this.peek() == '=')
                    {
                        this.advance(2);
                        return new Token(Tokentype.LTEQ, "<=");
                    }
                    else
                    {
                        this.advance();
                        return new Token(Tokentype.LT, "<");
                    }

                case '>':
                    if (this.peek() == '=')
                    {
                        this.advance(2);
                        return new Token(Tokentype.GTEQ, ">=");
                    }
                    else
                    {
                        this.advance();
                        return new Token(Tokentype.GT, ">");
                    }

                case '!':
                    if (this.peek() == '=')
                    {
                        this.advance(2);
                        return new Token(Tokentype.NE, "!=");
                    }
                    else
                    {
                        this.advance();
                        return new Token(Tokentype.NOT, "!");
                    }

                case '+':
                    this.advance();
                    return new Token(Tokentype.ADD, "+");

                case '-':
                    this.advance();
                    return new Token(Tokentype.SUB, "-");

                case '*':
                    if (this.peek() == '*')
                    {
                        this.advance(2);
                        return new Token(Tokentype.EXP, "**");
                    }
                    else
                    {
                        this.advance();
                        return new Token(Tokentype.MUL, "*");
                    }

                case '/':
                    this.advance();
                    return new Token(Tokentype.DIV, "/");

                case '%':
                    this.advance();
                    return new Token(Tokentype.MOD, "%");

                case '^':
                    this.advance();
                    return new Token(Tokentype.XOR, "^");

                case '&':
                    this.advance();
                    return new Token(Tokentype.AND, "&");
                    
                case '|':
                    this.advance();
                    return new Token(Tokentype.OR, "|");

                case '?':
                    this.advance();
                    return new Token(Tokentype.QUESTION, "?");

                case '(':
                    this.advance();
                    return new Token(Tokentype.LPAREN, "(");

                case '{':
                    this.advance();
                    return new Token(Tokentype.LBRACE, "{");

                case '[':
                    this.advance();
                    return new Token(Tokentype.LBRACKET, "[");

                case ')':
                    this.advance();
                    return new Token(Tokentype.RPAREN, ")");

                case '}':
                    this.advance();
                    return new Token(Tokentype.RBRACE, "}");

                case ']':
                    this.advance();
                    return new Token(Tokentype.RBRACKET, "]");

                case ';':
                    this.advance();
                    return new Token(Tokentype.EOL, ";");

                case ',':
                    this.advance();
                    return new Token(Tokentype.COMMA, ",");

                case '.':
                    this.advance();
                    return new Token(Tokentype.PERIOD, ".");
            }

            // tokenize vector literals
            if (this.currentchar == 'x' && this.peek() == '"')
                return this.getVec(Tokentype.HEXVECCONST);
            if (this.currentchar == '"')
                return this.getVec(Tokentype.BINVECCONST);

            // tokenize identifiers
            if (isAlpha(this.currentchar))
                return this.getId();

            if (this.currentchar >= '0' && this.currentchar <= '9')
            {
                if (this.peek() == 'x')
                {
                    return this.getInt(Tokentype.HEXINTCONST);
                }
                else if (this.peek() == 'b')
                {
                    return this.getInt(Tokentype.BININTCONST);
                }
                return this.getInt(Tokentype.DECINTCONST);
            }
            
            System.out.println(this.source.charAt(this.pos));
            error("Invalid Character", this.col, this.line);
        }
        return new Token(Tokentype.EOF, "");
    }

    /**
    * Unit test
    */
    public static void main(String args[])
    {
        // tests every valid token
        String source = "hi 10 0b101 0xabcd \"101101\" x\"abc103\" //comment lol \n" + 
                        "true false /*multiline comments are cool*/ := = == < > <= " +
                        ">= != + - * / % ** & | ^ ! & : ? ( ) { } [ ] ; , .";
        Lexer lexer = new Lexer(source);
        while (true)
        {
            Token next = lexer.getNextToken();
            System.out.println(next);
            if (next.type == Tokentype.EOF)
            {
                return;
            }
        }
    }
}
