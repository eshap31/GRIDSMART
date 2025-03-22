package com.example.gridsmart.model;

/*
    Interface that represents a node in the energy grid graph
 */
public interface EnergyNode {
    /*
        * Get the unique identifier of the node
     */
    String getId();

    /*
        * Returns true if the node is currently active/online in the grid
     */
    boolean isActive();

    /*
        * Get the type of the node
     */
    NodeType getNodeType();
}
