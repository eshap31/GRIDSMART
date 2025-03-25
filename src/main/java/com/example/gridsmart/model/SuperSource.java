package com.example.gridsmart.model;

/*
 * SuperSource node that connects to all energy sources in the network.
 * S vertex in Edmonds-Karp algo
 */
public class SuperSource extends SuperNode {
    public SuperSource(String id) {
        super(id);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.SUPER_SOURCE;
    }
}