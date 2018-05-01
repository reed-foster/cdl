/*
Graph.java - Reed Foster
Graph class with strings for vertices
*/

package com.foster.cdl;

import java.util.*;

public class Graph
{
    private int vertices; //number of vertices
    private List<String> nameMap; //maps names to index in adjacency list
    private List<List<Integer>> adjacencyList; // each element of the list stores a list of the address of child vertices
    private List<Integer> inDegrees; // each element stores the indegree of the vertex whose adress corresponds to the index of the list

    Graph()
    {
        this.vertices = 0;
        this.nameMap = new ArrayList<String>();
        this.adjacencyList = new ArrayList<List<Integer>>();
        this.inDegrees = new ArrayList<Integer>();
    }

    /**
    * edge configuration: verex1 -> vertex2
    * @param vertex1 String name of "from" vertex
    * @param vertex2 String name of "to" vertex
    * 
    */
    public void addEdge(String vertex1, String vertex2)
    {
        int idx1 = this.nameMap.indexOf(vertex1); // doesn't matter that indexOf only returns the index of the first instance; there are no duplicate entries in nameMap
        int idx2 = this.nameMap.indexOf(vertex2);
        if (idx1 == -1) // new vertex
        {
            idx1 = this.adjacencyList.size();
            this.nameMap.add(idx1, vertex1);
            this.adjacencyList.add(new ArrayList<Integer>());
            this.inDegrees.add(0);
            this.vertices++;
        }
        if (idx2 == -1) // new vertex
        {
            idx2 = this.adjacencyList.size();
            this.nameMap.add(idx2, vertex2);
            this.adjacencyList.add(new ArrayList<Integer>());
            this.inDegrees.add(0);
            this.vertices++;
        }
        if (!this.adjacencyList.get(idx1).contains(idx2))
        {
            this.adjacencyList.get(idx1).add(idx2); // if there isn't already an edge from vertex1 to vertex2, add it to the adjacency list
            this.inDegrees.set(idx2, this.inDegrees.get(idx2) + 1); // increment the indegree of the "to" vertex
        }
    }

    public String rootVertex()
    {
        for (int i = 0; i < this.vertices; i++)
        {
            if (this.inDegrees.get(i) == 0)
                return this.nameMap.get(i); 
        }
        return "";
    }

    public boolean acyclic()
    {
        boolean[] visited = new boolean[this.vertices];
        boolean[] recStack = new boolean[this.vertices];
        for (int i = 0; i < this.vertices; i++)
        {
            visited[i] = false;
            recStack[i] = false;
        }
        for (int i = 0; i < this.vertices; i++)
        {
            if (cyclicUtil(i, visited, recStack))
                return false;
        }
        return true;
    }

    private boolean cyclicUtil(int vertex, boolean[] visited, boolean[] recStack)
    {
        visited[vertex] = true;
        recStack[vertex] = true;
        for (Integer child : this.adjacencyList.get(vertex))
        {
            if (!visited[child]) //if the child vertex is not visited, recurse
            {
                if (this.cyclicUtil(child, visited, recStack))
                    return true;
            }
            else if (recStack[child]) //visited, and on recStack; this is where the magic happens
            {
                return true;
            }
        }
        recStack[vertex] = false;
        return false;
    }
}