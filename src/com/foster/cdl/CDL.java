/*
CDL.java - Reed Foster
Main class, reads source from files and writes to terminal for output
*/

package com.foster.cdl;

import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.*;

public class CDL
{
    private static String readFile(String filename) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(filename));
        return new String(encoded, Charset.defaultCharset());
    }

    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.out.println("Please supply at least one source");
            return;
        }
        String source = "";
        for (String arg : args)
        {
            try
            {
                source += readFile(arg) + "\n";
            }
            catch (IOException e)
            {
                System.out.println("Invalid filename");
                return;
            }
        }
        VHDLGenerator gen = new VHDLGenerator(source);
        System.out.println(gen.getVHDL());
    }
}