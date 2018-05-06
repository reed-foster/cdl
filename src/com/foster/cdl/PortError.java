package com.foster.cdl;

class PortError extends Error
{
    PortError(String message)
    {
        super(String.format("PortError: %s.", message));
    }
}