package com.example.gridsmart.graph;

import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/*
 * A specialized HashMap that maps sources to their consumer allocations.
 * Updated to work with the graph-based model by storing edge references.
 */
public class ReverseAllocationMap extends HashMap<EnergySource, HashMap<EnergyConsumer, Allocation>> {

    /*
     * Creates a new empty reverse allocation map
     */
    public ReverseAllocationMap() {
        super();
    }

    /*
     * Adds an allocation to the map (tracks consumers depending on a source)
     */
    public void addAllocation(EnergySource source, EnergyConsumer consumer, Allocation allocation) {
        this.computeIfAbsent(source, k -> new HashMap<EnergyConsumer, Allocation>())
                .put(consumer, allocation);
    }

    /*
     * Gets all consumers that rely on a specific source
     */
    public Map<EnergyConsumer, Allocation> getAllocations(EnergySource source) {
        return this.containsKey(source) ? this.get(source) : Collections.<EnergyConsumer, Allocation>emptyMap();
    }

    /*
     * Retrieves a specific consumer's allocation from a source
     */
    public Allocation getAllocation(EnergySource source, EnergyConsumer consumer) {
        Map<EnergyConsumer, Allocation> consumerMap = this.get(source);
        return (consumerMap != null) ? consumerMap.get(consumer) : null;
    }

    /*
     * Removes all allocations for a given source
     */
    public void removeAllocations(EnergySource source) {
        this.remove(source);
    }

    /*
     * Removes a specific consumer's allocation from a source
     */
    public void removeAllocation(EnergySource source, EnergyConsumer consumer) {
        Map<EnergyConsumer, Allocation> allocations = this.get(source);
        if (allocations != null) {
            allocations.remove(consumer);
            if (allocations.isEmpty()) {
                this.remove(source);  // Clean up empty sources
            }
        }
    }

    /*
     * Updates an existing allocation amount in O(1)
     */
    public void updateAllocation(EnergySource source, EnergyConsumer consumer, double newAmount) {
        Map<EnergyConsumer, Allocation> allocations = this.get(source);
        if (allocations != null && allocations.containsKey(consumer)) {
            allocations.get(consumer).setAllocatedEnergy(newAmount);
        }
    }

    /*
     * Synchronizes all allocations with the corresponding graph edges
     */
    public void synchronizeWithGraph(Graph graph) {
        for (EnergySource source : keySet()) {
            Map<EnergyConsumer, Allocation> consumerAllocations = get(source);

            for (Map.Entry<EnergyConsumer, Allocation> entry : consumerAllocations.entrySet()) {
                EnergyConsumer consumer = entry.getKey();
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
        for (EnergySource source : keySet()) {
            Map<EnergyConsumer, Allocation> consumerAllocations = get(source);

            for (Map.Entry<EnergyConsumer, Allocation> entry : consumerAllocations.entrySet()) {
                EnergyConsumer consumer = entry.getKey();
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