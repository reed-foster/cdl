package com.foster.cdl;

class GenericError extends Error
{
    GenericError(String message)
    {
        super(String.format("GenericError: %s.", message));
    }
}