package com.foster.cdl;

public class Lexer extends LexicalAnalyzer
{
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
        return;
    }

    private void advance()
    {
        this.advance(1);
        return;
    }

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
    
    private static boolean isSpace(char c)
    {
    	return (c == ' ' || c == '\t' || c == '\n');
    }

    private Token getVec(Tokentype type)
    {
        if (type == Tokentype.HEXVECCONST)
            this.advance(2);
        else if (type == Tokentype.BINVECCONST)
            this.advance();
        else
            error("Invalid Vector Type", this.col, this.line);

        int stringend = this.source.indexOf("\"", this.pos);
        String value = this.source.substring(this.pos, stringend);
        this.advance(stringend - this.pos + 1); //skip over vector
        return new Token(type, value);

    }

    private Token getInt(Tokentype type)
    {
        if (type == Tokentype.HEXINTCONST || type == Tokentype.BININTCONST)
            this.advance(2);
        else if (type != Tokentype.DECINTCONST)
            error("Invalid Integer Type", this.col, this.line);

        int stringend = this.pos;
        char upperbound = type == Tokentype.BININTCONST ? '1' : '9';
        while ((this.source.charAt(stringend) >= '0' && this.source.charAt(stringend) <= upperbound)
             || (type == Tokentype.HEXINTCONST && isAlpha(this.source.charAt(stringend))))
            stringend++;
        String value = this.source.substring(this.pos, stringend);
        this.advance(stringend - this.pos + 1);
        return new Token(type, value);
    }
    
    private Token getId()
    {
    	int stringend = this.pos;
    	while (isAlpha(this.source.charAt(stringend)))
    		stringend++;
    	String value = this.source.substring(this.pos, stringend);
    	this.advance(stringend - this.pos + 1);
    	if (value.compareTo("true") == 0 || value.compareTo("false") == 0)
    		return new Token(Tokentype.BOOLCONST, value);
    	return new Token(Tokentype.ID, value);
    }

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

    public static void main(String args[])
    {
    	// tests every valid token
        Lexer lexer = new Lexer("hi 10 0b101 0xabcd \"101101\" x\"abc103\" //comment lol \n true false /*multiline comments are cool*/ := = == < > <= >= != + - * / % ** & | ^ ! & : ? ( ) { } [ ] ; , .");
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