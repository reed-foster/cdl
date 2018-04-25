/*
Lexer.java - Reed Foster
Tokenizes source code
*/

package com.foster.cdl;

import java.util.*;

public class Lexer
{
    // Initialize Reserved Keyword Sets
    public static final Set<String> PORTDIR = arrayToSet(new String[] {"input", "output"});
    public static final Set<String> TYPE = arrayToSet(new String[] {"int", "vec", "bool"});
    public static final Set<String> BITWISEOP = arrayToSet(new String[] {"and", "or", "not", "nand", "nor", "xor", "xnor"});
    private static final Set<String> other = arrayToSet(new String[] {"component", "port", "arch", "signal", "const", "variable", "new"});
    public static final Set<String> RESERVEDIDS = combinesets(other, TYPE, BITWISEOP, PORTDIR);

    private static Set<String> arrayToSet(String[] array)
    {
        return new HashSet<String>(Arrays.asList(array));
    }

    @SafeVarargs // we don't care about the type of the varargs list, just the type of the elements
    private static Set<String> combinesets(Set<String> ...sets)
    {
        Set<String> superset = new HashSet<String>();
        for (Set<String> set : sets)
        {
            superset.addAll(set);
        }
        return superset;
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

    /**
    * Error thrower method
    * 
    */
    public static void error(String message, int col, int line) throws SyntaxError
    {
        throw new SyntaxError(String.format("%s at col %d on line %d.", message, col + 1, line + 1));
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
    * @return Token for (bin|hex)vec literal
    */
    private Token getVec(Tokentype type)
    {
        if (type == Tokentype.HEXVECLITERAL)
            this.advance(2);
        else if (type == Tokentype.BINVECLITERAL)
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
    * @return Token for (bin|hex|dec)int literal
    */
    private Token getInt(Tokentype type)
    {
        if (type == Tokentype.HEXINTLITERAL || type == Tokentype.BININTLITERAL)
            this.advance(2);
        else if (type != Tokentype.DECINTLITERAL)
            error("Internal Error: Invalid Integer Type", this.col, this.line);

        List<Character> chars = new ArrayList<Character>();
        char upperbound = type == Tokentype.BININTLITERAL ? '1' : '9';
        while ((this.currentchar >= '0' && this.currentchar <= upperbound)
                || (type == Tokentype.HEXINTLITERAL && (isNum(this.currentchar)
                    || (this.currentchar >= 'A' && this.currentchar <= 'F')
                    || (this.currentchar >= 'a' && this.currentchar <= 'f'))))
        {
            chars.add(new Character(this.currentchar));
            this.advance();
        }
        StringBuilder builder = new StringBuilder(chars.size());
        for (Character c : chars)
            builder.append(c);
        String value = builder.toString();
        return new Token(type, value);
    }
    
    /**
    * Parses alphanumeric strings
    * @return Token for alphanumeric string
    */
    private Token getId()
    {
        List<Character> chars = new ArrayList<Character>();
        while (isAlpha(this.currentchar) || isNum(this.currentchar)) // first character is always alpha; this method is only called if currentchar is alpha
        {
            chars.add(new Character(this.currentchar));
            this.advance();
        }
        StringBuilder builder = new StringBuilder(chars.size());
        for (Character c : chars)
            builder.append(c);
        String value = builder.toString();
        if (value.equals("true") || value.equals("false"))
            return new Token(Tokentype.BOOLLITERAL, value);
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
                return this.getVec(Tokentype.HEXVECLITERAL);
            if (this.currentchar == '"')
                return this.getVec(Tokentype.BINVECLITERAL);

            // tokenize identifiers
            if (isAlpha(this.currentchar))
                return this.getId();

            if (this.currentchar >= '0' && this.currentchar <= '9')
            {
                if (this.peek() == 'x')
                {
                    return this.getInt(Tokentype.HEXINTLITERAL);
                }
                else if (this.peek() == 'b')
                {
                    return this.getInt(Tokentype.BININTLITERAL);
                }
                return this.getInt(Tokentype.DECINTLITERAL);
            }
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
