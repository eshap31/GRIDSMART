package com.example.gridsmart.model;

/*
 * SuperSink node that connects from all energy consumers in the network.
 * T vertex in Edmonds-Karp algo
 */
public class SuperSink extends SuperNode {
    public SuperSink(String id) {
        super(id);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.SUPER_SINK;
    }
}