/*
Nodetype.java - Reed Foster
Enum definitions for AST node types
*/

package com.foster.cdl;

public enum Nodetype
{
    COMPONENT,
    GENDEC,
    PORTDEC, PORT,
    ARCH, COMPDEC, SIGDEC, SIGASSIGN,
    TERNARYOP, BINARYOP, UNARYOP,
    IDENTIFIER, CONSTANT
}