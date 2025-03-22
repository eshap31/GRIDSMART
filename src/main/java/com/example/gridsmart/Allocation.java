package com.example.gridsmart;

/*
 * Represents an allocation of energy from a source to a consumer.
 * In the graph model, this corresponds to the flow on an edge.
 */
public class Allocation {
    private EnergySource source;
    private EnergyConsumer consumer;
    private double allocatedEnergy;  // Raw energy allocated, without fragmentation overhead.
    private GraphEdge edge;  // Reference to the graph edge representing this allocation

    /*
     * Creates an allocation between source and consumer with specified energy amount
     */
    public Allocation(EnergySource source, EnergyConsumer consumer, double allocatedEnergy) {
        this.source = source;
        this.consumer = consumer;
        this.allocatedEnergy = allocatedEnergy;
        this.edge = null;  // Will be set when added to the graph
    }

    /*
     * Creates an allocation based on a graph edge with its current flow
     */
    public Allocation(GraphEdge edge) {
        if (!(edge.getSource() instanceof EnergySource) || !(edge.getTarget() instanceof EnergyConsumer)) {
            throw new IllegalArgumentException("Edge must connect an EnergySource to an EnergyConsumer");
        }

        this.source = (EnergySource) edge.getSource();
        this.consumer = (EnergyConsumer) edge.getTarget();
        this.allocatedEnergy = edge.getFlow();
        this.edge = edge;
    }

    public EnergySource getSource() {
        return source;
    }

    public EnergyConsumer getConsumer() {
        return consumer;
    }

    public double getAllocatedEnergy() {
        return allocatedEnergy;
    }

    public void setAllocatedEnergy(double allocatedEnergy) {
        this.allocatedEnergy = allocatedEnergy;

        // Update the edge flow if we have a reference
        if (edge != null) {
            edge.setFlow(allocatedEnergy);
        }
    }

    /*
     * Gets the graph edge associated with this allocation
     */
    public GraphEdge getEdge() {
        return edge;
    }

    /*
     * Sets the graph edge associated with this allocation
     */
    public void setEdge(GraphEdge edge) {
        this.edge = edge;
    }

    /*
     * Updates the edge flow to match the allocation
     */
    public void synchronizeWithEdge() {
        if (edge != null) {
            edge.setFlow(allocatedEnergy);
        }
    }

    /*
     * Updates the allocation to match the edge flow
     */
    public void synchronizeWithAllocation() {
        if (edge != null) {
            allocatedEnergy = edge.getFlow();
        }
    }

    @Override
    public String toString() {
        return "Allocation{" +
                "source=" + source.getId() +
                ", consumer=" + consumer.getId() +
                ", allocatedEnergy=" + allocatedEnergy +
                '}';
    }
}