package com.example.gridsmart.graph;

import com.example.gridsmart.model.*;
import com.example.gridsmart.util.EnergyConsumerQueue;
import java.util.*;

/*
 * global energy allocation algorithm.
 * Allocates energy to consumers in order of their priority levels.
 */
public class GlobalAllocationAlgorithm
{
    /*
     * Runs the prioritized allocation algorithm on the given graph.
     * Consumers with higher priority (lower priority number) are served first.
     *
     * graph The energy network graph
     * consumers List of energy consumers
     * sources List of energy sources
     */
    public void run(Graph graph, List<EnergyConsumer> consumers, List<EnergySource> sources) {
        addNodesToGraph(graph, consumers, sources);
        Map<Integer, List<EnergyConsumer>> priorityGroups = groupConsumersByPriority(consumers);
        SuperSource superSource = graph.addSuperSource("super_source");

        List<Integer> priorityLevels = new ArrayList<>(priorityGroups.keySet());
        Collections.sort(priorityLevels);  // Lower numbers = higher priority

        for (int priorityLevel : priorityLevels) {
            List<EnergyConsumer> priorityConsumers = priorityGroups.get(priorityLevel);
            processPriorityLevel(graph, superSource, priorityLevel, priorityConsumers, sources);
        }

        graph.removeNode(superSource.getId());
    }

    // makes sure that the nodes are added to the graph
    // if not, it adds them
    private void addNodesToGraph(Graph graph, List<EnergyConsumer> consumers, List<EnergySource> sources) {
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
    }

    // groups consumers by priority
    // returns a Map of priority levels to lists of consumers
    private Map<Integer, List<EnergyConsumer>> groupConsumersByPriority(List<EnergyConsumer> consumers) {
        EnergyConsumerQueue consumerQueue = new EnergyConsumerQueue();
        for (EnergyConsumer consumer : consumers) {
            consumerQueue.add(consumer);
        }

        Map<Integer, List<EnergyConsumer>> priorityGroups = new HashMap<>();

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
        // add a Sink Node
        SuperSink superSink = new SuperSink("super_sink_p" + priorityLevel);
        graph.addNode(superSink);

        // connect the consumers to the Sink Node
        List<GraphEdge> consumerToSinkEdges = new ArrayList<>();
        for (EnergyConsumer consumer : priorityConsumers) {
            if (consumer.getRemainingDemand() > 0) {
                double demandCapacity = consumer.getRemainingDemand();
                GraphEdge edge = graph.addEdge(consumer.getId(), superSink.getId(), demandCapacity);
                consumerToSinkEdges.add(edge);
            }
        }

        // update the super source edges
        graph.updateSuperSourceEdges();

        // run the Edmonds-Karp algorithm on the current priority level
        runEdmondsKarp(graph, superSource, superSink);


        updateConsumerAllocations(graph, priorityConsumers);
        updateSourceLoads(graph, sources);

        for (GraphEdge edge : consumerToSinkEdges) {
            graph.removeEdge(edge.getSource().getId(), edge.getTarget().getId());
        }
        graph.removeNode(superSink.getId());
    }


    /*
     * Runs the Edmonds-Karp algorithm between the given super source and super sink.
     *
     * graph - The energy network graph
     * superSource - The super source node
     * superSink - The super sink node
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

    /*
     * Updates consumer allocations based on the current flow in the graph.
     * graph - The energy network graph with updated flows
     * consumers - List of consumers to update
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

    /*
     * Updates source loads based on the current flow in the graph.
     * graph - The energy network graph with updated flows
     * sources - List of sources to update
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