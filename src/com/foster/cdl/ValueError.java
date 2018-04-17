package com.foster.cdl;

class ValueError extends Error
{
    ValueError(String message)
    {
        super(String.format("ValueError: %s.", message));
    }
}