package com.example.gridsmart.demo;

import com.example.gridsmart.graph.*;
import com.example.gridsmart.model.*;
import com.example.gridsmart.offline.GlobalAllocationAlgorithm;

import java.util.*;

/**
 * Demonstration of the graph-based energy allocation system.
 * Creates a sample network, runs the global allocation algorithm,
 * and displays the results.
 */
public class OfflineAllocationTest {
    public static void main(String[] args) {
        // Create sources
        EnergySource s1 = new EnergySource("S1", 300, SourceType.SOLAR);
        EnergySource s2 = new EnergySource("S2", 150, SourceType.WIND);
        EnergySource s3 = new EnergySource("S3", 100, SourceType.HYDRO);

        List<EnergySource> sources = new ArrayList<>();
        sources.add(s1);
        sources.add(s2);
        sources.add(s3);

        // Create consumers
        EnergyConsumer c1 = new EnergyConsumer("C1", 1, 150); // Hospital
        EnergyConsumer c2 = new EnergyConsumer("C2", 1, 100); // Fire Station
        EnergyConsumer c3 = new EnergyConsumer("C3", 3, 200); // Mall
        EnergyConsumer c4 = new EnergyConsumer("C4", 2, 180); // School

        List<EnergyConsumer> consumers = new ArrayList<>();
        consumers.add(c1);
        consumers.add(c2);
        consumers.add(c3);
        consumers.add(c4);

        // Create and populate the graph
        Graph graph = new Graph();

        // Add sources and consumers to graph
        for (EnergySource source : sources) {
            graph.addNode(source);
        }
        for (EnergyConsumer consumer : consumers) {
            graph.addNode(consumer);
        }

        // Add source-to-consumer edges with "infinite" capacity
        for (EnergySource source : sources) {
            for (EnergyConsumer consumer : consumers) {
                // add edge from source to consumer
                graph.addEdge(source.getId(), consumer.getId(), Double.MAX_VALUE);
            }
        }

        // Create a fresh graph for the allocation manager
        Graph managerGraph = new Graph();

        // Create the EnergyAllocationManager with a fresh graph
        EnergyAllocationManager manager = new EnergyAllocationManager(managerGraph);

        // Run the allocation algorithm with the manager
        GlobalAllocationAlgorithm allocator = new GlobalAllocationAlgorithm(manager);
        allocator.run(graph, consumers, sources);

        // Print results
        System.out.println("===== Final Allocations =====");
        for (EnergyConsumer consumer : consumers) {
            double allocated = consumer.getAllocatedEnergy();
            double demand = consumer.getDemand();
            double percentage = (demand > 0) ? (allocated / demand) * 100.0 : 0.0;

            System.out.printf("Consumer %s (Priority %d): Allocated %.2f / %.2f (%.2f%%)%n",
                    consumer.getId(),
                    consumer.getPriority(),
                    allocated,
                    demand,
                    percentage);
        }

        System.out.println("\n===== Source Loads =====");
        for (EnergySource source : sources) {
            System.out.printf("Source %s: Load %.2f / Capacity %.2f%n",
                    source.getId(),
                    source.getCurrentLoad(),
                    source.getCapacity());
        }

        // Get the detailed allocations directly from the algorithm's allocation store
        Map<String, Map<String, Double>> allocationStore = allocator.getAllocationStore();

        // Create maps for quick lookups
        Map<String, EnergyConsumer> consumerMap = new HashMap<>();
        for (EnergyConsumer consumer : consumers) {
            consumerMap.put(consumer.getId(), consumer);
        }

        Map<String, EnergySource> sourceMap = new HashMap<>();
        for (EnergySource source : sources) {
            sourceMap.put(source.getId(), source);
        }

        // Print detailed allocations per consumer with their sources
        System.out.println("\n===== Detailed Allocations from Algorithm =====");
        for (EnergyConsumer consumer : consumers) {
            String consumerId = consumer.getId();

            System.out.printf("Consumer %s (Priority %d, Demand %.2f):%n",
                    consumerId, consumer.getPriority(), consumer.getDemand());

            if (!allocationStore.containsKey(consumerId) || allocationStore.get(consumerId).isEmpty()) {
                System.out.println("  No allocations");
            } else {
                Map<String, Double> sourceAllocations = allocationStore.get(consumerId);
                for (Map.Entry<String, Double> entry : sourceAllocations.entrySet()) {
                    String sourceId = entry.getKey();
                    double allocation = entry.getValue();
                    EnergySource source = sourceMap.get(sourceId);

                    System.out.printf("  From %s (%s): %.2f%n",
                            sourceId, source.getType(), allocation);
                }
            }
        }

        // New section: Print which consumers are allocated energy from each source
        System.out.println("\n===== Source to Consumer Allocations from Algorithm =====");

        // Create reversed map: source -> consumer -> allocation
        Map<String, Map<String, Double>> sourceToConsumerMap = new HashMap<>();

        // Populate the reversed map
        for (Map.Entry<String, Map<String, Double>> consumerEntry : allocationStore.entrySet()) {
            String consumerId = consumerEntry.getKey();
            Map<String, Double> sourceAllocations = consumerEntry.getValue();

            for (Map.Entry<String, Double> sourceEntry : sourceAllocations.entrySet()) {
                String sourceId = sourceEntry.getKey();
                double allocation = sourceEntry.getValue();

                // Initialize the inner map if needed
                if (!sourceToConsumerMap.containsKey(sourceId)) {
                    sourceToConsumerMap.put(sourceId, new HashMap<>());
                }

                // Add the allocation
                sourceToConsumerMap.get(sourceId).put(consumerId, allocation);
            }
        }

        // Print the source to consumer allocations
        for (EnergySource source : sources) {
            String sourceId = source.getId();

            System.out.printf("Source %s (%s, Capacity %.2f):%n",
                    sourceId, source.getType(), source.getCapacity());

            if (!sourceToConsumerMap.containsKey(sourceId) || sourceToConsumerMap.get(sourceId).isEmpty()) {
                System.out.println("  No consumers allocated");
            } else {
                Map<String, Double> consumerAllocations = sourceToConsumerMap.get(sourceId);
                double totalAllocated = 0;

                for (Map.Entry<String, Double> entry : consumerAllocations.entrySet()) {
                    String consumerId = entry.getKey();
                    double allocation = entry.getValue();
                    EnergyConsumer consumer = consumerMap.get(consumerId);

                    System.out.printf("  To %s (Priority %d): %.2f%n",
                            consumerId, consumer.getPriority(), allocation);

                    totalAllocated += allocation;
                }

                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
                        totalAllocated, source.getCapacity(),
                        (totalAllocated / source.getCapacity()) * 100.0);
            }
        }

        // NEW SECTION: Print allocations from the EnergyAllocationManager
        System.out.println("\n===== EnergyAllocationManager Allocations =====");

        // Get the manager from the algorithm
        EnergyAllocationManager allocationManager = allocator.getAllocationManager();

        // Print allocations for each consumer
        System.out.println("\n----- Consumer Allocations from EnergyAllocationManager -----");
        Map<String, EnergyConsumer> allConsumers = allocationManager.getAllConsumers();

        for (EnergyConsumer consumer : allConsumers.values()) {
            System.out.printf("Consumer %s (Priority %d, Demand %.2f):%n",
                    consumer.getId(), consumer.getPriority(), consumer.getDemand());

            Map<EnergySource, Allocation> allocations = allocationManager.getAllocationsForConsumer(consumer);

            if (allocations.isEmpty()) {
                System.out.println("  No allocations");
            } else {
                double totalAllocation = 0;

                for (Map.Entry<EnergySource, Allocation> entry : allocations.entrySet()) {
                    EnergySource source = entry.getKey();
                    Allocation allocation = entry.getValue();
                    double amount = allocation.getAllocatedEnergy();

                    System.out.printf("  From %s (%s): %.2f%n",
                            source.getId(), source.getType(), amount);

                    totalAllocation += amount;
                }

                double fulfillmentPercentage = (consumer.getDemand() > 0) ?
                        (totalAllocation / consumer.getDemand()) * 100.0 : 0.0;

                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
                        totalAllocation, consumer.getDemand(), fulfillmentPercentage);
            }
        }

        // Print allocations for each source
        System.out.println("\n----- Source Allocations from EnergyAllocationManager -----");
        Map<String, EnergySource> allSources = allocationManager.getAllSources();

        for (EnergySource source : allSources.values()) {
            System.out.printf("Source %s (%s, Capacity %.2f):%n",
                    source.getId(), source.getType(), source.getCapacity());

            Map<EnergyConsumer, Allocation> allocations = allocationManager.getAllocationsForSource(source);

            if (allocations.isEmpty()) {
                System.out.println("  No consumers allocated");
            } else {
                double totalAllocated = 0;

                for (Map.Entry<EnergyConsumer, Allocation> entry : allocations.entrySet()) {
                    EnergyConsumer consumer = entry.getKey();
                    Allocation allocation = entry.getValue();
                    double amount = allocation.getAllocatedEnergy();

                    System.out.printf("  To %s (Priority %d): %.2f%n",
                            consumer.getId(), consumer.getPriority(), amount);

                    totalAllocated += amount;
                }

                double utilizationPercentage = (source.getCapacity() > 0) ?
                        (totalAllocated / source.getCapacity()) * 100.0 : 0.0;

                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
                        totalAllocated, source.getCapacity(), utilizationPercentage);
            }
        }

        // Check if the graph in the allocation manager is synchronized with allocations
        System.out.println("\n----- EnergyAllocationManager Graph Validation -----");
        managerGraph = allocationManager.getGraph();
        boolean graphValid = true;

        for (EnergyConsumer consumer : consumers) {
            // Check if consumer is in the graph
            if (managerGraph.getNode(consumer.getId()) == null) {
                System.out.println("ERROR: Consumer " + consumer.getId() + " is missing from manager graph");
                graphValid = false;
                continue;
            }

            // Get allocations from the manager
            Map<EnergySource, Allocation> allocations = allocationManager.getAllocationsForConsumer(consumer);

            // Verify each allocation has a corresponding edge in the graph with matching flow
            for (Map.Entry<EnergySource, Allocation> entry : allocations.entrySet()) {
                EnergySource source = entry.getKey();
                Allocation allocation = entry.getValue();
                double allocationAmount = allocation.getAllocatedEnergy();

                GraphEdge edge = managerGraph.getEdge(source.getId(), consumer.getId());
                if (edge == null) {
                    System.out.println("ERROR: Edge missing from " + source.getId() +
                            " to " + consumer.getId() + " in manager graph");
                    graphValid = false;
                } else if (Math.abs(edge.getFlow() - allocationAmount) > 0.001) {
                    System.out.println("ERROR: Flow mismatch for " + source.getId() +
                            " to " + consumer.getId() +
                            ". Allocation: " + allocationAmount +
                            ", Edge flow: " + edge.getFlow());
                    graphValid = false;
                }
            }
        }

        if (graphValid) {
            System.out.println("Graph validation successful: All allocations are properly reflected in the graph");
        } else {
            System.out.println("Graph validation failed: Some allocations are not properly reflected in the graph");
        }
    }
//    public static void main(String[] args) {
//        // Create sources
//        EnergySource s1 = new EnergySource("S1", 300, SourceType.SOLAR);
//        EnergySource s2 = new EnergySource("S2", 150, SourceType.WIND);
//        EnergySource s3 = new EnergySource("S3", 100, SourceType.HYDRO);
//
//        List<EnergySource> sources = new ArrayList<>();
//        sources.add(s1);
//        sources.add(s2);
//        sources.add(s3);
//
//        // Create consumers
//        EnergyConsumer c1 = new EnergyConsumer("C1", 1, 150); // Hospital
//        EnergyConsumer c2 = new EnergyConsumer("C2", 1, 100); // Fire Station
//        EnergyConsumer c3 = new EnergyConsumer("C3", 3, 200); // Mall
//        EnergyConsumer c4 = new EnergyConsumer("C4", 2, 180); // School
//
//        List<EnergyConsumer> consumers = new ArrayList<>();
//        consumers.add(c1);
//        consumers.add(c2);
//        consumers.add(c3);
//        consumers.add(c4);
//
//        // Create and populate the graph
//        Graph graph = new Graph();
//
//        // Add sources and consumers to graph
//        for (EnergySource source : sources) {
//            graph.addNode(source);
//        }
//        for (EnergyConsumer consumer : consumers) {
//            graph.addNode(consumer);
//        }
//
//        // Add source-to-consumer edges with "infinite" capacity
//        for (EnergySource source : sources) {
//            for (EnergyConsumer consumer : consumers) {
//                // add edge from source to consumer
//                graph.addEdge(source.getId(), consumer.getId(), Double.MAX_VALUE);
//            }
//        }
//
//        // Run the allocation algorithm
//        GlobalAllocationAlgorithm allocator = new GlobalAllocationAlgorithm();
//        allocator.run(graph, consumers, sources);
//
//        // Print results
//        System.out.println("===== Final Allocations =====");
//        for (EnergyConsumer consumer : consumers) {
//            double allocated = consumer.getAllocatedEnergy();
//            double demand = consumer.getDemand();
//            double percentage = (demand > 0) ? (allocated / demand) * 100.0 : 0.0;
//
//            System.out.printf("Consumer %s (Priority %d): Allocated %.2f / %.2f (%.2f%%)%n",
//                    consumer.getId(),
//                    consumer.getPriority(),
//                    allocated,
//                    demand,
//                    percentage);
//        }
//
//        System.out.println("\n===== Source Loads =====");
//        for (EnergySource source : sources) {
//            System.out.printf("Source %s: Load %.2f / Capacity %.2f%n",
//                    source.getId(),
//                    source.getCurrentLoad(),
//                    source.getCapacity());
//        }
//
//        // Get the detailed allocations directly from the algorithm's allocation store
//        Map<String, Map<String, Double>> allocationStore = allocator.getAllocationStore();
//
//        // Create maps for quick lookups
//        Map<String, EnergyConsumer> consumerMap = new HashMap<>();
//        for (EnergyConsumer consumer : consumers) {
//            consumerMap.put(consumer.getId(), consumer);
//        }
//
//        Map<String, EnergySource> sourceMap = new HashMap<>();
//        for (EnergySource source : sources) {
//            sourceMap.put(source.getId(), source);
//        }
//
//        // Print detailed allocations per consumer with their sources
//        System.out.println("\n===== Detailed Allocations from Algorithm =====");
//        for (EnergyConsumer consumer : consumers) {
//            String consumerId = consumer.getId();
//
//            System.out.printf("Consumer %s (Priority %d, Demand %.2f):%n",
//                    consumerId, consumer.getPriority(), consumer.getDemand());
//
//            if (!allocationStore.containsKey(consumerId) || allocationStore.get(consumerId).isEmpty()) {
//                System.out.println("  No allocations");
//            } else {
//                Map<String, Double> sourceAllocations = allocationStore.get(consumerId);
//                for (Map.Entry<String, Double> entry : sourceAllocations.entrySet()) {
//                    String sourceId = entry.getKey();
//                    double allocation = entry.getValue();
//                    EnergySource source = sourceMap.get(sourceId);
//
//                    System.out.printf("  From %s (%s): %.2f%n",
//                            sourceId, source.getType(), allocation);
//                }
//            }
//        }
//
//        // New section: Print which consumers are allocated energy from each source
//        System.out.println("\n===== Source to Consumer Allocations from Algorithm =====");
//
//        // Create reversed map: source -> consumer -> allocation
//        Map<String, Map<String, Double>> sourceToConsumerMap = new HashMap<>();
//
//        // Populate the reversed map
//        for (Map.Entry<String, Map<String, Double>> consumerEntry : allocationStore.entrySet()) {
//            String consumerId = consumerEntry.getKey();
//            Map<String, Double> sourceAllocations = consumerEntry.getValue();
//
//            for (Map.Entry<String, Double> sourceEntry : sourceAllocations.entrySet()) {
//                String sourceId = sourceEntry.getKey();
//                double allocation = sourceEntry.getValue();
//
//                // Initialize the inner map if needed
//                if (!sourceToConsumerMap.containsKey(sourceId)) {
//                    sourceToConsumerMap.put(sourceId, new HashMap<>());
//                }
//
//                // Add the allocation
//                sourceToConsumerMap.get(sourceId).put(consumerId, allocation);
//            }
//        }
//
//        // Print the source to consumer allocations
//        for (EnergySource source : sources) {
//            String sourceId = source.getId();
//
//            System.out.printf("Source %s (%s, Capacity %.2f):%n",
//                    sourceId, source.getType(), source.getCapacity());
//
//            if (!sourceToConsumerMap.containsKey(sourceId) || sourceToConsumerMap.get(sourceId).isEmpty()) {
//                System.out.println("  No consumers allocated");
//            } else {
//                Map<String, Double> consumerAllocations = sourceToConsumerMap.get(sourceId);
//                double totalAllocated = 0;
//
//                for (Map.Entry<String, Double> entry : consumerAllocations.entrySet()) {
//                    String consumerId = entry.getKey();
//                    double allocation = entry.getValue();
//                    EnergyConsumer consumer = consumerMap.get(consumerId);
//
//                    System.out.printf("  To %s (Priority %d): %.2f%n",
//                            consumerId, consumer.getPriority(), allocation);
//
//                    totalAllocated += allocation;
//                }
//
//                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
//                        totalAllocated, source.getCapacity(),
//                        (totalAllocated / source.getCapacity()) * 100.0);
//            }
//        }
//    }
}