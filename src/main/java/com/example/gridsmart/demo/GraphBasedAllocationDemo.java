package com.example.gridsmart.demo;

import com.example.gridsmart.graph.*;
import com.example.gridsmart.model.*;
import java.util.*;

/**
 * Demonstration of the graph-based energy allocation system.
 * Creates a sample network, runs the global allocation algorithm,
 * and displays the results.
 */
public class GraphBasedAllocationDemo {

    public static void main(String[] args) {
        System.out.println("===== Graph-Based Energy Allocation Demo =====\n");

        // Create energy sources
        List<EnergySource> sources = createSources();
        System.out.println("Created energy sources:");
        for (EnergySource source : sources) {
            System.out.println(" - " + source);
        }

        // Create energy consumers with different priorities
        List<EnergyConsumer> consumers = createConsumers();
        System.out.println("\nCreated energy consumers:");
        for (EnergyConsumer consumer : consumers) {
            System.out.println(" - " + consumer);
        }

        // Create a graph and add all nodes
        Graph graph = new Graph();
        addNodesToGraph(graph, sources, consumers);

        // Add connections between sources and consumers
        createConnections(graph, sources, consumers);
        System.out.println("\nInitial graph created with connections.");

        // Run the global allocation algorithm
        System.out.println("\nRunning the global allocation algorithm...");
        GlobalAllocationAlgorithm algorithm = new GlobalAllocationAlgorithm();
        algorithm.run(graph, consumers, sources);

        // Display the results
        System.out.println("\n===== Allocation Results =====");
        printAllocationResults(graph, sources, consumers);

        // Calculate and print statistics
        printAllocationStatistics(sources, consumers);
    }

    /**
     * Creates sample energy sources with different capacities and types.
     *
     * @return List of energy sources
     */
    private static List<EnergySource> createSources() {
        List<EnergySource> sources = new ArrayList<>();

        // Create a mix of energy sources
        sources.add(new EnergySource("solar1", 100.0, SourceType.SOLAR));
        sources.add(new EnergySource("wind1", 80.0, SourceType.WIND));
        sources.add(new EnergySource("hydro1", 120.0, SourceType.HYDRO));
        sources.add(new EnergySource("nuclear1", 200.0, SourceType.NUCLEAR));
        sources.add(new EnergySource("battery1", 50.0, SourceType.BATTERY));

        return sources;
    }

    /**
     * Creates sample energy consumers with different priorities and demands.
     *
     * @return List of energy consumers
     */
    private static List<EnergyConsumer> createConsumers() {
        List<EnergyConsumer> consumers = new ArrayList<>();

        // Priority 1 (highest priority) - Critical infrastructure
        consumers.add(new EnergyConsumer("hospital1", 1, 130.0));
        consumers.add(new EnergyConsumer("emergency1", 1, 70.0));

        // Priority 2 - Important services
        consumers.add(new EnergyConsumer("datacenter1", 2, 100.0));
        consumers.add(new EnergyConsumer("watertreatment1", 2, 90.0));

        // Priority 3 - Regular commercial
        consumers.add(new EnergyConsumer("mall1", 3, 60.0));
        consumers.add(new EnergyConsumer("factory1", 3, 80.0));

        // Priority 4 (lowest priority) - Residential
        consumers.add(new EnergyConsumer("residential1", 4, 40.0));
        consumers.add(new EnergyConsumer("residential2", 4, 35.0));

        return consumers;
    }

    /**
     * Adds source and consumer nodes to the graph.
     *
     * @param graph The graph to update
     * @param sources List of energy sources
     * @param consumers List of energy consumers
     */
    private static void addNodesToGraph(Graph graph, List<EnergySource> sources, List<EnergyConsumer> consumers) {
        // Add all sources to the graph
        for (EnergySource source : sources) {
            graph.addNode(source);
        }

        // Add all consumers to the graph
        for (EnergyConsumer consumer : consumers) {
            graph.addNode(consumer);
        }
    }

    /**
     * Creates connections between sources and consumers.
     * Not all sources are connected to all consumers to simulate
     * a realistic network with geographical constraints.
     *
     * @param graph The graph to update
     * @param sources List of energy sources
     * @param consumers List of energy consumers
     */
    private static void createConnections(Graph graph, List<EnergySource> sources, List<EnergyConsumer> consumers) {
        // Connect solar1 to hospital1, emergency1, residential1
        connectSourceToConsumers(graph, "solar1", new String[]{"hospital1", "emergency1", "residential1"});

        // Connect wind1 to emergency1, residential1, residential2
        connectSourceToConsumers(graph, "wind1", new String[]{"emergency1", "residential1", "residential2"});

        // Connect hydro1 to hospital1, datacenter1, watertreatment1
        connectSourceToConsumers(graph, "hydro1", new String[]{"hospital1", "datacenter1", "watertreatment1"});

        // Connect nuclear1 to all consumers (it's a baseload plant)
        String[] allConsumerIds = consumers.stream().map(EnergyNode::getId).toArray(String[]::new);
        connectSourceToConsumers(graph, "nuclear1", allConsumerIds);

        // Connect battery1 to hospital1, emergency1 (critical infrastructure)
        connectSourceToConsumers(graph, "battery1", new String[]{"hospital1", "emergency1"});
    }

    /**
     * Helper method to connect a source to multiple consumers.
     *
     * @param graph The graph to update
     * @param sourceId ID of the source
     * @param consumerIds Array of consumer IDs
     */
    private static void connectSourceToConsumers(Graph graph, String sourceId, String[] consumerIds) {
        EnergySource source = (EnergySource) graph.getNode(sourceId);

        if (source != null) {
            double capacity = source.getCapacity();

            for (String consumerId : consumerIds) {
                EnergyNode consumerNode = graph.getNode(consumerId);

                if (consumerNode != null) {
                    // Add edge with the source's capacity
                    graph.addEdge(sourceId, consumerId, capacity);
                }
            }
        }
    }

    /**
     * Prints the allocation results, showing which consumers got what amount
     * of energy from which sources.
     *
     * @param graph The graph with flow results
     * @param sources List of energy sources
     * @param consumers List of energy consumers
     */
    private static void printAllocationResults(Graph graph, List<EnergySource> sources, List<EnergyConsumer> consumers) {
        // Create a map to store allocations by consumer
        Map<String, Map<String, Double>> consumerAllocations = new HashMap<>();

        // Process all edges with positive flow
        for (EnergySource source : sources) {
            for (GraphEdge edge : graph.getOutgoingEdges(source.getId())) {
                if (edge.getFlow() > 0 && edge.getTarget().getNodeType() == NodeType.CONSUMER) {
                    String consumerId = edge.getTarget().getId();
                    double flow = edge.getFlow();

                    // Store the allocation
                    consumerAllocations.computeIfAbsent(consumerId, k -> new HashMap<>())
                            .put(source.getId(), flow);
                }
            }
        }

        // Print allocations by priority
        List<EnergyConsumer> sortedConsumers = new ArrayList<>(consumers);
        sortedConsumers.sort(Comparator.comparingInt(EnergyConsumer::getPriority)
                .thenComparing(EnergyConsumer::getId));

        System.out.println("Energy allocations by consumer (sorted by priority):");
        for (EnergyConsumer consumer : sortedConsumers) {
            System.out.printf("\nConsumer: %s (Priority: %d, Demand: %.2f kW)\n",
                    consumer.getId(), consumer.getPriority(), consumer.getDemand());

            Map<String, Double> allocations = consumerAllocations.getOrDefault(consumer.getId(), Collections.emptyMap());

            if (allocations.isEmpty()) {
                System.out.println("  No energy allocated.");
            } else {
                double totalAllocated = 0;

                for (Map.Entry<String, Double> allocation : allocations.entrySet()) {
                    String sourceId = allocation.getKey();
                    double amount = allocation.getValue();
                    totalAllocated += amount;

                    EnergySource source = (EnergySource) graph.getNode(sourceId);
                    System.out.printf("  From %s (%s): %.2f kW\n",
                            sourceId, source.getType(), amount);
                }

                // Calculate percentage of demand satisfied
                double demandSatisfiedPct = (totalAllocated / consumer.getDemand()) * 100;
                System.out.printf("  Total allocated: %.2f kW (%.2f%% of demand)\n",
                        totalAllocated, demandSatisfiedPct);
            }
        }

        System.out.println("\nEnergy usage by source:");
        for (EnergySource source : sources) {
            double totalUsed = source.getCurrentLoad();
            double usagePct = (totalUsed / source.getCapacity()) * 100;

            System.out.printf("%s (%s): %.2f kW used out of %.2f kW capacity (%.2f%%)\n",
                    source.getId(), source.getType(), totalUsed, source.getCapacity(), usagePct);
        }
    }

    /**
     * Prints statistics about the allocation outcome.
     *
     * @param sources List of energy sources
     * @param consumers List of energy consumers
     */
    private static void printAllocationStatistics(List<EnergySource> sources, List<EnergyConsumer> consumers) {
        // Calculate total demand, capacity, and allocation
        double totalDemand = consumers.stream().mapToDouble(EnergyConsumer::getDemand).sum();
        double totalCapacity = sources.stream().mapToDouble(EnergySource::getCapacity).sum();
        double totalAllocated = consumers.stream().mapToDouble(EnergyConsumer::getAllocatedEnergy).sum();

        // Calculate overall statistics
        double demandSatisfiedPct = (totalAllocated / totalDemand) * 100;
        double capacityUtilizationPct = (totalAllocated / totalCapacity) * 100;

        System.out.println("\n===== Overall Statistics =====");
        System.out.printf("Total energy demand: %.2f kW\n", totalDemand);
        System.out.printf("Total energy capacity: %.2f kW\n", totalCapacity);
        System.out.printf("Total energy allocated: %.2f kW\n", totalAllocated);
        System.out.printf("Demand satisfaction: %.2f%%\n", demandSatisfiedPct);
        System.out.printf("Capacity utilization: %.2f%%\n", capacityUtilizationPct);

        // Calculate statistics by priority
        Map<Integer, Double> demandByPriority = new HashMap<>();
        Map<Integer, Double> allocatedByPriority = new HashMap<>();

        for (EnergyConsumer consumer : consumers) {
            int priority = consumer.getPriority();
            demandByPriority.put(priority,
                    demandByPriority.getOrDefault(priority, 0.0) + consumer.getDemand());
            allocatedByPriority.put(priority,
                    allocatedByPriority.getOrDefault(priority, 0.0) + consumer.getAllocatedEnergy());
        }

        System.out.println("\nAllocation by priority level:");
        for (int priority : new TreeSet<>(demandByPriority.keySet())) {
            double demand = demandByPriority.get(priority);
            double allocated = allocatedByPriority.getOrDefault(priority, 0.0);
            double satisfactionPct = (allocated / demand) * 100;

            System.out.printf("Priority %d: %.2f kW allocated out of %.2f kW demanded (%.2f%%)\n",
                    priority, allocated, demand, satisfactionPct);
        }
    }
}