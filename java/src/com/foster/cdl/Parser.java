/*
Parser.java - Reed Foster
Generates AST from token stream based on CDL language grammar
*/

package com.foster.cdl;

import java.util.*;

public class Parser extends LexicalAnalyzer
{
    private Token currenttok;
    private Lexer lexer;
    
    Parser(Lexer lexer)
    {
        this.lexer = lexer;
        this.currenttok = this.lexer.getNextToken();
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
            error("Unexpected Token", this.currenttok.value, this.lexer.getline());
    }

    /**
    * Advanced eat method; verifies that the current token's value is the same as the supplied String value
    * @param type same as normal eat method
    * @param value checks against currenttok.value
    */
    private void eat(Tokentype type, String value)
    {
        if (this.currenttok.value.compareTo(value) == 0)
            this.eat(type);
        else
            error("Unexpected Token", this.currenttok.value, this.lexer.getline());
    }

    /**
    * Initializes a hashmap with one key and one value
    * @param k key String
    * @param v value String
    * @return HashMap containing (k -> v)
    */
    private Map quickHashMap(String k, String v)
    {
        Map hm = new HashMap<String, String>();
        hm.put(k, v);
        return hm;
    }

    public Tree parse()
    {
        
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
            else if (this.currenttok.value.compareTo("port") == 0)
                children.add(this.portdec());
            else if (this.currenttok.value.compareTo("arch") == 0)
                children.add(this.arch());
            else
                error("Unexpected Token", this.currenttok.value, this.lexer.getline());
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
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("type", this.currenttok.value);
        this.eat(Tokentype.RESERVED, this.currenttok.value);
        attributes.put("name", this.currenttok.value);
        this.eat(Tokentype.ID);
        if (attributes.get("type").compareTo("vec") == 0)
        {
            this.eat(Tokentype.LBRACKET);
            attributes.put("width", this.currenttok.value);
            this.eat(Tokentype.DECINTCONST);
            this.eat(Tokentype.RBRCKET);
        }
        this.eat(Tokentype.EOL);
        return new Tree(Nodetype.GENDEC, attributes);
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
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put("direction", this.currenttok.value);
        this.eat(Tokentype.RESERVED);
        attributes.put("type", this.currenttok.value);
        this.eat(Tokentype.RESERVED);
        attributes.put("name", this.currenttok.value);
        this.eat(Tokentype.ID);
        if (attributes.get("type").compareTo("vec") == 0)
        {
            this.eat(Tokentype.LBRACKET);
            attributes.put("width", this.currenttok.value);
            this.eat(Tokentype.DECINTCONST);
            this.eat(Tokentype.RBRCKET);
        }
        this.eat(Tokentype.EOL);
        return new Tree(Nodetype.PORT, attributes);
    }

    /**
    * Parses architecture ("arch", LBRACE, {archbody}, RBRACE)
    * @return Tree with root node of type Nodetype.ARCH and children of type Nodetype.SIGDEC, Nodetype.COMPDEC, and Nodetype.BINARYOP
    */
    private Tree arch()
    {
        List<Tree> children = new ArrayList<Tree>();

        this.eat(Tokentype.RESERVED, "arch");
        this.eat(Tokentype.LBRACE);
        while (this.currenttok.type != Tokentype.RBRACE)
        {
            if (this.currenttok.value.compareTo("signal") == 0)
                children.add(this.sigdec())
            else if (this.currenttok.type == Tokentype.ID)
            {
                Tree id = self.identifier();
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
                    this.eat(Tokentype.ID);
                    this.eat(Tokentype.EQ);
                    this.eat(Tokentype.RESERVED, "new")
                    this.eat(Tokentype.ID, id.attributes.get("value")); // verify assigned component instance is the same type as declared
                    this.eat(Tokentype.LPAREN);
                    if (this.currenttok.type != Nodetype.RPAREN)
                        compchildren.add(this.genericlist())
                    children.add(new Tree(Nodetype.COMPDEC, compattr, compchildren));
                }
            }
            else
                error("Unexpected Token", this.currenttok.value, this.lexer.getline());
        }
        return new Tree(Nodetype.ARCH, attributes, children);
    }

    /**
    * Parses signal declaration ("signal", {type}, {name})
    * @return Tree with root node of type Nodetype.SIGDEC
    */
    private Tree sigdec()
    {
        Map<String, String> attributes = new HashMap<String, String>();
        this.eat(Tokentype.RESERVED, "signal");
        attributes.put("type", this.currenttok.value);
        this.eat(Tokentype.RESERVED, this.currenttok.value);
        attributes.put("name", this.currenttok.value);
        this.eat(Tokentype.ID);
        if (attributes.get("type").compareTo("vec") == 0)
        {
            this.eat(Tokentype.LBRACKET);
            attributes.put("width", this.currenttok.value);
            this.eat(Tokentype.DECINTCONST);
            this.eat(Tokentype.RBRCKET);
        }
        this.eat(Tokentype.EOL);
        return new Tree(Nodetype.SIGDEC, attributes);
    }

    /**
    * Parses generic initialization assignments for component declaration and generic mapping
    * @return Tree with root node of type Nodetype.BINARYOP and children of type Nodetype.BINARYOP, Nodetype.IDENTIFIER, and Nodetype.CONSTANT
    */
    private Tree genericlist() // list of generic initializations for component declaration/generic mapping
    {
        List<Tree> children = new ArrayList<Tree>();
        children.add(this.identifier(false));
        this.eat(Tokentype.ID);
        this.eat(Tokentype.EQ);
        children.add(this.constant());
        Tree assignment = new Tree(Nodetype.BINARYOP, quickHashMap("type", "="), children);
        if (this.currenttok.type == Tokentype.COMMA)
        {
            this.eat(Tokentype.COMMA);
            children = new ArrayList<Tree>();
            children.add(assignment);
            children.add(this.genericlist());
            return new Tree(Nodetype.BINARYOP, quickHashMap("type", ","), children);
        }
        return assignment;
    }

    /**
    * Parses identifiers, conditionally parsing "compound" identifiers ({id}, PERIOD, {id})
    * @param allowcompound (boolean) true if parsing of a compound identifier is desired, false otherwise
    * @return Tree with root node of type Nodetype.IDENTIFIER or Nodetype.BINARYOP (which would have children of type Nodetype.IDENTIFIER)
    */
    private Tree identifier(boolean allowcompound)
    {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("value", this.currenttok.value);
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
    * Parses constants/literals
    * @return Tree with root node of type Nodetype.CONSTANT
    */
    private Tree constant()
    {
        Map<String, String> attributes = new HashMap<String, String>();
        Tokentype t = this.currenttok.type;
        attributes.put("value", this.currenttok.value);
        attributes.put("type", t)
        if (t == Tokentype.DECINTCONST || t == Tokentype.BININTCONST || t == Tokentype.HEXINTCONST || t == Tokentype.BINVECCONST || t == Tokentype.HEXVECCONST || t == Tokentype.BOOLCONST)
            this.eat(t);
        else
            error("Unexpected Token", this.currenttok.value, this.lexer.getLine());
        return new Tree(Nodetype.CONSTANT, attributes);
    }

    private Tree expression()
    {

    }

    private Tree boolexpr()
    {

    }

    private Tree boolfactor()
    {

    }

    private Tree relation()
    {

    }

    private Tree sum()
    {

    }

    private Tree product()
    {

    }

    private Tree factor()
    {

    }

    private Tree power()
    {

    }

    private Tree term()
    {

    }
}