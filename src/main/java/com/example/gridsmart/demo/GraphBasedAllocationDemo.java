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
    private static final double INFINITE_CAPACITY = 99999.0; // Define a large value for "infinite" capacity

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
                // Use a large value to represent "infinite" capacity
                graph.addEdge(source.getId(), consumer.getId(), INFINITE_CAPACITY);
            }
        }

        // Run the allocation algorithm
        GlobalAllocationAlgorithm allocator = new GlobalAllocationAlgorithm();
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

        // Print allocations per consumer with their sources
        System.out.println("\n===== Detailed Allocations =====");
        EnergyAllocationManager manager = new EnergyAllocationManager(graph);
        for (EnergyConsumer consumer : consumers) {
            Map<EnergySource, Allocation> allocations = manager.getAllocationsForConsumer(consumer);

            System.out.printf("Consumer %s (Priority %d, Demand %.2f):%n",
                    consumer.getId(), consumer.getPriority(), consumer.getDemand());

            if (allocations.isEmpty()) {
                System.out.println("  No allocations");
            } else {
                for (Map.Entry<EnergySource, Allocation> entry : allocations.entrySet()) {
                    EnergySource source = entry.getKey();
                    Allocation allocation = entry.getValue();

                    System.out.printf("  From %s: %.2f%n",
                            source.getId(), allocation.getAllocatedEnergy());
                }
            }
        }
    }
}