/*
SemanticAnalyzer.java - Reed Foster
Class for verification of semantics of parsed source code (i.e. "yes, it's valid syntax, but does it make sense?")
*/

package com.foster.cdl;

import java.util.*;

public class SemanticAnalyzer
{
    private List<Tree> components;

    SemanticAnalyzer(String source)
    {
        components = new ArrayList<Tree>();
        Parser parser;
        int startindex = 0;
        int endindex = 0;
        do
        {
            startindex = source.indexOf("component", endindex);
            endindex = source.indexOf("component", startindex + 1);
            endindex = endindex == -1 ? source.length() - 1 : endindex;
            parser = new Parser(new Lexer(source.substring(startindex, endindex)));
            components.add(parser.parse());
        } while (source.indexOf("component", endindex) != -1);
    }

    /*public void verify()
    {

    }

    private void orderDependencies()
    {
        for (Tree component : this.components)
        {

        }
    }*/

    private ArrayList<HashMap<String,String>> getCompDecs(Tree node)
    {
        if (node.nodetype == Nodetype.COMPDEC)
        {
            ArrayList<HashMap<String,String>> cdecs = new ArrayList<HashMap<String, String>>();
            cdecs.add(new HashMap<String, String>(node.attributes));
            return cdecs;
        }
        //getCompDecs on all children
        ArrayList<HashMap<String,String>> compdecs = new ArrayList<HashMap<String, String>>();
        for (Tree child : node.getChildren())
        {
            compdecs.addAll(this.getCompDecs(child));
        }
        return compdecs;
    }

    public static void main(String[] args)
    {
        String source = "component C1{port{}arch{C2 c2 = new C2();C3 c3 = new C3();}} component C2{port{}arch{C3 c3 = new C3();}} component C3{port{}arch{}}";
        SemanticAnalyzer s = new SemanticAnalyzer(source);
        for (int i = 0; i < s.components.size(); i++)
        {
            System.out.println(String.format("component %s has the following dependencies", s.components.get(i).attributes.get("name")));
            ArrayList<HashMap<String,String>> cdecs = s.getCompDecs(s.components.get(i));
            for (Map cdec : cdecs)
            {
                System.out.println(cdec.get("type"));
            }
        }
    }
}