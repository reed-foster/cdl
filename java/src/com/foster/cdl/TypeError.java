package com.foster.cdl;

class TypeError extends Error
{
    TypeError(String message)
    {
        super(String.format("TypeError: %s.", message));
    }
}