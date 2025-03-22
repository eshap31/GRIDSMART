package com.example.gridsmart;

import java.util.HashMap;
import java.util.Map;

/*
 * Central manager for energy allocations that coordinates between the graph model
 * and the allocation maps. Ensures all data structures stay synchronized.
 */
public class EnergyAllocationManager {

    private final EnergyAllocationMap allocationMap;
    private final ReverseAllocationMap reverseAllocationMap;
    private final Graph graph;

    /*
     * Creates a new allocation manager with empty maps and graph
     */
    public EnergyAllocationManager() {
        this.allocationMap = new EnergyAllocationMap();
        this.reverseAllocationMap = new ReverseAllocationMap();
        this.graph = new Graph();
    }

    /*
     * Creates a new allocation manager with an existing graph
     */
    public EnergyAllocationManager(Graph graph) {
        this.allocationMap = new EnergyAllocationMap();
        this.reverseAllocationMap = new ReverseAllocationMap();
        this.graph = graph;

        // Initialize allocation maps from the graph
        initializeFromGraph();
    }

    /*
     * Initializes allocation maps from the current graph structure and flows
     */
    private void initializeFromGraph() {
        // Clear existing allocations
        for (EnergyConsumer consumer : allocationMap.keySet()) {
            allocationMap.removeAllocations(consumer);
        }

        // For each consumer node in the graph
        for (EnergyNode node : graph.getNodesByType(NodeType.CONSUMER)) {
            EnergyConsumer consumer = (EnergyConsumer) node;

            // Look at all incoming edges (from sources)
            for (GraphEdge edge : graph.getIncomingEdges(consumer.getId())) {
                if (edge.getSource().getNodeType() == NodeType.SOURCE &&
                        edge.getFlow() > 0) {

                    EnergySource source = (EnergySource) edge.getSource();

                    // Create allocation from the edge
                    Allocation allocation = new Allocation(edge);

                    // Add to both maps without updating consumer/source states (already set in graph)
                    allocationMap.addAllocation(consumer, source, allocation);
                    reverseAllocationMap.addAllocation(source, consumer, allocation);
                }
            }
        }
    }

    /*
     * Gets the underlying graph
     */
    public Graph getGraph() {
        return graph;
    }

    /*
     * Ensures a node exists in the graph
     */
    private void ensureNodeInGraph(EnergyNode node) {
        if (graph.getNode(node.getId()) == null) {
            graph.addNode(node);
        }
    }

    /*
     * Add an allocation (keeps both maps and graph in sync)
     */
    public void addAllocation(EnergyConsumer consumer, EnergySource source, double amount) {
        // Ensure nodes are in the graph
        ensureNodeInGraph(source);
        ensureNodeInGraph(consumer);

        // Create or update edge in the graph
        GraphEdge edge = graph.getEdge(source.getId(), consumer.getId());
        if (edge == null) {
            edge = graph.addEdge(source.getId(), consumer.getId(), source.getCapacity());
        }
        edge.setFlow(amount);

        // Create allocation object
        Allocation allocation = new Allocation(edge);

        // Add to both maps
        allocationMap.addAllocation(consumer, source, allocation);
        reverseAllocationMap.addAllocation(source, consumer, allocation);

        // Update the consumer and source states
        consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() + amount);
        source.setCurrentLoad(source.getCurrentLoad() + amount);
    }

    /*
     * Get all allocations for a consumer
     */
    public Map<EnergySource, Allocation> getAllocationsForConsumer(EnergyConsumer consumer) {
        return allocationMap.getAllocations(consumer);
    }

    /*
     * Get all consumers that rely on a specific source
     */
    public Map<EnergyConsumer, Allocation> getAllocationsForSource(EnergySource source) {
        return reverseAllocationMap.getAllocations(source);
    }

    /*
     * Get a specific allocation for a consumer and source
     */
    public Allocation getAllocation(EnergyConsumer consumer, EnergySource source) {
        return allocationMap.getAllocation(consumer, source);
    }

    /*
     * Remove all allocations for a consumer
     */
    public void removeAllocationsForConsumer(EnergyConsumer consumer) {
        Map<EnergySource, Allocation> allocations = allocationMap.getAllocations(consumer);
        double totalAllocated = 0;

        for (Map.Entry<EnergySource, Allocation> entry : allocations.entrySet()) {
            EnergySource source = entry.getKey();
            Allocation allocation = entry.getValue();

            // Update source load
            source.setCurrentLoad(source.getCurrentLoad() - allocation.getAllocatedEnergy());

            // Track total energy being deallocated
            totalAllocated += allocation.getAllocatedEnergy();

            // Remove from reverse map
            reverseAllocationMap.removeAllocation(source, consumer);

            // Remove edge from graph or set flow to 0
            GraphEdge edge = graph.getEdge(source.getId(), consumer.getId());
            if (edge != null) {
                edge.setFlow(0);
            }
        }

        // Update consumer's allocated energy
        consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() - totalAllocated);

        // Remove from main map
        allocationMap.removeAllocations(consumer);
    }

    /*
     * Remove a specific allocation
     */
    public void removeAllocation(EnergyConsumer consumer, EnergySource source) {
        Allocation allocation = allocationMap.getAllocation(consumer, source);
        if (allocation != null) {
            double amount = allocation.getAllocatedEnergy();

            // Update consumer and source states
            consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() - amount);
            source.setCurrentLoad(source.getCurrentLoad() - amount);

            // Remove from both maps
            allocationMap.removeAllocation(consumer, source);
            reverseAllocationMap.removeAllocation(source, consumer);

            // Update graph edge
            GraphEdge edge = graph.getEdge(source.getId(), consumer.getId());
            if (edge != null) {
                edge.setFlow(0);
            }
        }
    }

    /*
     * Update an allocation (keeps both maps and graph in sync)
     */
    public void updateAllocation(EnergyConsumer consumer, EnergySource source, double newAmount) {
        Allocation allocation = allocationMap.getAllocation(consumer, source);
        if (allocation != null) {
            double oldAmount = allocation.getAllocatedEnergy();
            double difference = newAmount - oldAmount;

            // Update allocation amount
            allocation.setAllocatedEnergy(newAmount);

            // Both maps reference the same Allocation object, so we only need to update it once

            // Update the consumer and source states
            consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() + difference);
            source.setCurrentLoad(source.getCurrentLoad() + difference);

            // Update graph edge
            GraphEdge edge = graph.getEdge(source.getId(), consumer.getId());
            if (edge != null) {
                edge.setFlow(newAmount);
            } else if (newAmount > 0) {
                // Create edge if it doesn't exist and flow is positive
                edge = graph.addEdge(source.getId(), consumer.getId(), source.getCapacity());
                edge.setFlow(newAmount);

                // Update the allocation with the new edge
                allocation.setEdge(edge);
            }
        }
    }

    /*
     * Check if a consumer is fully allocated
     */
    public boolean isFullyAllocated(EnergyConsumer consumer) {
        Map<EnergySource, Allocation> allocations = allocationMap.getAllocations(consumer);
        double totalAllocated = 0;

        for (Allocation allocation : allocations.values()) {
            totalAllocated += allocation.getAllocatedEnergy();
        }

        return totalAllocated >= consumer.getDemand();
    }

    /*
     * Get all consumers in the system
     */
    public Map<String, EnergyConsumer> getAllConsumers() {
        Map<String, EnergyConsumer> result = new HashMap<>();

        // First add from the map
        for (EnergyConsumer consumer : allocationMap.keySet()) {
            result.put(consumer.getId(), consumer);
        }

        // Then check the graph for any additional consumers
        for (EnergyNode node : graph.getNodesByType(NodeType.CONSUMER)) {
            EnergyConsumer consumer = (EnergyConsumer) node;
            result.put(consumer.getId(), consumer);
        }

        return result;
    }

    /*
     * Get all sources in the system
     */
    public Map<String, EnergySource> getAllSources() {
        Map<String, EnergySource> result = new HashMap<>();

        // Get sources from the reverse map
        for (EnergySource source : reverseAllocationMap.keySet()) {
            result.put(source.getId(), source);
        }

        // Check the graph for any additional sources
        for (EnergyNode node : graph.getNodesByType(NodeType.SOURCE)) {
            EnergySource source = (EnergySource) node;
            result.put(source.getId(), source);
        }

        return result;
    }

    /*
     * Build a full network graph from the current allocations
     */
    public void buildGraphFromAllocations() {
        // Clear the graph
        Graph newGraph = new Graph();

        // Add all consumers from allocation map
        for (EnergyConsumer consumer : allocationMap.keySet()) {
            newGraph.addNode(consumer);

            // Add sources and edges for this consumer
            Map<EnergySource, Allocation> sourceAllocations = allocationMap.getAllocations(consumer);
            for (Map.Entry<EnergySource, Allocation> entry : sourceAllocations.entrySet()) {
                EnergySource source = entry.getKey();
                Allocation allocation = entry.getValue();

                // Add source if not already in graph
                if (newGraph.getNode(source.getId()) == null) {
                    newGraph.addNode(source);
                }

                // Add edge with capacity and flow
                GraphEdge edge = newGraph.addEdge(source.getId(), consumer.getId(), source.getCapacity());
                edge.setFlow(allocation.getAllocatedEnergy());

                // Update allocation to reference this edge
                allocation.setEdge(edge);
            }
        }

        // Replace our graph with the new one
        this.graph.getAllNodes().clear();
        for (EnergyNode node : newGraph.getAllNodes()) {
            this.graph.addNode(node);
        }

        // Copy all edges
        for (EnergyNode source : newGraph.getAllNodes()) {
            for (GraphEdge edge : newGraph.getOutgoingEdges(source.getId())) {
                EnergyNode target = edge.getTarget();
                GraphEdge newEdge = this.graph.addEdge(source.getId(), target.getId(), edge.getCapacity(), edge.getWeight());
                newEdge.setFlow(edge.getFlow());
            }
        }
    }
}