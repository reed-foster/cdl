/*
DeclaredIdentifier.java - Reed Foster
Class for storage of identifier information
*/

package com.foster.cdl;

import java.util.*;

public class DeclaredIdentifier
{
    public final String name;
    public final String type;
    public final Tree declaration;

    DeclaredIdentifier(Tree declaration)
    {
        this.name = declaration.attributes.get("name");
        this.type = declaration.attributes.get("type");
        this.declaration = declaration;
    }

    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        DeclaredIdentifier other = (DeclaredIdentifier) obj;
        return this.name.equals(other.name);
    }
}