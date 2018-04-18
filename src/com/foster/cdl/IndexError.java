package com.foster.cdl;

class IndexError extends Error
{
    IndexError(String message)
    {
        super(String.format("IndexError: %s.", message));
    }
}