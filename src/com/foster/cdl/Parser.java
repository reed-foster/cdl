/*
Parser.java - Reed Foster
Generates AST from token stream based on CDL language grammar
*/

package com.foster.cdl;

import java.util.*;

public class Parser
{
    private Token currenttok;
    private Lexer lexer;
    
    Parser(Lexer lexer)
    {
        this.lexer = lexer;
        this.currenttok = this.lexer.getNextToken();
    }

    /**
    * Matches a token type to a list of token types
    * @param type type of token to check against list of types
    * @param types varargs list of types to check against
    * @return true if type matches at least one element of types
    */
    private static boolean match(Tokentype type, Tokentype ...types)
    {
        for (Tokentype t : types)
        {
            if (type == t)
                return true;
        }
        return false;
    }

    /**
    * Matches a string to a list of strings
    * @param value of string to check against list of values
    * @param values varargs list of values to check against
    * @return true if value matches at least one element of values
    */
    private static boolean match(String value, String ...values)
    {
        for (String v : values)
        {
            if (value.equals(v))
                return true;
        }
        return false;
    }
    
    /**
    * "Eats" the current token; error checking method to verify that the current token is of the expected type
    * @param type the type to check against the current token type
    */
    private void eat(Tokentype type)
    {
        if (this.currenttok.type == type)
            this.currenttok = lexer.getNextToken();
        else
            error("Unexpected Token", String.format("Expected (%s), got (%s)", type.toString(), this.currenttok.value.toString()), this.lexer.getline());
    }

    /**
    * Advanced eat method; verifies that the current token's value matches at least one of the elements of values
    * @param type same as normal eat method
    * @param values list of values to check against currenttok.value
    */
    private void eat(Tokentype type, String ...values)
    {
        if (match(this.currenttok.value, values))
            this.eat(type);
        else
            error("Unexpected Token", String.format("Expected (%s), got (%s)", values.toString(), this.currenttok.value), this.lexer.getline());
    }

    /**
    * eatType verifies that the current token is reserved and a valid type string
    * calls method eat with the parameters Tokentype.RESERVED and "int", "vec", and "bool"
    */
    private void eatType()
    {
        this.eat(Tokentype.RESERVED, "int", "vec", "bool");
    }

    /**
    * Error thrower methods
    * @throws SyntaxError
    */
    private static void error(String type, String message, int line) throws SyntaxError
    {
        throw new SyntaxError(String.format("%s on line %d. %s", type, line + 1, message));
    }

    /**
    * Initializes a hashmap with one key and one value
    * @param k key String
    * @param v value String
    * @return HashMap containing (k -> v)
    */
    private Map<String, String> quickHashMap(String k, String v)
    {
        Map<String, String> hm = new HashMap<String, String>();
        hm.put(k, v);
        return hm;
    }

    public Tree parse()
    {
        return this.component();
    }

    /**
    * Parses component
    * @return Tree with root node of type Nodetype.COMPONENT and children of type Nodetype.GENDEC, Nodetype.PORTDEC, and Nodetype.ARCH
    */
    private Tree component()
    {
        List<Tree> children = new ArrayList<Tree>();
        Map<String, String> attributes = new HashMap<String, String>();

        this.eat(Tokentype.RESERVED, "component");
        String name = this.currenttok.value;
        this.eat(Tokentype.ID);
        this.eat(Tokentype.LBRACE);
        while (this.currenttok.type != Tokentype.RBRACE)
        {
            if (Lexer.TYPE.contains(this.currenttok.value))
                children.add(this.gendec());
            else if (this.currenttok.value.equals("port"))
                children.add(this.portdec());
            else if (this.currenttok.value.equals("arch"))
                children.add(this.arch());
            else
                error("Unexpected Token", String.format("Expected RESERVED, PORT, or ARCH, got (%s)", this.currenttok.value.toString()), this.lexer.getline());
        }
        this.eat(Tokentype.RBRACE);
        attributes.put("name", name);
        return new Tree(Nodetype.COMPONENT, attributes, children);
    }

    /**
    * Parses generic declaration ({type}, {name})
    * @return Tree with node of type Nodetype.GENDEC
    */
    private Tree gendec()
    {
        List<Tree> children = new ArrayList<Tree>();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("type", this.currenttok.value);
        this.eatType();
        if (attributes.get("type").equals("vec"))
        {
            this.eat(Tokentype.LBRACKET);
            children.add(this.expression());
            this.eat(Tokentype.RBRACKET);
        }
        attributes.put("name", this.currenttok.value);
        this.eat(Tokentype.ID);
        this.eat(Tokentype.EOL);
        return new Tree(Nodetype.GENDEC, attributes, children);
    }

    /**
    * Parses port declaration ("port", LBRACE, {portbody}, RBRACE)
    * @return Tree with root node of type Nodetype.PORTDEC and children of type Nodetype.PORT
    */
    private Tree portdec()
    {
        List<Tree> children = new ArrayList<Tree>();

        this.eat(Tokentype.RESERVED, "port");
        this.eat(Tokentype.LBRACE);
        while (this.currenttok.type != Tokentype.RBRACE)
        {
            children.add(this.port());
            this.eat(Tokentype.EOL);
        }
        this.eat(Tokentype.RBRACE);
        return new Tree(Nodetype.PORTDEC, children);
    }

    /**
    * Parses individual port declarations ({direction}, {type}, {name})
    * @return Tree with root node of type Nodetype.PORT
    */
    private Tree port()
    {
        List<Tree> children = new ArrayList<Tree>();
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put("direction", this.currenttok.value);
        this.eat(Tokentype.RESERVED, "input", "output");
        attributes.put("type", this.currenttok.value);
        this.eatType();
        if (attributes.get("type").equals("vec"))
        {
            this.eat(Tokentype.LBRACKET);
            children.add(this.expression());
            this.eat(Tokentype.RBRACKET);
        }
        attributes.put("name", this.currenttok.value);
        this.eat(Tokentype.ID);
        return new Tree(Nodetype.PORT, attributes, children);
    }

    /**
    * Parses architecture ("arch", LBRACE, {archbody}, RBRACE)
    * @return Tree with root node of type Nodetype.ARCH and children of type Nodetype.SIGDEC, Nodetype.COMPDEC, Nodetype.CONST, and Nodetype.BINARYOP
    */
    private Tree arch()
    {
        List<Tree> children = new ArrayList<Tree>();

        this.eat(Tokentype.RESERVED, "arch");
        this.eat(Tokentype.LBRACE);
        while (this.currenttok.type != Tokentype.RBRACE)
        {
            if (this.currenttok.value.equals("signal"))
                children.add(this.sigdec());
            else if (this.currenttok.value.equals("const"))
                children.add(this.constant());
            else if (this.currenttok.type == Tokentype.ID)
            {
                Tree id = this.identifier();
                if (this.currenttok.type == Tokentype.LTEQ)
                {
                    this.eat(Tokentype.LTEQ);
                    List<Tree> assignmentchildren = new ArrayList<Tree>();
                    assignmentchildren.add(id);
                    assignmentchildren.add(this.expression());
                    children.add(new Tree(Nodetype.BINARYOP, quickHashMap("type", "<="), assignmentchildren));
                    this.eat(Tokentype.EOL);
                }
                else if (this.currenttok.type == Tokentype.ID)
                {
                    Map<String, String> compattr = new HashMap<String, String>();
                    List<Tree> compchildren = new ArrayList<Tree>();

                    compattr.put("name", this.currenttok.value);
                    compattr.put("type", id.attributes.get("name"));
                    this.eat(Tokentype.ID);
                    this.eat(Tokentype.EQ);
                    this.eat(Tokentype.RESERVED, "new");
                    this.eat(Tokentype.ID, id.attributes.get("name")); // verify assigned component instance is the same type as declared
                    this.eat(Tokentype.LPAREN);
                    if (this.currenttok.type != Tokentype.RPAREN)
                        compchildren.addAll(this.genericlist());
                    this.eat(Tokentype.RPAREN);
                    this.eat(Tokentype.EOL);
                    children.add(new Tree(Nodetype.COMPDEC, compattr, compchildren));
                }
            }
            else
                error("Unexpected Token", String.format("Expected ID or SIGNAL, got (%s)", this.currenttok.value.toString()), this.lexer.getline());
        }
        return new Tree(Nodetype.ARCH, children);
    }

    /**
    * Parses signal declaration ("signal", {type}, {name})
    * @return Tree with root node of type Nodetype.SIGDEC
    */
    private Tree sigdec()
    {
        List<Tree> children = new ArrayList<Tree>();
        Map<String, String> attributes = new HashMap<String, String>();
        this.eat(Tokentype.RESERVED, "signal");
        attributes.put("type", this.currenttok.value);
        this.eatType();
        if (attributes.get("type").equals("vec"))
        {
            this.eat(Tokentype.LBRACKET);
            children.add(this.expression());
            this.eat(Tokentype.RBRACKET);
        }
        attributes.put("name", this.currenttok.value);
        this.eat(Tokentype.ID);
        this.eat(Tokentype.EOL);
        return new Tree(Nodetype.SIGDEC, attributes, children);
    }

    /**
    * Parses constant declarations ("const", {type}, {name}, "=", {expression})
    * @return Tree with root node of type Nodetype.CONST
    */
    private Tree constant()
    {
        List<Tree> children = new ArrayList<Tree>();
        Map<String, String> attributes = new HashMap<String, String>();
        this.eat(Tokentype.RESERVED, "const");
        attributes.put("type", this.currenttok.value);
        this.eatType();
        if (attributes.get("type").equals("vec"))
        {
            this.eat(Tokentype.LBRACKET);
            children.add(this.expression());
            this.eat(Tokentype.RBRACKET);
        }
        attributes.put("name", this.currenttok.value);
        this.eat(Tokentype.ID);
        this.eat(Tokentype.EQ);
        children.add(this.expression());
        this.eat(Tokentype.EOL);
        return new Tree(Nodetype.CONST, attributes, children);
    }

    /**
    * Parses generic initialization assignments for component declaration and generic mapping
    * @return Tree with root node of type Nodetype.BINARYOP and children of type Nodetype.IDENTIFIER and all possible types expression() returns
    */
    private List<Tree> genericlist() // list of generic initializations for component declaration/generic mapping
    {
        List<Tree> children = new ArrayList<Tree>();
        do
        {
            if (this.currenttok.type == Tokentype.COMMA)
                this.eat(Tokentype.COMMA);
            List<Tree> assignment = new ArrayList<Tree>();
            assignment.add(this.identifier(false));
            this.eat(Tokentype.EQ);
            assignment.add(this.expression());
            children.add(new Tree(Nodetype.BINARYOP, quickHashMap("type", "="), assignment));
        } while (this.currenttok.type == Tokentype.COMMA);
        return children;
    }

    /**
    * Parses identifiers, conditionally parsing "compound" identifiers ({id}, PERIOD, {id})
    * @param allowcompound (boolean) true if parsing of a compound identifier is desired, false otherwise
    * @return Tree with root node of type Nodetype.IDENTIFIER or Nodetype.BINARYOP (which would have children of type Nodetype.IDENTIFIER)
    */
    private Tree identifier(boolean allowcompound)
    {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("name", this.currenttok.value);
        this.eat(Tokentype.ID);
        Tree left = new Tree(Nodetype.IDENTIFIER, attributes);
        if (allowcompound && this.currenttok.type == Tokentype.PERIOD)
        {
            List<Tree> children = new ArrayList<Tree>();
            children.add(left);
            this.eat(Tokentype.PERIOD);
            children.add(this.identifier(false));
            return new Tree(Nodetype.BINARYOP, quickHashMap("type", "."), children);
        }
        return left;
    }

    /**
    * Default identifier parsing method; parses allowing compound identifiers
    * @return this.identifier(true);
    */
    private Tree identifier()
    {
        return this.identifier(true);
    }

    /**
    * Parses literals
    * @return Tree with root node of type Nodetype.LITERAL
    */
    private Tree literal()
    {
        Map<String, String> attributes = new HashMap<String, String>();
        Tokentype t = this.currenttok.type;
        attributes.put("value", this.currenttok.value);
        attributes.put("type", t.toString());
        if (match(t, Tokentype.DECINTLITERAL, Tokentype.BININTLITERAL, Tokentype.HEXINTLITERAL, Tokentype.BINVECLITERAL, Tokentype.HEXVECLITERAL))
            this.eat(t);
        else
            error("Unexpected Token", String.format("Expected LITERAL, got (%s)", this.currenttok.value.toString()), this.lexer.getline());
        return new Tree(Nodetype.LITERAL, attributes);
    }

    /**
    * Parses expressions: boolexpr (a bit of a misnomer) with an optional ternary operation
    * @return Tree represeting parsed expression
    */
    private Tree expression()
    {
        Tree node = this.boolexpr();
        if (this.currenttok.type == Tokentype.QUESTION)
        {
            List<Tree> children = new ArrayList<Tree>();
            this.eat(Tokentype.QUESTION);
            Tree left = this.expression();
            this.eat(Tokentype.COLON);
            children.add(node);
            children.add(left);
            children.add(this.expression());
            node = new Tree(Nodetype.TERNARYOP, quickHashMap("type", "?"), children);
        }
        return node;
    }

    /**
    * Parses boolexpr: boolfactor followed by an unlimited number of AND, OR, or XOR operators
    * @return Tree representing parsed boolexpr
    */
    private Tree boolexpr()
    {
        Tree node = this.boolfactor();
        if (match(this.currenttok.type, Tokentype.AND, Tokentype.OR, Tokentype.XOR))
        {
            Map<String, String> attributes = quickHashMap("type", this.currenttok.value);
            List<Tree> children = new ArrayList<Tree>();
            this.eat(this.currenttok.type);
            children.add(node);
            children.add(this.boolexpr());
            node = new Tree(Nodetype.BINARYOP, attributes, children);
        }
        return node;
    }

    /**
    * Parses boolfactor: a relation with an optional unary NOT operator preceeding it
    * @return Tree representing parsed boolfactor
    */
    private Tree boolfactor()
    {
        if (this.currenttok.type == Tokentype.NOT)
        {
            List<Tree> children = new ArrayList<Tree>();
            this.eat(Tokentype.NOT);
            children.add(this.relation());
            return new Tree(Nodetype.UNARYOP, quickHashMap("type", "!"), children);
        }
        return this.relation();
    }

    /**
    * Parses relation: (a misnomer; it's really either a boolean literal, a relation, or a sum)
    * @return Tree representing a parsed "relation"
    */
    private Tree relation()
    {
        if (this.currenttok.type == Tokentype.BOOLLITERAL)
        {
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put("value", this.currenttok.value);
            attributes.put("type", this.currenttok.type.toString());
            Tree node = new Tree(Nodetype.LITERAL, attributes);
            this.eat(Tokentype.BOOLLITERAL);
            return node;
        }
        else
        {
            Tree node = this.sum();
            Tokentype t = this.currenttok.type;
            if (match(t, Tokentype.LT, Tokentype.GT, Tokentype.LTEQ, Tokentype.GTEQ, Tokentype.EQ, Tokentype.NE))
            {
                Map<String, String> attributes = quickHashMap("type", this.currenttok.value);
                List<Tree> children = new ArrayList<Tree>();
                this.eat(t);
                children.add(node);
                children.add(this.sum());
                node = new Tree(Nodetype.BINARYOP, attributes, children);
            }
            return node;
        }
    }

    /**
    * Parses sums: arbitrary number of products separated by ADD, SUB, AND (concatenation), bitwise OR, or bitwise NOR operators
    * @return Tree of parsed sum
    */
    private Tree sum()
    {
        Tree node = this.product();
        Tokentype t = this.currenttok.type;
        String v = this.currenttok.value;
        if (match(t, Tokentype.ADD, Tokentype.SUB, Tokentype.AND) || (v.equals("or") || v.equals("nor")))
        {
            List<Tree> children = new ArrayList<Tree>();
            this.eat(t);
            children.add(node);
            children.add(this.sum());
            node = new Tree(Nodetype.BINARYOP, quickHashMap("type", v), children);
        }
        return node;
    }

    /**
    * Parses products: arbitrary number of factors separated by MUL, DIV, MOD, bitwise AND, bitwise NAND, bitwise XOR, or bitwise XNOR operators
    * @return Tree of parsed product
    */
    private Tree product()
    {
        Tree node = this.factor();
        Tokentype t = this.currenttok.type;
        String v = this.currenttok.value;
        if (match(t, Tokentype.MUL, Tokentype.DIV, Tokentype.MOD) || (v.equals("and") || v.equals("nand") || v.equals("xor") || v.equals("xnor")))
        {
            List<Tree> children = new ArrayList<Tree>();
            this.eat(t);
            children.add(node);
            children.add(this.product());
            node = new Tree(Nodetype.BINARYOP, quickHashMap("type", v), children);
        }
        return node;
    }

    /**
    * Parses factors: power with an optional unary SUB or bitwise NOT
    * @return Tree of parsed factor
    */
    private Tree factor()
    {
        if (this.currenttok.type == Tokentype.SUB || this.currenttok.value.equals("not"))
        {
            Map<String, String> attributes = quickHashMap("type", this.currenttok.value);
            List<Tree> children = new ArrayList<Tree>();
            children.add(this.power());
            return new Tree(Nodetype.UNARYOP, attributes, children);
        }
        return this.power();
    }

    /**
    * Parses power: term with an optional exponentiation (to the power of another term)
    * @return Tree of parsed power
    */
    private Tree power()
    {
        Tree node = this.term();
        Tokentype t = this.currenttok.type;
        if (t == Tokentype.EXP)
        {
            List<Tree> children = new ArrayList<Tree>();
            this.eat(t);
            children.add(node);
            children.add(this.term());
            node = new Tree(Nodetype.BINARYOP, quickHashMap("type", "**"), children);
        }
        return node;
    }

    /**
    * Term parser: this is where it gets exciting. A term can consist of a literal, identifier, or parenthetical *expression*. This allows nested expressions
    * A term can be followed by a postfix splice operator which consists of an LBRACKET, an upper index, an optional lower index (separated by a COLON), and an RBRACKET
    * @return Tree of parsed Term
    */
    private Tree term()
    {
        Tree node;
        Tokentype t = this.currenttok.type;
        if (match(t, Tokentype.DECINTLITERAL, Tokentype.BININTLITERAL, Tokentype.HEXINTLITERAL, Tokentype.BINVECLITERAL, Tokentype.HEXVECLITERAL))
        {
            node = this.literal();
        }
        else if (t == Tokentype.LPAREN)
        {
            this.eat(Tokentype.LPAREN);
            List<Tree> children = new ArrayList<Tree>();
            children.add(this.expression());
            node = new Tree(Nodetype.UNARYOP, quickHashMap("type", "()"), children);
            this.eat(Tokentype.RPAREN);
        }
        else if (t == Tokentype.ID)
        {
            node = this.identifier();
        }
        else
        {
            error("Unexpected Token", String.format("Expected LITERAL, IDENTIFIER, or LPAREN, got (%s)", this.currenttok.value.toString()), this.lexer.getline());
            return null;
        }

        if (this.currenttok.type == Tokentype.LBRACKET)
        {
            List<Tree> children = new ArrayList<Tree>();
            children.add(node);
            this.eat(Tokentype.LBRACKET);
            children.add(this.expression());
            if (this.currenttok.type == Tokentype.COLON)
            {
                this.eat(Tokentype.COLON);
                children.add(this.expression());
            }
            this.eat(Tokentype.RBRACKET);
            node = new Tree(Nodetype.TERNARYOP, quickHashMap("type", "[]"), children);
        }
        return node;
    }

    /**
    * Test Method
    */
    public static void main(String[] args)
    {
        String[] expressiontests = {
            "a + b - 2 ** c",
            "a & b",
            "a ? 2 : 3",
            "(b - a) <= 0 ? c : d"
        };
        String component = "component AndGate\n" +
                        "{\n" +
                        "  int width;\n" +
                        "  int ports;\n" +
                        "  port\n" +
                        "  {\n" +
                        "    input int inputint;\n" +
                        "    input vec inputvec[3];\n" +
                        "    output bool outputbool;\n" +
                        "  }\n" +
                        "  arch\n" +
                        "  {\n" +
                        "      signal vec foo[3];\n" +
                        "      foo <= fox < banana;\n" +
                        "      CompType compinst = new CompType(lol = 3, foo = 5, banana = x\"4\");\n" +
                        "  }\n" +
                        "}";
        for (String src : expressiontests)
        {
            Parser p = new Parser(new Lexer(src));
            Tree t = p.expression();
            String s = t.visit(0);
            System.out.println(String.format("\nTesting Expression: %s", src));
            System.out.println(s);
        }
        Parser p = new Parser(new Lexer(component));
        Tree t = p.component();
        System.out.println("\nTesting Full component definition");
        System.out.println(t);
    }
}
