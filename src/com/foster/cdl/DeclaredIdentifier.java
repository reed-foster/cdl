/*
DeclaredIdentifier.java - Reed Foster
Class for storage of identifier information
*/

package com.foster.cdl;

import java.util.*;

public class DeclaredIdentifier
{
    public final String name;
    public final Tree declaration;

    DeclaredIdentifier(String name, Tree declaration)
    {
        this.name = name;
        this.declaration = declaration;
    }

    @override
    public int hashCode()
    {
        return this.name.hashCode();
    }
}