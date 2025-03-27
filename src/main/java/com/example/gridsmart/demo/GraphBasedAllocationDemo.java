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

        // Run the allocation algorithm
        GlobalAllocationAlgorithm allocator = new GlobalAllocationAlgorithm();
        allocator.run(graph, consumers, sources);

        // Print results
        System.out.println("\n\n===== Final Allocations =====");
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

        // Create an EnergyAllocationManager to get the detailed allocations
        EnergyAllocationManager manager = new EnergyAllocationManager(graph);

        // Print detailed allocations per consumer with their sources
        System.out.println("\n===== Detailed Allocations =====");
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

                    System.out.printf("  From %s (%s): %.2f%n",
                            source.getId(), source.getType(), allocation.getAllocatedEnergy());
                }
            }
        }

        // New section: Print which consumers are allocated energy from each source
        System.out.println("\n===== Source to Consumer Allocations =====");
        for (EnergySource source : sources) {
            Map<EnergyConsumer, Allocation> allocations = manager.getAllocationsForSource(source);

            System.out.printf("Source %s (%s, Capacity %.2f):%n",
                    source.getId(), source.getType(), source.getCapacity());

            if (allocations.isEmpty()) {
                System.out.println("  No consumers allocated");
            } else {
                double totalAllocated = 0;
                for (Map.Entry<EnergyConsumer, Allocation> entry : allocations.entrySet()) {
                    EnergyConsumer consumer = entry.getKey();
                    Allocation allocation = entry.getValue();

                    System.out.printf("  To %s (Priority %d): %.2f%n",
                            consumer.getId(), consumer.getPriority(), allocation.getAllocatedEnergy());

                    totalAllocated += allocation.getAllocatedEnergy();
                }

                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
                        totalAllocated, source.getCapacity(),
                        (totalAllocated / source.getCapacity()) * 100.0);
            }
        }
    }
}