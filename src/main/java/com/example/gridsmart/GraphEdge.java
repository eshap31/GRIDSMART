package com.example.gridsmart;

/*
 * Represents a directed edge in the energy grid graph, connecting a source node
 * to a consumer node with capacity, flow, and cost/weight properties.
 */
public class GraphEdge
{
    private final EnergyNode source;
    private final EnergyNode target;
    private double capacity;   // Maximum energy that can flow through this edge
    private double flow;       // Current energy flowing through this edge
    private double weight;     // Cost or weight of using this edge (can represent transmission loss, financial cost, etc.)
    private boolean active;    // Whether this edge is currently active in the network

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
    }

    /*
     * Creates a new edge between source and target with specified capacity and default weight of 1.0
     */
    public GraphEdge(EnergyNode source, EnergyNode target, double capacity) {
        this(source, target, capacity, 1.0);
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
     */
    public void setFlow(double flow) {
        if (flow > capacity) {
            throw new IllegalArgumentException("Flow cannot exceed capacity");
        }
        this.flow = flow;
    }

    /*
     * Returns the residual capacity of this edge (capacity - flow)
     */
    public double getResidualCapacity() {
        return capacity - flow;
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
     * Creates a reverse edge for residual graph in flow algorithms
     */
    public GraphEdge createReverseEdge() {
        GraphEdge reverse = new GraphEdge(target, source, 0, -weight);
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
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return source.getId().equals(graphEdge.source.getId()) &&
                target.getId().equals(graphEdge.target.getId());
    }
}
