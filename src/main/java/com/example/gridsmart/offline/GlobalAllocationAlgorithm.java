package com.example.gridsmart.offline;

import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.graph.GraphEdge;
import com.example.gridsmart.model.*;
import com.example.gridsmart.util.EnergyConsumerQueue;
import java.util.*;

/**
 * global energy allocation algorithm.
 * Allocates energy to consumers in order of their priority levels.
 * This version only adds the relevant consumers to the graph for each priority level.
 */
public class GlobalAllocationAlgorithm
{
    // Storage for allocations from each source to each consumer
    private Map<String, Map<String, Double>> allocationStore = new HashMap<>();

    // Energy allocation manager to store and manage allocations
    private EnergyAllocationManager allocationManager;

    public GlobalAllocationAlgorithm() {
        this.allocationManager = new EnergyAllocationManager();
    }

    public GlobalAllocationAlgorithm(EnergyAllocationManager allocationManager) {
        this.allocationManager = allocationManager;
    }

    public Map<String, Map<String, Double>> getAllocationStore() {
        return allocationStore;
    }

    public EnergyAllocationManager getAllocationManager() {
        return allocationManager;
    }

    /**
     * Runs the prioritized allocation algorithm on the given graph.
     * graph - energy network graph
     * consumers - List of energy consumers
     * sources - List of energy sources
     */
    public void run(Graph graph, List<EnergyConsumer> consumers, List<EnergySource> sources) {
        // Initialize allocation storage
        allocationStore.clear();

        // Clear existing allocations in the manager
        for (EnergyConsumer consumer : consumers) {
            allocationManager.removeAllocationsForConsumer(consumer);
        }

        // Reset all allocations and loads to zero at the start
        for (EnergyConsumer consumer : consumers) {
            consumer.setAllocatedEnergy(0);
        }

        for (EnergySource source : sources) {
            source.setCurrentLoad(0);
        }

        // Add sources to the graph
        for (EnergySource source : sources) {
            if (graph.getNode(source.getId()) == null) {
                graph.addNode(source);
            }
        }

        // Clear any existing consumers from the graph
        // This ensures we start fresh and only add the ones we're processing
        for (EnergyConsumer consumer : consumers) {
            if (graph.getNode(consumer.getId()) != null) {
                graph.removeNode(consumer.getId());
            }
        }

        Map<Integer, List<EnergyConsumer>> priorityGroups = groupConsumersByPriority(consumers);
        SuperSource superSource = graph.addSuperSource("super_source");

        // create a list of all the priority levels in the graph
        List<Integer> priorityLevels = new ArrayList<>(priorityGroups.keySet());
        Collections.sort(priorityLevels);  // Lower numbers = higher priority

        for (int priorityLevel : priorityLevels) {
            List<EnergyConsumer> priorityConsumers = priorityGroups.get(priorityLevel);
            processPriorityLevel(graph, superSource, priorityLevel, priorityConsumers, sources);
        }

        graph.removeNode(superSource.getId());

        // After all priority levels are processed, update the EnergyAllocationManager with our allocations
        createDetailedAllocations(consumers, sources);

        // Build the energy allocation manager's graph from the allocations we've created
        allocationManager.buildGraphFromAllocations();
    }

    /**
     * Creates detailed allocations from stored allocation data
     * This uses the EnergyAllocationManager to store allocations
     */
    private void createDetailedAllocations(List<EnergyConsumer> consumers, List<EnergySource> sources) {
        System.out.println("\n===== Creating Detailed Allocations =====");

        // First ensure the allocation manager has a clean slate
        for (EnergyConsumer consumer : consumers) {
            allocationManager.removeAllocationsForConsumer(consumer);
        }

        // Create a map for quick lookup of sources by ID
        Map<String, EnergySource> sourceMap = new HashMap<>();
        for (EnergySource source : sources) {
            sourceMap.put(source.getId(), source);
        }

        // For each consumer, create allocations from each source
        for (EnergyConsumer consumer : consumers) {
            String consumerId = consumer.getId();

            if (allocationStore.containsKey(consumerId)) {
                Map<String, Double> sourceAllocations = allocationStore.get(consumerId);

                System.out.println("Consumer " + consumerId + " (Priority " + consumer.getPriority() +
                        ", Demand " + consumer.getDemand() + "):");

                if (sourceAllocations.isEmpty()) {
                    System.out.println("  No allocations");
                } else {
                    for (Map.Entry<String, Double> entry : sourceAllocations.entrySet()) {
                        String sourceId = entry.getKey();
                        double allocation = entry.getValue();

                        EnergySource source = sourceMap.get(sourceId);
                        if (source != null) {
                            System.out.println("  From " + sourceId + ": " + allocation);

                            // Add this allocation to the allocation manager
                            // Don't update consumer/source states - they're already set
                            allocationManager.addAllocation(consumer, source, allocation);
                        }
                    }
                }
            } else {
                System.out.println("Consumer " + consumerId + " (Priority " + consumer.getPriority() +
                        ", Demand " + consumer.getDemand() + "):");
                System.out.println("  No allocations");
            }
        }
    }

    // groups consumers by priority
    // returns a Map of priority levels to lists of consumers
    private Map<Integer, List<EnergyConsumer>> groupConsumersByPriority(List<EnergyConsumer> consumers) {
        // create an EnergyConsumerQueue
        EnergyConsumerQueue consumerQueue = new EnergyConsumerQueue();

        // Add all the consumers to the queue
        for (EnergyConsumer consumer : consumers) {
            consumerQueue.add(consumer);
        }

        Map<Integer, List<EnergyConsumer>> priorityGroups = new HashMap<>();

        // group consumers by priority
        while (!consumerQueue.isEmpty()) {
            EnergyConsumer consumer = consumerQueue.poll();
            int priority = consumer.getPriority();

            priorityGroups.computeIfAbsent(priority, k -> new ArrayList<>()).add(consumer);
        }

        return priorityGroups;
    }

    // handle each priority level's allocation
    private void processPriorityLevel(Graph graph, SuperSource superSource, int priorityLevel,
                                      List<EnergyConsumer> priorityConsumers, List<EnergySource> sources) {
        System.out.println("\n\n===== Processing Priority Level " + priorityLevel + " =====");

        // Track current source loads before this priority level starts
        Map<String, Double> previousSourceLoads = new HashMap<>();
        for (EnergySource source : sources) {
            previousSourceLoads.put(source.getId(), source.getCurrentLoad());
        }

        // 1. Add consumers of this priority level to the graph
        for (EnergyConsumer consumer : priorityConsumers) {
            if (consumer.getRemainingDemand() > 0) {
                // Only add consumers that still have demand
                graph.addNode(consumer);

                // Connect sources to consumers with available capacity
                for (EnergySource source : sources) {
                    // Available energy = capacity - current load
                    double sourceCapacity = source.getCapacity() - source.getCurrentLoad();
                    if (sourceCapacity > 0) {
                        graph.addEdge(source.getId(), consumer.getId(), sourceCapacity);
                    }
                }
            }
        }

        // 2. Add a super sink for this priority level
        SuperSink superSink = new SuperSink("super_sink_p" + priorityLevel);
        graph.addNode(superSink);

        // 3. Connect the consumers to the super sink
        List<GraphEdge> consumerToSinkEdges = new ArrayList<>();
        for (EnergyConsumer consumer : priorityConsumers) {
            if (consumer.getRemainingDemand() > 0 && graph.getNode(consumer.getId()) != null) {
                double demandCapacity = consumer.getRemainingDemand();
                GraphEdge edge = graph.addEdge(consumer.getId(), superSink.getId(), demandCapacity);
                consumerToSinkEdges.add(edge);
            }
        }

        // 4. Update the super source edges to connect to all sources with their available capacity
        for (EnergySource source : sources) {
            // Remove any existing edge from super source to this source
            if (graph.getEdge(superSource.getId(), source.getId()) != null) {
                graph.removeEdge(superSource.getId(), source.getId());
            }

            // Add new edge with current available capacity
            double availableEnergy = source.getCapacity() - source.getCurrentLoad();
            if (availableEnergy > 0) {
                graph.addEdge(superSource.getId(), source.getId(), availableEnergy);
            }
        }

        // 5. Print the graph state before running Edmonds-Karp
        System.out.println("Graph before running Edmonds-Karp for priority level " + priorityLevel + ":");
        printGraph(graph, "Before Edmonds-Karp");

        // 6. Run the Edmonds-Karp algorithm for this priority level
        runEdmondsKarp(graph, superSource, superSink);

        // 7. Calculate and store the new allocations for this priority level
        // Create a map to store the allocations for this priority level
        Map<String, Map<String, Double>> newAllocations = new HashMap<>();

        for (EnergyConsumer consumer : priorityConsumers) {
            if (graph.getNode(consumer.getId()) != null) {
                String consumerId = consumer.getId();
                newAllocations.put(consumerId, new HashMap<>());

                // Sum up all incoming flows from sources
                for (GraphEdge edge : graph.getIncomingEdges(consumer.getId())) {
                    if (edge.getSource().getNodeType() == NodeType.SOURCE) {
                        String sourceId = edge.getSource().getId();
                        double flow = edge.getFlow();
                        if (flow > 0) {
                            newAllocations.get(consumerId).put(sourceId, flow);
                        }
                    }
                }
            }
        }

        // 8. Update consumer allocations based on the calculated flows
        for (EnergyConsumer consumer : priorityConsumers) {
            String consumerId = consumer.getId();
            if (newAllocations.containsKey(consumerId)) {
                double totalFlow = 0;
                for (double flow : newAllocations.get(consumerId).values()) {
                    totalFlow += flow;
                }

                if (totalFlow > 0) {
                    // This updates the consumer's allocated energy
                    consumer.setAllocatedEnergy(totalFlow);
                    System.out.println("Consumer " + consumerId + " allocated: " + totalFlow +
                            " (demand: " + consumer.getDemand() + ")");
                }
            }
        }

        // 9. Update source loads based on the new allocations
        for (EnergySource source : sources) {
            String sourceId = source.getId();
            double previousLoad = previousSourceLoads.get(sourceId);
            double additionalLoad = 0;

            // Calculate total flow from this source to all consumers in this priority level
            for (Map<String, Double> sourceAllocations : newAllocations.values()) {
                if (sourceAllocations.containsKey(sourceId)) {
                    additionalLoad += sourceAllocations.get(sourceId);
                }
            }

            // Update the source load
            if (additionalLoad > 0) {
                // Add the new load for this priority level to the previous load
                double newTotalLoad = previousLoad + additionalLoad;
                source.setCurrentLoad(newTotalLoad);
                System.out.println("Source " + sourceId + " load updated from " +
                        previousLoad + " to " + newTotalLoad +
                        " (capacity: " + source.getCapacity() + ")");
            }
        }

        // 10. Store the allocations for later retrieval
        storeAllocations(newAllocations, priorityConsumers, sources);

        // 11. Print the graph state after allocation
        System.out.println("Graph after allocation for priority level " + priorityLevel + ":");
        printGraph(graph, "After Allocation");

        // 12. Clean up: Remove all consumer nodes and edges for this priority level
        // First remove edges to super sink
        for (GraphEdge edge : consumerToSinkEdges) {
            graph.removeEdge(edge.getSource().getId(), edge.getTarget().getId());
        }

        // Then remove consumer nodes
        for (EnergyConsumer consumer : priorityConsumers) {
            if (graph.getNode(consumer.getId()) != null) {
                // This will also remove all connected edges
                graph.removeNode(consumer.getId());
            }
        }

        // Finally remove the super sink
        graph.removeNode(superSink.getId());

        // 13. Print the graph state after cleanup
        System.out.println("Graph after cleanup for priority level " + priorityLevel + ":");
        printGraph(graph, "After Cleanup");
    }

    // Store allocations for later retrieval using the EnergyAllocationManager
    private void storeAllocations(Map<String, Map<String, Double>> allocations,
                                  List<EnergyConsumer> consumers,
                                  List<EnergySource> sources) {
        // Map of consumer ID to source ID to allocation
        for (EnergyConsumer consumer : consumers) {
            String consumerId = consumer.getId();
            if (allocations.containsKey(consumerId)) {
                Map<String, Double> sourceAllocations = allocations.get(consumerId);

                for (Map.Entry<String, Double> entry : sourceAllocations.entrySet()) {
                    String sourceId = entry.getKey();
                    double allocation = entry.getValue();

                    EnergySource source = null;
                    for (EnergySource s : sources) {
                        if (s.getId().equals(sourceId)) {
                            source = s;
                            break;
                        }
                    }

                    if (source != null) {
                        System.out.println("Stored allocation: Consumer " + consumerId +
                                " receives " + allocation + " from Source " + sourceId);

                        // Store in our allocation store for backward compatibility
                        if (!allocationStore.containsKey(consumerId)) {
                            allocationStore.put(consumerId, new HashMap<>());
                        }

                        // Add this allocation
                        if (allocationStore.get(consumerId).containsKey(sourceId)) {
                            // Add to existing allocation
                            double existingAllocation = allocationStore.get(consumerId).get(sourceId);
                            allocationStore.get(consumerId).put(sourceId, existingAllocation + allocation);
                        } else {
                            // New allocation
                            allocationStore.get(consumerId).put(sourceId, allocation);
                        }

                        // Note: We don't add allocations here because we'll do a complete
                        // update from the graph at the end of the run method
                    }
                }
            }
        }
    }

    /*
     * Runs the Edmonds-Karp algorithm between the given super source and super sink.
     * graph - The energy network graph
     * superSource - The super source node
     * superSink - The super sink node
     */
    private void runEdmondsKarp(Graph graph, SuperSource superSource, SuperSink superSink) {
        // Map to store parent edges for path reconstruction
        Map<String, GraphEdge> parentEdges = new HashMap<>();

        // Get the source and sink nodes
        EnergyNode source = graph.getNode(superSource.getId());
        EnergyNode sink = graph.getNode(superSink.getId());

        int iteration = 0;

        // Main Edmonds-Karp algorithm loop
        while (graph.BFS(source, sink, parentEdges))  // the current path will be in parentEdges
        {
            iteration++;
            System.out.println("\n----- Edmonds-Karp Iteration " + iteration + " -----");
            displayAugmentingPath(graph, source, sink, parentEdges);

            // default bottleneck capacity is the maximum value
            double bottleneckCapacity = Double.MAX_VALUE;

            // calculate bottleneck capacity by tracing back from sink to source
            String currentId = sink.getId();
            while (!currentId.equals(source.getId())) {
                GraphEdge edge = parentEdges.get(currentId);
                bottleneckCapacity = Math.min(bottleneckCapacity, edge.getResidualCapacity());
                currentId = edge.getSource().getId();
            }

            // Update flows along the path
            currentId = sink.getId();
            int i=0;
            while (!currentId.equals(source.getId())) {
                GraphEdge edge = parentEdges.get(currentId);

                if (edge.isReverse()) {
                    // For reverse edges, decrease flow in the original edge
                    if (edge.getReverseEdge() != null) {
                        double oldFlow = edge.getReverseEdge().getFlow();
                        edge.getReverseEdge().setFlow(oldFlow - bottleneckCapacity);

                        // Update source/consumer if this is a direct source-consumer edge
                        updateSourceConsumerOnFlowChange(graph, edge.getTarget(), edge.getSource(), -bottleneckCapacity);
                    }
                } else {
                    // For forward edges, increase flow
                    double oldFlow = edge.getFlow();
                    edge.setFlow(oldFlow + bottleneckCapacity);

                    // Update source/consumer if this is a direct source-consumer edge
                    updateSourceConsumerOnFlowChange(graph, edge.getSource(), edge.getTarget(), bottleneckCapacity);
                }

                currentId = edge.getSource().getId();
                i++;
            }
            printGraph(graph, "Graph after iteration " + iteration + " amount of edges in path: " + i);
        }
    }

    /**
     * Updates source loads and consumer allocations when edge flows change.
     *
     * @param graph The graph containing the nodes
     * @param sourceNode The source node of the edge
     * @param targetNode The target node of the edge
     * @param flowChange The amount by which the flow has changed
     */
    private void updateSourceConsumerOnFlowChange(Graph graph, EnergyNode sourceNode, EnergyNode targetNode, double flowChange) {
        // Check if this is a source-to-consumer edge
        if (sourceNode.getNodeType() == NodeType.SOURCE && targetNode.getNodeType() == NodeType.CONSUMER) {
            if (sourceNode instanceof EnergySource && targetNode instanceof EnergyConsumer) {
                EnergySource source = (EnergySource) sourceNode;
                EnergyConsumer consumer = (EnergyConsumer) targetNode;

                // Update source load
                double currentLoad = source.getCurrentLoad();
                source.setCurrentLoad(currentLoad + flowChange);

                // Update consumer allocation
                double currentAllocation = consumer.getAllocatedEnergy();
                consumer.setAllocatedEnergy(currentAllocation + flowChange);

                System.out.println("  Updated: Source " + source.getId() +
                        " load changed from " + currentLoad + " to " + source.getCurrentLoad());
                System.out.println("  Updated: Consumer " + consumer.getId() +
                        " allocation changed from " + currentAllocation + " to " + consumer.getAllocatedEnergy());
            }
        }
        // Handle super source to source edges
        else if (sourceNode.getNodeType() == NodeType.SUPER_SOURCE && targetNode.getNodeType() == NodeType.SOURCE) {
            // Nothing to update here, super source doesn't have load
        }
        // Handle consumer to super sink edges
        else if (sourceNode.getNodeType() == NodeType.CONSUMER && targetNode.getNodeType() == NodeType.SUPER_SINK) {
            // Nothing to update here, super sink doesn't have allocation
        }
    }

    /*
     * Updates consumer allocations based on the current flow in the graph.
     * This method is now used just for display purposes.
     * The actual allocation is handled in processPriorityLevel.
     *
     * graph - The energy network graph with updated flows
     * consumers - List of consumers to update
     */
    private void updateConsumerAllocations(Graph graph, List<EnergyConsumer> consumers) {
        // We're now handling consumer allocations directly in processPriorityLevel
        // to keep track of allocations from specific sources.
        // This method is kept for compatibility but does less work.

        System.out.println("Consumer allocation summary:");
        for (EnergyConsumer consumer : consumers) {
            if (graph.getNode(consumer.getId()) != null) {
                System.out.println("Consumer " + consumer.getId() +
                        " current allocation: " + consumer.getAllocatedEnergy() +
                        " / demand: " + consumer.getDemand());
            }
        }
    }

    /*
     * Updates source loads based on the current flow in the graph.
     * graph - The energy network graph with updated flows
     * sources - List of sources to update
     */
    private void updateSourceLoads(Graph graph, List<EnergySource> sources) {
        for (EnergySource source : sources) {
            // Reset the load before recalculating
            double previousLoad = source.getCurrentLoad();
            double totalLoad = 0;

            // Sum up all outgoing flows to consumers that are still in the graph
            for (GraphEdge edge : graph.getOutgoingEdges(source.getId())) {
                if (edge.getTarget().getNodeType() == NodeType.CONSUMER) {
                    totalLoad += edge.getFlow();
                }
            }

            // Update the source load
            source.setCurrentLoad(totalLoad);
            System.out.println("Source " + source.getId() + " load updated from " +
                    previousLoad + " to " + totalLoad +
                    " (capacity: " + source.getCapacity() + ")");
        }
    }

    /**
     * Displays the complete augmenting path found by BFS and calculates the bottleneck capacity.
     * This is useful for debugging the Edmonds-Karp algorithm.
     *
     * graph - The energy network graph
     * source - Source node (super source)
     * sink - Sink node (super sink)
     * parentEdges - Map of parent edges from BFS
     * return The bottleneck capacity of the path
     */
    private void displayAugmentingPath(Graph graph, EnergyNode source, EnergyNode sink, Map<String, GraphEdge> parentEdges) {
        System.out.println("\n===== Augmenting Path Details =====");

        // Find the bottleneck capacity and collect path nodes
        double bottleneckCapacity = Double.MAX_VALUE;
        List<String> pathNodes = new ArrayList<>();
        List<Double> pathCapacities = new ArrayList<>();

        // Start from sink and trace back to source
        String currentId = sink.getId();
        pathNodes.add(0, currentId);

        while (!currentId.equals(source.getId())) {
            GraphEdge edge = parentEdges.get(currentId);
            if (edge == null) {
                System.out.println("ERROR: Path is incomplete. Missing parent for node: " + currentId);
                return;
            }

            double residualCapacity = edge.getResidualCapacity();
            bottleneckCapacity = Math.min(bottleneckCapacity, residualCapacity);
            pathCapacities.add(0, residualCapacity);

            // Move to parent node
            currentId = edge.getSource().getId();
            pathNodes.add(0, currentId);
        }

        // Print the complete path with capacities
        System.out.println("Path: ");
        for (int i = 0; i < pathNodes.size() - 1; i++) {
            String fromNode = pathNodes.get(i);
            String toNode = pathNodes.get(i + 1);
            GraphEdge edge = graph.getEdge(fromNode, toNode);

            String edgeType = edge.isReverse() ? "reverse" : "forward";
            System.out.printf("  %s -> %s (%s edge, capacity: %.2f, flow: %.2f, residual: %.2f)%n",
                    fromNode, toNode, edgeType, edge.getCapacity(), edge.getFlow(), pathCapacities.get(i));
        }

        System.out.println("Bottleneck capacity: " + bottleneckCapacity);
    }

    /**
     * Prints a detailed representation of the graph with all edges and their properties.
     * This is useful for debugging and visualizing the current state of the network.
     *
     * @param graph The graph to print
     * @param title Optional title for the graph printout
     */
    public static void printGraph(Graph graph, String title) {
        System.out.println("\n========== " + title + " ==========");

        // Get all nodes in the graph
        Collection<EnergyNode> allNodes = graph.getAllNodes();
        System.out.println("Graph contains " + allNodes.size() + " nodes");

        // Sort nodes by type and ID for more organized output
        List<EnergyNode> sortedNodes = new ArrayList<>(allNodes);
        sortedNodes.sort((a, b) -> {
            if (a.getNodeType() != b.getNodeType()) {
                return a.getNodeType().compareTo(b.getNodeType());
            }
            return a.getId().compareTo(b.getId());
        });

        // Print all edges
        System.out.println("\nEdges:");
        System.out.println("----------------------------------------------");
        System.out.printf("%-15s %-15s %10s %10s%n", "Source", "Target", "Flow", "Capacity");
        System.out.println("----------------------------------------------");

        int edgeCount = 0;
        double totalFlow = 0;

        // For each node
        for (EnergyNode node : sortedNodes) {
            List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());

            // Skip nodes with no outgoing edges
            if (edges.isEmpty()) {
                continue;
            }

            // Sort edges by target ID
            edges.sort((a, b) -> a.getTarget().getId().compareTo(b.getTarget().getId()));

            // Print each edge
            for (GraphEdge edge : edges) {
                System.out.printf("%-15s %-15s %10.2f %10.2f%n",
                        edge.getSource().getId(),
                        edge.getTarget().getId(),
                        edge.getFlow(),
                        edge.getCapacity());

                edgeCount++;
                totalFlow += edge.getFlow();
            }
        }

        System.out.println("----------------------------------------------");
        System.out.println("Total: " + edgeCount + " edges, " + totalFlow + " total flow");

        // Summary section for sources and consumers
        printNodeSummary(graph);
    }

    /**
     * Prints a summary of sources and consumers in the graph.
     *
     * @param graph The graph to summarize
     */
    private static void printNodeSummary(Graph graph) {
        System.out.println("\nSources Summary:");
        System.out.println("----------------------------------------------");
        System.out.printf("%-15s %15s %15s%n", "Source", "Load", "Capacity");
        System.out.println("----------------------------------------------");

        // Print sources
        List<EnergyNode> sources = graph.getNodesByType(NodeType.SOURCE);
        sources.sort((a, b) -> a.getId().compareTo(b.getId()));

        for (EnergyNode node : sources) {
            if (node instanceof EnergySource) {
                EnergySource source = (EnergySource) node;
                System.out.printf("%-15s %15.2f %15.2f%n",
                        source.getId(),
                        source.getCurrentLoad(),
                        source.getCapacity());
            }
        }

        System.out.println("\nConsumers Summary:");
        System.out.println("----------------------------------------------");
        System.out.printf("%-15s %15s %15s %10s%n", "Consumer", "Allocated", "Demand", "Priority");
        System.out.println("----------------------------------------------");

        // Print consumers
        List<EnergyNode> consumers = graph.getNodesByType(NodeType.CONSUMER);
        consumers.sort((a, b) -> {
            if (a instanceof EnergyConsumer && b instanceof EnergyConsumer) {
                EnergyConsumer c1 = (EnergyConsumer) a;
                EnergyConsumer c2 = (EnergyConsumer) b;
                if (c1.getPriority() != c2.getPriority()) {
                    return Integer.compare(c1.getPriority(), c2.getPriority());
                }
            }
            return a.getId().compareTo(b.getId());
        });

        for (EnergyNode node : consumers) {
            if (node instanceof EnergyConsumer) {
                EnergyConsumer consumer = (EnergyConsumer) node;
                System.out.printf("%-15s %15.2f %15.2f %10d%n",
                        consumer.getId(),
                        consumer.getAllocatedEnergy(),
                        consumer.getDemand(),
                        consumer.getPriority());
            }
        }

        System.out.println("======================================");
    }
}
//package com.example.gridsmart.offline;
//
//import com.example.gridsmart.graph.Graph;
//import com.example.gridsmart.graph.GraphEdge;
//import com.example.gridsmart.model.*;
//import com.example.gridsmart.util.EnergyConsumerQueue;
//import java.util.*;
//
///**
// * global energy allocation algorithm.
// * Allocates energy to consumers in order of their priority levels.
// * This version only adds the relevant consumers to the graph for each priority level.
// */
//public class GlobalAllocationAlgorithm
//{
//    // Storage for allocations from each source to each consumer
//    private Map<String, Map<String, Double>> allocationStore = new HashMap<>();
//
//    public Map<String, Map<String, Double>> getAllocationStore() {
//        return allocationStore;
//    }
//
//    /**
//     * Runs the prioritized allocation algorithm on the given graph.
//     * graph - energy network graph
//     * consumers - List of energy consumers
//     * sources - List of energy sources
//     */
//    public void run(Graph graph, List<EnergyConsumer> consumers, List<EnergySource> sources) {
//        // Initialize allocation storage
//        allocationStore.clear();
//
//        // Reset all allocations and loads to zero at the start
//        for (EnergyConsumer consumer : consumers) {
//            consumer.setAllocatedEnergy(0);
//        }
//
//        for (EnergySource source : sources) {
//            source.setCurrentLoad(0);
//        }
//
//        // Add sources to the graph
//        for (EnergySource source : sources) {
//            if (graph.getNode(source.getId()) == null) {
//                graph.addNode(source);
//            }
//        }
//
//        // Clear any existing consumers from the graph
//        // This ensures we start fresh and only add the ones we're processing
//        for (EnergyConsumer consumer : consumers) {
//            if (graph.getNode(consumer.getId()) != null) {
//                graph.removeNode(consumer.getId());
//            }
//        }
//
//        Map<Integer, List<EnergyConsumer>> priorityGroups = groupConsumersByPriority(consumers);
//        SuperSource superSource = graph.addSuperSource("super_source");
//
//        // create a list of all the priority levels in the graph
//        List<Integer> priorityLevels = new ArrayList<>(priorityGroups.keySet());
//        Collections.sort(priorityLevels);  // Lower numbers = higher priority
//
//        for (int priorityLevel : priorityLevels) {
//            List<EnergyConsumer> priorityConsumers = priorityGroups.get(priorityLevel);
//            processPriorityLevel(graph, superSource, priorityLevel, priorityConsumers, sources);
//        }
//
//        graph.removeNode(superSource.getId());
//
//        // After all priority levels are processed, update the EnergyAllocationManager with our allocations
//        // This would use the proper manager in a real implementation
//        createDetailedAllocations(consumers, sources);
//    }
//
//    /**
//     * Creates detailed allocations from stored allocation data
//     * This would normally use the EnergyAllocationManager
//     */
//    private void createDetailedAllocations(List<EnergyConsumer> consumers, List<EnergySource> sources) {
//        System.out.println("\n===== Creating Detailed Allocations =====");
//
//        // Create a map for quick lookup of sources by ID
//        Map<String, EnergySource> sourceMap = new HashMap<>();
//        for (EnergySource source : sources) {
//            sourceMap.put(source.getId(), source);
//        }
//
//        // For each consumer, create allocations from each source
//        for (EnergyConsumer consumer : consumers) {
//            String consumerId = consumer.getId();
//
//            if (allocationStore.containsKey(consumerId)) {
//                Map<String, Double> sourceAllocations = allocationStore.get(consumerId);
//
//                System.out.println("Consumer " + consumerId + " (Priority " + consumer.getPriority() +
//                        ", Demand " + consumer.getDemand() + "):");
//
//                if (sourceAllocations.isEmpty()) {
//                    System.out.println("  No allocations");
//                } else {
//                    for (Map.Entry<String, Double> entry : sourceAllocations.entrySet()) {
//                        String sourceId = entry.getKey();
//                        double allocation = entry.getValue();
//
//                        EnergySource source = sourceMap.get(sourceId);
//                        if (source != null) {
//                            System.out.println("  From " + sourceId + ": " + allocation);
//
//                            // In a real implementation, you would add this to your allocation manager
//                            // allocationManager.addAllocation(consumer, source, allocation);
//                        }
//                    }
//                }
//            } else {
//                System.out.println("Consumer " + consumerId + " (Priority " + consumer.getPriority() +
//                        ", Demand " + consumer.getDemand() + "):");
//                System.out.println("  No allocations");
//            }
//        }
//    }
//
//    // groups consumers by priority
//    // returns a Map of priority levels to lists of consumers
//    private Map<Integer, List<EnergyConsumer>> groupConsumersByPriority(List<EnergyConsumer> consumers) {
//        // create an EnergyConsumerQueue
//        EnergyConsumerQueue consumerQueue = new EnergyConsumerQueue();
//
//        // Add all the consumers to the queue
//        for (EnergyConsumer consumer : consumers) {
//            consumerQueue.add(consumer);
//        }
//
//        Map<Integer, List<EnergyConsumer>> priorityGroups = new HashMap<>();
//
//        // group consumers by priority
//        while (!consumerQueue.isEmpty()) {
//            EnergyConsumer consumer = consumerQueue.poll();
//            int priority = consumer.getPriority();
//
//            priorityGroups.computeIfAbsent(priority, k -> new ArrayList<>()).add(consumer);
//        }
//
//        return priorityGroups;
//    }
//
//    // handle each priority level's allocation
//    private void processPriorityLevel(Graph graph, SuperSource superSource, int priorityLevel,
//                                      List<EnergyConsumer> priorityConsumers, List<EnergySource> sources) {
//        System.out.println("\n\n===== Processing Priority Level " + priorityLevel + " =====");
//
//        // Track current source loads before this priority level starts
//        Map<String, Double> previousSourceLoads = new HashMap<>();
//        for (EnergySource source : sources) {
//            previousSourceLoads.put(source.getId(), source.getCurrentLoad());
//        }
//
//        // 1. Add consumers of this priority level to the graph
//        for (EnergyConsumer consumer : priorityConsumers) {
//            if (consumer.getRemainingDemand() > 0) {
//                // Only add consumers that still have demand
//                graph.addNode(consumer);
//
//                // Connect sources to consumers with available capacity
//                for (EnergySource source : sources) {
//                    // Available energy = capacity - current load
//                    double sourceCapacity = source.getCapacity() - source.getCurrentLoad();
//                    if (sourceCapacity > 0) {
//                        graph.addEdge(source.getId(), consumer.getId(), sourceCapacity);
//                    }
//                }
//            }
//        }
//
//        // 2. Add a super sink for this priority level
//        SuperSink superSink = new SuperSink("super_sink_p" + priorityLevel);
//        graph.addNode(superSink);
//
//        // 3. Connect the consumers to the super sink
//        List<GraphEdge> consumerToSinkEdges = new ArrayList<>();
//        for (EnergyConsumer consumer : priorityConsumers) {
//            if (consumer.getRemainingDemand() > 0 && graph.getNode(consumer.getId()) != null) {
//                double demandCapacity = consumer.getRemainingDemand();
//                GraphEdge edge = graph.addEdge(consumer.getId(), superSink.getId(), demandCapacity);
//                consumerToSinkEdges.add(edge);
//            }
//        }
//
//        // 4. Update the super source edges to connect to all sources with their available capacity
//        for (EnergySource source : sources) {
//            // Remove any existing edge from super source to this source
//            if (graph.getEdge(superSource.getId(), source.getId()) != null) {
//                graph.removeEdge(superSource.getId(), source.getId());
//            }
//
//            // Add new edge with current available capacity
//            double availableEnergy = source.getCapacity() - source.getCurrentLoad();
//            if (availableEnergy > 0) {
//                graph.addEdge(superSource.getId(), source.getId(), availableEnergy);
//            }
//        }
//
//        // 5. Print the graph state before running Edmonds-Karp
//        System.out.println("Graph before running Edmonds-Karp for priority level " + priorityLevel + ":");
//        printGraph(graph, "Before Edmonds-Karp");
//
//        // 6. Run the Edmonds-Karp algorithm for this priority level
//        runEdmondsKarp(graph, superSource, superSink);
//
//        // 7. Calculate and store the new allocations for this priority level
//        // Create a map to store the allocations for this priority level
//        Map<String, Map<String, Double>> newAllocations = new HashMap<>();
//
//        for (EnergyConsumer consumer : priorityConsumers) {
//            if (graph.getNode(consumer.getId()) != null) {
//                String consumerId = consumer.getId();
//                newAllocations.put(consumerId, new HashMap<>());
//
//                // Sum up all incoming flows from sources
//                for (GraphEdge edge : graph.getIncomingEdges(consumer.getId())) {
//                    if (edge.getSource().getNodeType() == NodeType.SOURCE) {
//                        String sourceId = edge.getSource().getId();
//                        double flow = edge.getFlow();
//                        if (flow > 0) {
//                            newAllocations.get(consumerId).put(sourceId, flow);
//                        }
//                    }
//                }
//            }
//        }
//
//        // 8. Update consumer allocations based on the calculated flows
//        for (EnergyConsumer consumer : priorityConsumers) {
//            String consumerId = consumer.getId();
//            if (newAllocations.containsKey(consumerId)) {
//                double totalFlow = 0;
//                for (double flow : newAllocations.get(consumerId).values()) {
//                    totalFlow += flow;
//                }
//
//                if (totalFlow > 0) {
//                    // This updates the consumer's allocated energy
//                    consumer.setAllocatedEnergy(totalFlow);
//                    System.out.println("Consumer " + consumerId + " allocated: " + totalFlow +
//                            " (demand: " + consumer.getDemand() + ")");
//                }
//            }
//        }
//
//        // 9. Update source loads based on the new allocations
//        for (EnergySource source : sources) {
//            String sourceId = source.getId();
//            double previousLoad = previousSourceLoads.get(sourceId);
//            double additionalLoad = 0;
//
//            // Calculate total flow from this source to all consumers in this priority level
//            for (Map<String, Double> sourceAllocations : newAllocations.values()) {
//                if (sourceAllocations.containsKey(sourceId)) {
//                    additionalLoad += sourceAllocations.get(sourceId);
//                }
//            }
//
//            // Update the source load
//            if (additionalLoad > 0) {
//                // Add the new load for this priority level to the previous load
//                double newTotalLoad = previousLoad + additionalLoad;
//                source.setCurrentLoad(newTotalLoad);
//                System.out.println("Source " + sourceId + " load updated from " +
//                        previousLoad + " to " + newTotalLoad +
//                        " (capacity: " + source.getCapacity() + ")");
//            }
//        }
//
//        // 10. Store the allocations for later retrieval (you'll need to create/use the appropriate manager here)
//        // This would be implemented with the EnergyAllocationManager
//        storeAllocations(newAllocations, priorityConsumers, sources);
//
//        // 11. Print the graph state after allocation
//        System.out.println("Graph after allocation for priority level " + priorityLevel + ":");
//        printGraph(graph, "After Allocation");
//
//        // 12. Clean up: Remove all consumer nodes and edges for this priority level
//        // First remove edges to super sink
//        for (GraphEdge edge : consumerToSinkEdges) {
//            graph.removeEdge(edge.getSource().getId(), edge.getTarget().getId());
//        }
//
//        // Then remove consumer nodes
//        for (EnergyConsumer consumer : priorityConsumers) {
//            if (graph.getNode(consumer.getId()) != null) {
//                // This will also remove all connected edges
//                graph.removeNode(consumer.getId());
//            }
//        }
//
//        // Finally remove the super sink
//        graph.removeNode(superSink.getId());
//
//        // 13. Print the graph state after cleanup
//        System.out.println("Graph after cleanup for priority level " + priorityLevel + ":");
//        printGraph(graph, "After Cleanup");
//    }
//
//    // Store allocations for later retrieval (this would normally use your EnergyAllocationManager)
//    private void storeAllocations(Map<String, Map<String, Double>> allocations,
//                                  List<EnergyConsumer> consumers,
//                                  List<EnergySource> sources) {
//        // This is a simplified version - in a real implementation, you'd use your allocation manager
//        // Map of consumer ID to source ID to allocation
//        for (EnergyConsumer consumer : consumers) {
//            String consumerId = consumer.getId();
//            if (allocations.containsKey(consumerId)) {
//                Map<String, Double> sourceAllocations = allocations.get(consumerId);
//
//                for (Map.Entry<String, Double> entry : sourceAllocations.entrySet()) {
//                    String sourceId = entry.getKey();
//                    double allocation = entry.getValue();
//
//                    EnergySource source = null;
//                    for (EnergySource s : sources) {
//                        if (s.getId().equals(sourceId)) {
//                            source = s;
//                            break;
//                        }
//                    }
//
//                    if (source != null) {
//                        System.out.println("Stored allocation: Consumer " + consumerId +
//                                " receives " + allocation + " from Source " + sourceId);
//
//                        // Store in our allocation store
//                        if (!allocationStore.containsKey(consumerId)) {
//                            allocationStore.put(consumerId, new HashMap<>());
//                        }
//
//                        // Add this allocation
//                        if (allocationStore.get(consumerId).containsKey(sourceId)) {
//                            // Add to existing allocation
//                            double existingAllocation = allocationStore.get(consumerId).get(sourceId);
//                            allocationStore.get(consumerId).put(sourceId, existingAllocation + allocation);
//                        } else {
//                            // New allocation
//                            allocationStore.get(consumerId).put(sourceId, allocation);
//                        }
//
//                        // Here you would normally call your allocation manager to store this allocation
//                        // For example: allocationManager.addAllocation(consumer, source, allocation);
//                    }
//                }
//            }
//        }
//    }
//
//    /*
//     * Runs the Edmonds-Karp algorithm between the given super source and super sink.
//     * graph - The energy network graph
//     * superSource - The super source node
//     * superSink - The super sink node
//     */
//    private void runEdmondsKarp(Graph graph, SuperSource superSource, SuperSink superSink) {
//        // Map to store parent edges for path reconstruction
//        Map<String, GraphEdge> parentEdges = new HashMap<>();
//
//        // Get the source and sink nodes
//        EnergyNode source = graph.getNode(superSource.getId());
//        EnergyNode sink = graph.getNode(superSink.getId());
//
//        int iteration = 0;
//
//        // Main Edmonds-Karp algorithm loop
//        while (graph.BFS(source, sink, parentEdges))  // the current path will be in parentEdges
//        {
//            iteration++;
//            System.out.println("\n----- Edmonds-Karp Iteration " + iteration + " -----");
//            displayAugmentingPath(graph, source, sink, parentEdges);
//
//            // default bottleneck capacity is the maximum value
//            double bottleneckCapacity = Double.MAX_VALUE;
//
//            // calculate bottleneck capacity by tracing back from sink to source
//            String currentId = sink.getId();
//            while (!currentId.equals(source.getId())) {
//                GraphEdge edge = parentEdges.get(currentId);
//                bottleneckCapacity = Math.min(bottleneckCapacity, edge.getResidualCapacity());
//                currentId = edge.getSource().getId();
//            }
//
//            // Update flows along the path
//            currentId = sink.getId();
//            int i=0;
//            while (!currentId.equals(source.getId())) {
//                GraphEdge edge = parentEdges.get(currentId);
//
//                if (edge.isReverse()) {
//                    // For reverse edges, decrease flow in the original edge
//                    if (edge.getReverseEdge() != null) {
//                        double oldFlow = edge.getReverseEdge().getFlow();
//                        edge.getReverseEdge().setFlow(oldFlow - bottleneckCapacity);
//
//                        // Update source/consumer if this is a direct source-consumer edge
//                        updateSourceConsumerOnFlowChange(graph, edge.getTarget(), edge.getSource(), -bottleneckCapacity);
//                    }
//                } else {
//                    // For forward edges, increase flow
//                    double oldFlow = edge.getFlow();
//                    edge.setFlow(oldFlow + bottleneckCapacity);
//
//                    // Update source/consumer if this is a direct source-consumer edge
//                    updateSourceConsumerOnFlowChange(graph, edge.getSource(), edge.getTarget(), bottleneckCapacity);
//                }
//
//                currentId = edge.getSource().getId();
//                i++;
//            }
//            printGraph(graph, "Graph after iteration " + iteration + " amount of edges in path: " + i);
//        }
//    }
//
//    /**
//     * Updates source loads and consumer allocations when edge flows change.
//     *
//     * @param graph The graph containing the nodes
//     * @param sourceNode The source node of the edge
//     * @param targetNode The target node of the edge
//     * @param flowChange The amount by which the flow has changed
//     */
//    private void updateSourceConsumerOnFlowChange(Graph graph, EnergyNode sourceNode, EnergyNode targetNode, double flowChange) {
//        // Check if this is a source-to-consumer edge
//        if (sourceNode.getNodeType() == NodeType.SOURCE && targetNode.getNodeType() == NodeType.CONSUMER) {
//            if (sourceNode instanceof EnergySource && targetNode instanceof EnergyConsumer) {
//                EnergySource source = (EnergySource) sourceNode;
//                EnergyConsumer consumer = (EnergyConsumer) targetNode;
//
//                // Update source load
//                double currentLoad = source.getCurrentLoad();
//                source.setCurrentLoad(currentLoad + flowChange);
//
//                // Update consumer allocation
//                double currentAllocation = consumer.getAllocatedEnergy();
//                consumer.setAllocatedEnergy(currentAllocation + flowChange);
//
//                System.out.println("  Updated: Source " + source.getId() +
//                        " load changed from " + currentLoad + " to " + source.getCurrentLoad());
//                System.out.println("  Updated: Consumer " + consumer.getId() +
//                        " allocation changed from " + currentAllocation + " to " + consumer.getAllocatedEnergy());
//            }
//        }
//        // Handle super source to source edges
//        else if (sourceNode.getNodeType() == NodeType.SUPER_SOURCE && targetNode.getNodeType() == NodeType.SOURCE) {
//            // Nothing to update here, super source doesn't have load
//        }
//        // Handle consumer to super sink edges
//        else if (sourceNode.getNodeType() == NodeType.CONSUMER && targetNode.getNodeType() == NodeType.SUPER_SINK) {
//            // Nothing to update here, super sink doesn't have allocation
//        }
//    }
//
//    /*
//     * Updates consumer allocations based on the current flow in the graph.
//     * This method is now used just for display purposes.
//     * The actual allocation is handled in processPriorityLevel.
//     *
//     * graph - The energy network graph with updated flows
//     * consumers - List of consumers to update
//     */
//    private void updateConsumerAllocations(Graph graph, List<EnergyConsumer> consumers) {
//        // We're now handling consumer allocations directly in processPriorityLevel
//        // to keep track of allocations from specific sources.
//        // This method is kept for compatibility but does less work.
//
//        System.out.println("Consumer allocation summary:");
//        for (EnergyConsumer consumer : consumers) {
//            if (graph.getNode(consumer.getId()) != null) {
//                System.out.println("Consumer " + consumer.getId() +
//                        " current allocation: " + consumer.getAllocatedEnergy() +
//                        " / demand: " + consumer.getDemand());
//            }
//        }
//    }
//
//    /*
//     * Updates source loads based on the current flow in the graph.
//     * graph - The energy network graph with updated flows
//     * sources - List of sources to update
//     */
//    private void updateSourceLoads(Graph graph, List<EnergySource> sources) {
//        for (EnergySource source : sources) {
//            // Reset the load before recalculating
//            double previousLoad = source.getCurrentLoad();
//            double totalLoad = 0;
//
//            // Sum up all outgoing flows to consumers that are still in the graph
//            for (GraphEdge edge : graph.getOutgoingEdges(source.getId())) {
//                if (edge.getTarget().getNodeType() == NodeType.CONSUMER) {
//                    totalLoad += edge.getFlow();
//                }
//            }
//
//            // Update the source load
//            source.setCurrentLoad(totalLoad);
//            System.out.println("Source " + source.getId() + " load updated from " +
//                    previousLoad + " to " + totalLoad +
//                    " (capacity: " + source.getCapacity() + ")");
//        }
//    }
//
//    /**
//     * Displays the complete augmenting path found by BFS and calculates the bottleneck capacity.
//     * This is useful for debugging the Edmonds-Karp algorithm.
//     *
//     * graph - The energy network graph
//     * source - Source node (super source)
//     * sink - Sink node (super sink)
//     * parentEdges - Map of parent edges from BFS
//     * return The bottleneck capacity of the path
//     */
//    private void displayAugmentingPath(Graph graph, EnergyNode source, EnergyNode sink, Map<String, GraphEdge> parentEdges) {
//        System.out.println("\n===== Augmenting Path Details =====");
//
//        // Find the bottleneck capacity and collect path nodes
//        double bottleneckCapacity = Double.MAX_VALUE;
//        List<String> pathNodes = new ArrayList<>();
//        List<Double> pathCapacities = new ArrayList<>();
//
//        // Start from sink and trace back to source
//        String currentId = sink.getId();
//        pathNodes.add(0, currentId);
//
//        while (!currentId.equals(source.getId())) {
//            GraphEdge edge = parentEdges.get(currentId);
//            if (edge == null) {
//                System.out.println("ERROR: Path is incomplete. Missing parent for node: " + currentId);
//                return;
//            }
//
//            double residualCapacity = edge.getResidualCapacity();
//            bottleneckCapacity = Math.min(bottleneckCapacity, residualCapacity);
//            pathCapacities.add(0, residualCapacity);
//
//            // Move to parent node
//            currentId = edge.getSource().getId();
//            pathNodes.add(0, currentId);
//        }
//
//        // Print the complete path with capacities
//        System.out.println("Path: ");
//        for (int i = 0; i < pathNodes.size() - 1; i++) {
//            String fromNode = pathNodes.get(i);
//            String toNode = pathNodes.get(i + 1);
//            GraphEdge edge = graph.getEdge(fromNode, toNode);
//
//            String edgeType = edge.isReverse() ? "reverse" : "forward";
//            System.out.printf("  %s -> %s (%s edge, capacity: %.2f, flow: %.2f, residual: %.2f)%n",
//                    fromNode, toNode, edgeType, edge.getCapacity(), edge.getFlow(), pathCapacities.get(i));
//        }
//
//        System.out.println("Bottleneck capacity: " + bottleneckCapacity);
//    }
//
//    /**
//     * Prints a detailed representation of the graph with all edges and their properties.
//     * This is useful for debugging and visualizing the current state of the network.
//     *
//     * @param graph The graph to print
//     * @param title Optional title for the graph printout
//     */
//    public static void printGraph(Graph graph, String title) {
//        System.out.println("\n========== " + title + " ==========");
//
//        // Get all nodes in the graph
//        Collection<EnergyNode> allNodes = graph.getAllNodes();
//        System.out.println("Graph contains " + allNodes.size() + " nodes");
//
//        // Sort nodes by type and ID for more organized output
//        List<EnergyNode> sortedNodes = new ArrayList<>(allNodes);
//        sortedNodes.sort((a, b) -> {
//            if (a.getNodeType() != b.getNodeType()) {
//                return a.getNodeType().compareTo(b.getNodeType());
//            }
//            return a.getId().compareTo(b.getId());
//        });
//
//        // Print all edges
//        System.out.println("\nEdges:");
//        System.out.println("----------------------------------------------");
//        System.out.printf("%-15s %-15s %10s %10s%n", "Source", "Target", "Flow", "Capacity");
//        System.out.println("----------------------------------------------");
//
//        int edgeCount = 0;
//        double totalFlow = 0;
//
//        // For each node
//        for (EnergyNode node : sortedNodes) {
//            List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());
//
//            // Skip nodes with no outgoing edges
//            if (edges.isEmpty()) {
//                continue;
//            }
//
//            // Sort edges by target ID
//            edges.sort((a, b) -> a.getTarget().getId().compareTo(b.getTarget().getId()));
//
//            // Print each edge
//            for (GraphEdge edge : edges) {
//                System.out.printf("%-15s %-15s %10.2f %10.2f%n",
//                        edge.getSource().getId(),
//                        edge.getTarget().getId(),
//                        edge.getFlow(),
//                        edge.getCapacity());
//
//                edgeCount++;
//                totalFlow += edge.getFlow();
//            }
//        }
//
//        System.out.println("----------------------------------------------");
//        System.out.println("Total: " + edgeCount + " edges, " + totalFlow + " total flow");
//
//        // Summary section for sources and consumers
//        printNodeSummary(graph);
//    }
//
//    /**
//     * Prints a summary of sources and consumers in the graph.
//     *
//     * @param graph The graph to summarize
//     */
//    private static void printNodeSummary(Graph graph) {
//        System.out.println("\nSources Summary:");
//        System.out.println("----------------------------------------------");
//        System.out.printf("%-15s %15s %15s%n", "Source", "Load", "Capacity");
//        System.out.println("----------------------------------------------");
//
//        // Print sources
//        List<EnergyNode> sources = graph.getNodesByType(NodeType.SOURCE);
//        sources.sort((a, b) -> a.getId().compareTo(b.getId()));
//
//        for (EnergyNode node : sources) {
//            if (node instanceof EnergySource) {
//                EnergySource source = (EnergySource) node;
//                System.out.printf("%-15s %15.2f %15.2f%n",
//                        source.getId(),
//                        source.getCurrentLoad(),
//                        source.getCapacity());
//            }
//        }
//
//        System.out.println("\nConsumers Summary:");
//        System.out.println("----------------------------------------------");
//        System.out.printf("%-15s %15s %15s %10s%n", "Consumer", "Allocated", "Demand", "Priority");
//        System.out.println("----------------------------------------------");
//
//        // Print consumers
//        List<EnergyNode> consumers = graph.getNodesByType(NodeType.CONSUMER);
//        consumers.sort((a, b) -> {
//            if (a instanceof EnergyConsumer && b instanceof EnergyConsumer) {
//                EnergyConsumer c1 = (EnergyConsumer) a;
//                EnergyConsumer c2 = (EnergyConsumer) b;
//                if (c1.getPriority() != c2.getPriority()) {
//                    return Integer.compare(c1.getPriority(), c2.getPriority());
//                }
//            }
//            return a.getId().compareTo(b.getId());
//        });
//
//        for (EnergyNode node : consumers) {
//            if (node instanceof EnergyConsumer) {
//                EnergyConsumer consumer = (EnergyConsumer) node;
//                System.out.printf("%-15s %15.2f %15.2f %10d%n",
//                        consumer.getId(),
//                        consumer.getAllocatedEnergy(),
//                        consumer.getDemand(),
//                        consumer.getPriority());
//            }
//        }
//
//        System.out.println("======================================");
//    }
//}