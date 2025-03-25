package com.example.gridsmart.model;

/*
    * SuperNode base class
    * SuperSink and SuperSource inherit from this class
 */
public abstract class SuperNode implements EnergyNode {
    protected final String id;
    protected boolean active;

    public SuperNode(String id) {
        this.id = id;
        this.active = true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public abstract NodeType getNodeType();
}