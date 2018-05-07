/*
CDL.java - Reed Foster
Main class, reads source from files and writes to terminal for output
*/

package com.foster.cdl;

import java.util.*;
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

    private static void writeFile(String filename, String contents) throws IOException
    {
        List<String> lines = Arrays.asList(contents.split("\n"));
        Path file = Paths.get(filename);
        Files.write(file, lines, Charset.defaultCharset());//, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.out.println("Please supply a destination");
            return;
        }
        String[] sourceFiles = new String[args.length - 1];
        String dest = args[0];
        for (int i = 1; i < args.length; i++)
            sourceFiles[i - 1] = args[i];
        if (sourceFiles.length < 1)
        {
            System.out.println("Please supply at least one source");
            return;
        }
        String source = "";
        for (String sourceFile : sourceFiles)
        {
            try
            {
                source += readFile(sourceFile) + "\n";
            }
            catch (IOException e)
            {
                System.out.println("Invalid filename");
                return;
            }
        }
        VHDLGenerator gen = new VHDLGenerator(source);
        String output = gen.getVHDL();
        try
        {
            writeFile(dest, output);
        }
        catch (IOException e)
        {
            System.out.println("Failed to write to output file");
            return;
        }
    }
}