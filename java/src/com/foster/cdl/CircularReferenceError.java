package com.foster.cdl;

class CircularReferenceError extends Error
{
    CircularReferenceError(String message)
    {
        super(message);
    }
}