/*
Graph.java - Reed Foster
Graph class with strings for vertices
*/

package com.foster.cdl;

import java.util.*;

public class Graph
{
    private int vertices; //number of vertices
    private Map<String, Integer> nameMap; //maps names to index in adjacency list
    private List<List<Integer>> adjacencyList;

    Graph()
    {
        this.vertices = 0;
        this.nameMap = new HashMap<String, Integer>();
        this.adjacencyList = new ArrayList<List<Integer>>();
    }

    /**
    * edge configuration: verex1 -> vertex2
    * @param vertex1 String name of "from" vertex
    * @param vertex2 String name of "to" vertex
    * 
    */
    public void addEdge(String vertex1, String vertex2)
    {
        int idx1 = this.nameMap.getOrDefault(vertex1, -1);
        int idx2 = this.nameMap.getOrDefault(vertex2, -1);
        if (idx1 == -1) // new vertex
        {
            idx1 = this.adjacencyList.size();
            this.nameMap.put(vertex1, idx1);
            this.adjacencyList.add(new ArrayList<Integer>());
            this.vertices++;
        }
        if (idx2 == -1) // new vertex
        {
            idx2 = this.adjacencyList.size();
            this.nameMap.put(vertex2, idx2);
            this.adjacencyList.add(new ArrayList<Integer>());
            this.vertices++;
        }
        if (!this.adjacencyList.get(idx1).contains(idx2))
            this.adjacencyList.get(idx1).add(idx2); // if there isn't already an edge from vertex1 to vertex2, add it to the adjacency list
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