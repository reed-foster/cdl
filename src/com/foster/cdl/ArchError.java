package com.foster.cdl;

class ArchError extends Error
{
    ArchError(String message)
    {
        super(String.format("ArchError: %s.", message));
    }
}