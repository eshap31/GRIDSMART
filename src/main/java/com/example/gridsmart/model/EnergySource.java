package com.example.gridsmart.model;

import com.example.gridsmart.graph.GraphEdge;

import java.util.ArrayList;
import java.util.List;

/*
 * Represents an energy source in the grid (e.g., solar panel, wind turbine)
 * and implements the EnergyNode interface to function within the graph model.
 */
public class EnergySource implements EnergyNode {
    private final String id; // ID for the energy source
    private double capacity; // maximum energy source can provide in kW
    private double currentLoad; // current load of the energy source in kW
    private boolean active; // true = Active, false = Offline
    private SourceType type; // Type of the energy source
    private List<GraphEdge> outgoingConnections; // Tracks connections to consumers

    public EnergySource(String id, double capacity, SourceType type) {
        this.id = id;
        this.capacity = capacity;
        this.currentLoad = 0;
        this.active = true;
        this.type = type;
        this.outgoingConnections = new ArrayList<>();
    }

    /*
     * Calculate the available energy remaining in this source
     */
    public double getAvailableEnergy() {
        return capacity - currentLoad;
    }

    /*
     * Set available energy by recalculating the current load
     */
    public void setAvailableEnergy(double availableEnergy) {
        this.currentLoad = this.capacity - availableEnergy;
    }

    @Override
    public String getId() {
        return id;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(double currentLoad) {
        this.currentLoad = currentLoad;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public SourceType getType() {
        return type;
    }

    /*
     * Add a connection to a consumer
     */
    public void addConnection(GraphEdge edge) {
        if (edge.getSource() == this) {
            outgoingConnections.add(edge);
        }
    }

    /*
     * Remove a connection to a consumer
     */
    public void removeConnection(GraphEdge edge) {
        outgoingConnections.remove(edge);
    }

    /*
     * Get all connections to consumers
     */
    public List<GraphEdge> getConnections() {
        return new ArrayList<>(outgoingConnections);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.SOURCE;
    }

    @Override
    public String toString() {
        return "EnergySource{" +
                "id=" + id +
                ", capacity=" + capacity +
                ", currentLoad=" + currentLoad +
                ", active=" + active +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnergySource that = (EnergySource) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}