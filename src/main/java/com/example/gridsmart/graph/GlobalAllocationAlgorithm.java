package com.example.gridsmart.graph;

import com.example.gridsmart.model.*;
import com.example.gridsmart.util.EnergyConsumerQueue;
import java.util.*;

/**
 * Implementation of prioritized global energy allocation algorithm.
 * Allocates energy to consumers in order of their priority levels.
 */
public class GlobalAllocationAlgorithm {

    /**
     * Runs the prioritized allocation algorithm on the given graph.
     * Consumers with higher priority (lower priority number) are served first.
     *
     * @param graph The energy network graph
     * @param consumers List of energy consumers
     * @param sources List of energy sources
     */
    public void run(Graph graph, List<EnergyConsumer> consumers, List<EnergySource> sources) {
        // Make sure all consumers and sources are in the graph
        for (EnergySource source : sources) {
            if (graph.getNode(source.getId()) == null) {
                graph.addNode(source);
            }
        }

        for (EnergyConsumer consumer : consumers) {
            if (graph.getNode(consumer.getId()) == null) {
                graph.addNode(consumer);
            }
        }

        // Create a priority queue for consumers
        EnergyConsumerQueue consumerQueue = new EnergyConsumerQueue();

        // Add all consumers to the queue
        for (EnergyConsumer consumer : consumers) {
            consumerQueue.add(consumer);
        }

        // Group consumers by priority level
        Map<Integer, List<EnergyConsumer>> priorityGroups = new HashMap<>();

        // Extract consumers from queue to maintain priority order
        while (!consumerQueue.isEmpty()) {
            EnergyConsumer consumer = consumerQueue.poll();
            int priority = consumer.getPriority();

            // Add to appropriate priority group
            if (!priorityGroups.containsKey(priority)) {
                priorityGroups.put(priority, new ArrayList<>());
            }
            priorityGroups.get(priority).add(consumer);
        }

        // Get priority levels in ascending order (lower number = higher priority)
        List<Integer> priorityLevels = new ArrayList<>(priorityGroups.keySet());
        Collections.sort(priorityLevels);

        // Create super source
        SuperSource superSource = graph.addSuperSource("super_source");

        // Process each priority level in order
        for (int priorityLevel : priorityLevels) {
            List<EnergyConsumer> priorityConsumers = priorityGroups.get(priorityLevel);

            // Create super sink for this priority level
            SuperSink superSink = new SuperSink("super_sink_p" + priorityLevel);
            graph.addNode(superSink);  // Explicitly add the node before adding edges

            // Connect only consumers at this priority level to the super sink
            List<GraphEdge> consumerToSinkEdges = new ArrayList<>();
            for (EnergyConsumer consumer : priorityConsumers) {
                // Only connect if consumer still has remaining demand
                if (consumer.getRemainingDemand() > 0) {
                    double demandCapacity = consumer.getRemainingDemand();
                    GraphEdge edge = graph.addEdge(consumer.getId(), superSink.getId(), demandCapacity);
                    consumerToSinkEdges.add(edge);
                }
            }

            // Update super source edges based on current available energy
            graph.updateSuperSourceEdges();

            // Run Edmonds-Karp algorithm for this priority level
            runEdmondsKarp(graph, superSource, superSink);

            // Update consumer allocations based on the flow results
            updateConsumerAllocations(graph, priorityConsumers);

            // Update source loads based on current flows
            updateSourceLoads(graph, sources);

            // Remove super sink and its connections for this priority level
            for (GraphEdge edge : consumerToSinkEdges) {
                graph.removeEdge(edge.getSource().getId(), edge.getTarget().getId());
            }
            graph.removeNode(superSink.getId());
        }

        // Clean up: remove super source
        graph.removeNode(superSource.getId());
    }

    /**
     * Runs the Edmonds-Karp algorithm between the given super source and super sink.
     *
     * @param graph The energy network graph
     * @param superSource The super source node
     * @param superSink The super sink node
     */
    private void runEdmondsKarp(Graph graph, SuperSource superSource, SuperSink superSink) {
        // Map to store parent edges for path reconstruction
        Map<String, GraphEdge> parentEdges = new HashMap<>();

        // Get node references from the graph
        EnergyNode source = graph.getNode(superSource.getId());
        EnergyNode sink = graph.getNode(superSink.getId());

        // Main Edmonds-Karp algorithm loop
        while (graph.BFS(source, sink, parentEdges)) {
            // Find the bottleneck capacity
            double bottleneckCapacity = Double.MAX_VALUE;

            // Calculate bottleneck capacity by tracing back from sink to source
            String currentId = sink.getId();
            while (!currentId.equals(source.getId())) {
                GraphEdge edge = parentEdges.get(currentId);
                bottleneckCapacity = Math.min(bottleneckCapacity, edge.getResidualCapacity());
                currentId = edge.getSource().getId();
            }

            // Update flows along the path
            currentId = sink.getId();
            while (!currentId.equals(source.getId())) {
                GraphEdge edge = parentEdges.get(currentId);

                if (edge.isReverse()) {
                    // For reverse edges, decrease flow in the original edge
                    if (edge.getReverseEdge() != null) {
                        edge.getReverseEdge().setFlow(edge.getReverseEdge().getFlow() - bottleneckCapacity);
                    }
                } else {
                    // For forward edges, increase flow
                    edge.setFlow(edge.getFlow() + bottleneckCapacity);
                }

                currentId = edge.getSource().getId();
            }
        }
    }

    /**
     * Updates consumer allocations based on the current flow in the graph.
     *
     * @param graph The energy network graph with updated flows
     * @param consumers List of consumers to update
     */
    private void updateConsumerAllocations(Graph graph, List<EnergyConsumer> consumers) {
        for (EnergyConsumer consumer : consumers) {
            double newAllocation = 0;

            // Sum up all incoming flows from sources
            for (GraphEdge edge : graph.getIncomingEdges(consumer.getId())) {
                if (edge.getSource().getNodeType() == NodeType.SOURCE) {
                    newAllocation += edge.getFlow();
                }
            }

            // Add to any existing allocation
            double totalAllocation = consumer.getAllocatedEnergy() + newAllocation;
            consumer.setAllocatedEnergy(totalAllocation);
        }
    }

    /**
     * Updates source loads based on the current flow in the graph.
     *
     * @param graph The energy network graph with updated flows
     * @param sources List of sources to update
     */
    private void updateSourceLoads(Graph graph, List<EnergySource> sources) {
        for (EnergySource source : sources) {
            double totalLoad = 0;

            // Sum up all outgoing flows to consumers
            for (GraphEdge edge : graph.getOutgoingEdges(source.getId())) {
                if (edge.getTarget().getNodeType() == NodeType.CONSUMER) {
                    totalLoad += edge.getFlow();
                }
            }

            source.setCurrentLoad(totalLoad);
        }
    }
}