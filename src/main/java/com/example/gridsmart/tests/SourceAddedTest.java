package com.example.gridsmart.tests;

import com.example.gridsmart.dynamic.DynamicReallocationManager;
import com.example.gridsmart.events.Event;
import com.example.gridsmart.events.EventSimulator;
import com.example.gridsmart.events.EventType;
import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergyNode;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;
import com.example.gridsmart.model.NodeType;

import java.util.Map;

public class SourceAddedTest {
    public static void main(String[] args) {
        System.out.println("===== Starting SOURCE_ADDED Event Test =====");

        // Create a graph with initial sources and consumers
        Graph graph = new Graph();
        EnergyAllocationManager allocationManager = new EnergyAllocationManager(graph);

        // Create initial energy sources
        EnergySource solar = new EnergySource("solar1", 600, SourceType.SOLAR);
        EnergySource wind = new EnergySource("wind1", 400, SourceType.WIND);

        // Add sources to graph
        graph.addNode(solar);
        graph.addNode(wind);

        // Create consumers with different priorities and demands
        // Priority 1 is highest priority
        EnergyConsumer hospital = new EnergyConsumer("hospital", 1, 700);  // This consumer will need more energy
        EnergyConsumer school = new EnergyConsumer("school", 3, 250);
        EnergyConsumer mall = new EnergyConsumer("mall", 4, 300);

        // Add consumers to graph
        graph.addNode(hospital);
        graph.addNode(school);
        graph.addNode(mall);

        // Add connections between sources and consumers
        graph.addEdge(solar.getId(), hospital.getId(), solar.getCapacity());
        graph.addEdge(solar.getId(), school.getId(), solar.getCapacity());
        graph.addEdge(solar.getId(), mall.getId(), solar.getCapacity());

        graph.addEdge(wind.getId(), hospital.getId(), wind.getCapacity());
        graph.addEdge(wind.getId(), school.getId(), wind.getCapacity());
        graph.addEdge(wind.getId(), mall.getId(), wind.getCapacity());

        // Set up initial allocations - not enough for hospital
        System.out.println("\n----- Creating Initial Allocations -----");

        // Hospital gets power but not enough to meet full demand
        allocationManager.addAllocation(hospital, solar, 500);
        System.out.println("Hospital allocated 500 from Solar (needs 700)");

        // School gets power from wind
        allocationManager.addAllocation(school, wind, 250);
        System.out.println("School allocated 250 from Wind");

        // Mall gets rest of wind power
        allocationManager.addAllocation(mall, wind, 150);
        allocationManager.addAllocation(mall, solar, 100);
        System.out.println("Mall allocated 150 from Wind and 100 from Solar");

        // Create reallocation manager
        DynamicReallocationManager reallocationManager = new DynamicReallocationManager(graph, allocationManager);

        // Print initial allocation status
        System.out.println("\n----- Initial Energy Allocation Status -----");
        printAllocationStatus(allocationManager);

        // Count initial sources
        int initialSourceCount = countNodesByType(graph, NodeType.SOURCE);
        System.out.println("\nInitial number of sources: " + initialSourceCount);

        // Create event simulator
        EventSimulator simulator = new EventSimulator(5000, graph);
        simulator.setEventHandler(reallocationManager);

        // PART 1: Test SOURCE_ADDED event
        System.out.println("\n===== PART 1: Testing SOURCE_ADDED Event =====");

        // Create and dispatch a source added event
        Event sourceAddedEvent = simulator.createEvent(EventType.SOURCE_ADDED);
        if (sourceAddedEvent != null) {
            System.out.println("\nTriggering SOURCE_ADDED event: " + sourceAddedEvent.getEventDescription());
            reallocationManager.handleEvent(sourceAddedEvent);
        } else {
            System.out.println("Failed to create SOURCE_ADDED event!");
        }

        // Verify a new source was added
        int newSourceCount = countNodesByType(graph, NodeType.SOURCE);
        System.out.println("\nNumber of sources after event: " + newSourceCount);
        System.out.println("New sources added: " + (newSourceCount - initialSourceCount));

        // Print allocation status after adding new source
        System.out.println("\n----- Allocation Status After Adding New Source -----");
        printAllocationStatus(allocationManager);

        // PART 2: Test multiple SOURCE_ADDED events
        System.out.println("\n===== PART 2: Testing Multiple SOURCE_ADDED Events =====");

        for (int i = 0; i < 3; i++) {
            System.out.println("\n--- Adding source #" + (i+1) + " ---");
            sourceAddedEvent = simulator.createEvent(EventType.SOURCE_ADDED);
            if (sourceAddedEvent != null) {
                reallocationManager.handleEvent(sourceAddedEvent);
            }
        }

        // Print final allocation status
        System.out.println("\n----- Final Allocation Status -----");
        printAllocationStatus(allocationManager);

        // Final source count
        int finalSourceCount = countNodesByType(graph, NodeType.SOURCE);
        System.out.println("\nFinal number of sources: " + finalSourceCount);
        System.out.println("Total new sources added: " + (finalSourceCount - initialSourceCount));

        // Print final statistics
        System.out.println("\n===== Final Statistics =====");
        reallocationManager.printStatistics();
        System.out.println("\nSOURCE_ADDED Event Test completed");
    }

    // Helper method to count nodes of a specific type
    private static int countNodesByType(Graph graph, NodeType type) {
        return graph.getNodesByType(type).size();
    }

    // Helper method to print current allocation status
    private static void printAllocationStatus(EnergyAllocationManager allocationManager) {
        System.out.println("CONSUMER STATUS:");
        for (EnergyConsumer consumer : allocationManager.getAllConsumers().values()) {
            double demand = consumer.getDemand();
            double allocated = consumer.getAllocatedEnergy();
            double percentSatisfied = (allocated / demand) * 100;

            System.out.printf("%s (Priority %d): %.1f/%.1f units (%.1f%%) - %s\n",
                    consumer.getId(),
                    consumer.getPriority(),
                    allocated,
                    demand,
                    percentSatisfied,
                    (Math.abs(allocated - demand) < 0.001) ? "FULLY SATISFIED" : "PARTIALLY SATISFIED"
            );
        }

        System.out.println("\nSOURCE STATUS:");
        for (EnergySource source : allocationManager.getAllSources().values()) {
            if (source.isActive()) {
                System.out.printf("%s (%s): %.1f/%.1f units used (%.1f%% capacity)\n",
                        source.getId(),
                        source.getType(),
                        source.getCurrentLoad(),
                        source.getCapacity(),
                        (source.getCurrentLoad() / source.getCapacity()) * 100
                );
            } else {
                System.out.printf("%s: OFFLINE\n", source.getId());
            }
        }
    }
}