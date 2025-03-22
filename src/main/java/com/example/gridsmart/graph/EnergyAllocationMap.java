package com.example.gridsmart.graph;

import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/*
 * A specialized HashMap that maps consumers to their source allocations.
 * Updated to work with the graph-based model by storing edge references.
 */
public class EnergyAllocationMap extends HashMap<EnergyConsumer, HashMap<EnergySource, Allocation>> {

    /*
     * Creates a new empty allocation map
     */
    public EnergyAllocationMap() {
        super();
    }

    /*
     * Adds an allocation to the map
     */
    public void addAllocation(EnergyConsumer consumer, EnergySource source, Allocation allocation) {
        this.computeIfAbsent(consumer, k -> new HashMap<EnergySource, Allocation>())
                .put(source, allocation);
    }

    /*
     * Gets all allocations for a specific consumer
     */
    public Map<EnergySource, Allocation> getAllocations(EnergyConsumer consumer) {
        return this.containsKey(consumer) ? this.get(consumer) : Collections.<EnergySource, Allocation>emptyMap();
    }

    /*
     * Retrieves a specific allocation in O(1) time
     */
    public Allocation getAllocation(EnergyConsumer consumer, EnergySource source) {
        Map<EnergySource, Allocation> consumerMap = this.get(consumer);
        return (consumerMap != null) ? consumerMap.get(source) : null;
    }

    /*
     * Removes all allocations for a consumer
     */
    public void removeAllocations(EnergyConsumer consumer) {
        this.remove(consumer);
    }

    /*
     * Removes a specific allocation in O(1) time
     */
    public void removeAllocation(EnergyConsumer consumer, EnergySource source) {
        Map<EnergySource, Allocation> allocations = this.get(consumer);
        if (allocations != null) {
            allocations.remove(source);
            if (allocations.isEmpty()) {
                // Clean up empty consumers
                this.remove(consumer);
            }
        }
    }

    /*
     * Updates an existing allocation amount in O(1)
     */
    public void updateAllocation(EnergyConsumer consumer, EnergySource source, double newAmount) {
        Map<EnergySource, Allocation> allocations = this.get(consumer);
        if (allocations != null && allocations.containsKey(source)) {
            Allocation allocation = allocations.get(source);
            allocation.setAllocatedEnergy(newAmount);
        }
    }

    /*
     * Checks if a consumer is fully allocated
     */
    public boolean isFullyAllocated(EnergyConsumer consumer) {
        double totalAllocated = this.getAllocations(consumer).values().stream()
                .mapToDouble(Allocation::getAllocatedEnergy)
                .sum();
        return totalAllocated >= consumer.getDemand();
    }

    /*
     * Synchronizes all allocations with the corresponding graph edges
     */
    public void synchronizeWithGraph(Graph graph) {
        for (EnergyConsumer consumer : keySet()) {
            Map<EnergySource, Allocation> sourceAllocations = get(consumer);

            for (Map.Entry<EnergySource, Allocation> entry : sourceAllocations.entrySet()) {
                EnergySource source = entry.getKey();
                Allocation allocation = entry.getValue();

                // Get or create the edge in the graph
                GraphEdge edge = graph.getEdge(source.getId(), consumer.getId());
                if (edge == null && allocation.getAllocatedEnergy() > 0) {
                    edge = graph.addEdge(source.getId(), consumer.getId(), source.getCapacity());
                }

                if (edge != null) {
                    // Update edge flow to match allocation
                    edge.setFlow(allocation.getAllocatedEnergy());

                    // Update allocation with edge reference
                    allocation.setEdge(edge);
                }
            }
        }
    }

    /*
     * Updates allocation values from the graph edges
     */
    public void updateFromGraph(Graph graph) {
        for (EnergyConsumer consumer : keySet()) {
            Map<EnergySource, Allocation> sourceAllocations = get(consumer);

            for (Map.Entry<EnergySource, Allocation> entry : sourceAllocations.entrySet()) {
                EnergySource source = entry.getKey();
                Allocation allocation = entry.getValue();

                // Get the edge from the graph
                GraphEdge edge = graph.getEdge(source.getId(), consumer.getId());
                if (edge != null) {
                    // Update allocation with edge flow
                    allocation.setAllocatedEnergy(edge.getFlow());
                }
            }
        }
    }
}