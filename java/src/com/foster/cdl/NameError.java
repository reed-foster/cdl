package com.foster.cdl;

class NameError extends Error
{
    NameError(String message)
    {
        super(String.format("NameError: %s.", message));
    }
}