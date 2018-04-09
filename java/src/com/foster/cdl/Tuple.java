/*
Tuple.java - Reed Foster
Class for tuples of size 2, used as element type of adjacency list for directed graphs
*/

package com.foster.cdl;

public class Tuple<T>
{
    public final T a;
    public final T b;
    Tuple(T a, T b)
    {
        this.a = a;
        this.b = b;
    }

    public boolean equals(Tuple<T> other)
    {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (!(other instanceof Tuple))
            return false;
        Tuple<T> other_ = (Tuple<T>) other;
        return other_.a.equals(this.a) && other_.b.equals(this.b);
    }
}