package com.example.gridsmart;

import java.util.ArrayList;
import java.util.List;

/*
 * Represents an energy consumer in the grid (e.g., hospital, data center)
 * and implements the EnergyNode interface to function within the graph model.
 */
public class EnergyConsumer implements EnergyNode {
    private final String id;
    private int priority; // Priority of the consumer (lower number = higher priority)
    private double demand; // Energy demand of the consumer in kW
    private double allocatedEnergy; // Energy allocated to the consumer in kW
    private boolean active; // Whether this consumer is currently active
    private List<GraphEdge> incomingConnections; // Tracks connections from sources

    public EnergyConsumer(String id, int priority, double demand) {
        this.id = id;
        this.priority = priority;
        this.demand = demand;
        this.allocatedEnergy = 0;
        this.active = true;
        this.incomingConnections = new ArrayList<>();
    }

    /*
     * Allocate energy to this consumer
     */
    public void allocateEnergy(double energy) {
        this.allocatedEnergy = energy;
    }

    /*
     * Check if the consumer is fully allocated
     */
    public boolean isFullyAllocated() {
        return Math.abs(this.demand - this.allocatedEnergy) < 0.001; // Using epsilon comparison for doubles
    }

    /*
     * Calculate remaining energy demand
     */
    public double getRemainingDemand() {
        return Math.max(0, this.demand - this.allocatedEnergy);
    }

    @Override
    public String getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public double getDemand() {
        return demand;
    }

    public double getAllocatedEnergy() {
        return allocatedEnergy;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public void setAllocatedEnergy(double allocatedEnergy) {
        this.allocatedEnergy = allocatedEnergy;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /*
     * Add a connection from a source
     */
    public void addConnection(GraphEdge edge) {
        if (edge.getTarget() == this) {
            incomingConnections.add(edge);
        }
    }

    /*
     * Remove a connection from a source
     */
    public void removeConnection(GraphEdge edge) {
        incomingConnections.remove(edge);
    }

    /*
     * Get all connections from sources
     */
    public List<GraphEdge> getConnections() {
        return new ArrayList<>(incomingConnections);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    public String toString() {
        return "EnergyConsumer{" +
                "id=" + id +
                ", priority=" + priority +
                ", demand=" + demand +
                ", allocatedEnergy=" + allocatedEnergy +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnergyConsumer that = (EnergyConsumer) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}