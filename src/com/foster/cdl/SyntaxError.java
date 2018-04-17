package com.foster.cdl;

class SyntaxError extends Error
{
    SyntaxError(String message)
    {
        super(String.format("SyntaxError: %s.", message));
    }
}