package com.example.gridsmart.graph;

import com.example.gridsmart.model.EnergyNode;

/*
 * Represents a directed edge in the energy grid graph, connecting a source node
 * to a consumer node with capacity, flow, and cost/weight properties.
 * Enhanced to support residual graph algorithms.
 */
public class GraphEdge {
    private final EnergyNode source;
    private final EnergyNode target;
    private double capacity;   // Maximum energy that can flow through this edge
    private double flow;       // Current energy flowing through this edge
    private double weight;     // Cost or weight of using this edge (can represent transmission loss, financial cost, etc.)
    private boolean active;    // Whether this edge is currently active in the network
    private boolean isReverse; // Indicates if this is a reverse edge in a residual graph
    private GraphEdge reverseEdge; // Reference to the reverse edge in a residual graph

    /*
     * Creates a new edge between source and target with specified capacity and weight
     */
    public GraphEdge(EnergyNode source, EnergyNode target, double capacity, double weight) {
        this.source = source;
        this.target = target;
        this.capacity = capacity;
        this.weight = weight;
        this.flow = 0;
        this.active = true;
        this.isReverse = false;
        this.reverseEdge = null;
    }

    /*
     * Creates a new edge between source and target with specified capacity and default weight of 1.0
     */
    public GraphEdge(EnergyNode source, EnergyNode target, double capacity) {
        this(source, target, capacity, 1.0);
    }

    /*
     * Creates a new residual edge (either forward or reverse)
     */
    public GraphEdge(EnergyNode source, EnergyNode target, double capacity, double weight, boolean isReverse) {
        this(source, target, capacity, weight);
        this.isReverse = isReverse;
    }

    /*
     * Returns the source node of this edge
     */
    public EnergyNode getSource() {
        return source;
    }

    /*
     * Returns the target node of this edge
     */
    public EnergyNode getTarget() {
        return target;
    }

    /*
     * Returns the capacity of this edge
     */
    public double getCapacity() {
        return capacity;
    }

    /*
     * Sets the capacity of this edge
     */
    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    /*
     * Returns the current flow through this edge
     */
    public double getFlow() {
        return flow;
    }

    /*
     * Sets the flow through this edge
     * If a reverse edge exists, updates its capacity accordingly
     */
    public void setFlow(double flow) {
        if (flow > capacity && !isReverse) {
            throw new IllegalArgumentException("Flow cannot exceed capacity");
        }

        double flowChange = flow - this.flow;
        this.flow = flow;

        // Update the reverse edge if it exists
        if (reverseEdge != null) {
            // When we increase flow in this edge, we increase capacity in reverse edge
            reverseEdge.capacity += flowChange;

            // Decrease capacity in this edge
            if (!isReverse) {
                this.capacity -= flowChange;
            }
        }
    }

    /*
     * Returns the residual capacity of this edge
     * For forward edges: capacity - flow
     * For reverse edges: flow (of the original edge)
     */
    public double getResidualCapacity() {
        if (isReverse) {
            // For reverse edges, the residual capacity is the flow of the original edge
            return capacity;
        } else {
            // For forward edges, the residual capacity is the unused capacity
            return capacity - flow;
        }
    }

    /*
     * Returns the weight/cost of this edge
     */
    public double getWeight() {
        return weight;
    }

    /*
     * Sets the weight/cost of this edge
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /*
     * Returns true if this edge is active
     */
    public boolean isActive() {
        return active;
    }

    /*
     * Sets the active status of this edge
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /*
     * Returns true if this is a reverse edge
     */
    public boolean isReverse() {
        return isReverse;
    }

    /*
     * Sets whether this is a reverse edge
     */
    public void setReverse(boolean isReverse) {
        this.isReverse = isReverse;
    }

    /*
     * Get the reverse edge reference
     */
    public GraphEdge getReverseEdge() {
        return reverseEdge;
    }

    /*
     * Set the reverse edge reference
     */
    public void setReverseEdge(GraphEdge reverseEdge) {
        this.reverseEdge = reverseEdge;
    }

    /*
     * Creates a reverse edge for residual graph in flow algorithms
     * and sets up the bidirectional relationship between edges
     */
    public GraphEdge createReverseEdge() {
        // Reverse edges have the same nodes but swapped, and a negated weight
        GraphEdge reverse = new GraphEdge(target, source, 0, -weight, true);

        // Set up the bidirectional relationship
        this.reverseEdge = reverse;
        reverse.reverseEdge = this;

        return reverse;
    }

    @Override
    public String toString() {
        return "GraphEdge{" +
                "source=" + source.getId() +
                ", target=" + target.getId() +
                ", capacity=" + capacity +
                ", flow=" + flow +
                ", weight=" + weight +
                ", active=" + active +
                ", isReverse=" + isReverse +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return source.getId().equals(graphEdge.source.getId()) &&
                target.getId().equals(graphEdge.target.getId()) &&
                isReverse == graphEdge.isReverse;
    }
}