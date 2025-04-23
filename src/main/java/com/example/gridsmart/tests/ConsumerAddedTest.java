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

import java.util.ArrayList;
import java.util.Map;

public class ConsumerAddedTest {
    public static void main(String[] args) {
        System.out.println("===== Starting CONSUMER_ADDED Event Test =====");

        // Create a graph with sources and consumers
        Graph graph = new Graph();
        EnergyAllocationManager allocationManager = new EnergyAllocationManager(graph);

        // Create energy sources
        EnergySource solar = new EnergySource("solar1", 800, SourceType.SOLAR);
        EnergySource wind = new EnergySource("wind1", 600, SourceType.WIND);
        EnergySource hydro = new EnergySource("hydro1", 1000, SourceType.HYDRO);

        // Add sources to graph
        graph.addNode(solar);
        graph.addNode(wind);
        graph.addNode(hydro);

        // Create initial consumers
        EnergyConsumer hospital = new EnergyConsumer("hospital", 1, 700);
        EnergyConsumer dataCenter = new EnergyConsumer("dataCenter", 2, 500);
        EnergyConsumer school = new EnergyConsumer("school", 3, 400);
        EnergyConsumer mall = new EnergyConsumer("mall", 4, 300);

        // Add consumers to graph
        graph.addNode(hospital);
        graph.addNode(dataCenter);
        graph.addNode(school);
        graph.addNode(mall);

        // Add connections between sources and consumers
        for (EnergyNode source : graph.getNodesByType(NodeType.SOURCE)) {
            for (EnergyNode consumer : graph.getNodesByType(NodeType.CONSUMER)) {
                graph.addEdge(source.getId(), consumer.getId(), ((EnergySource)source).getCapacity());
            }
        }

        // Set up initial allocations (leaving some energy available)
        System.out.println("\n----- Creating Initial Allocations -----");

        // Hospital gets power from solar
        allocationManager.addAllocation(hospital, solar, 700);
        System.out.println("Hospital allocated 700 from Solar");

        // Data center gets power from wind
        allocationManager.addAllocation(dataCenter, wind, 500);
        System.out.println("Data Center allocated 500 from Wind");

        // School gets power from hydro
        allocationManager.addAllocation(school, hydro, 400);
        System.out.println("School allocated 400 from Hydro");

        // Mall gets power from hydro and wind
        allocationManager.addAllocation(mall, hydro, 200);
        allocationManager.addAllocation(mall, wind, 100);
        System.out.println("Mall allocated 200 from Hydro and 100 from Wind");

        // Create reallocation manager with selective deallocator
        DynamicReallocationManager reallocationManager = new DynamicReallocationManager(graph, allocationManager);

        // Print initial allocation status
        System.out.println("\n----- Initial Energy Allocation Status -----");
        printAllocationStatus(allocationManager);

        // Display available energy
        System.out.println("\nAVAILABLE ENERGY IN SOURCES:");
        for (EnergySource source : allocationManager.getAllSources().values()) {
            System.out.printf("%s: %.1f units available\n",
                    source.getId(), source.getAvailableEnergy());
        }

        // Count initial consumers
        int initialConsumerCount = countNodesByType(graph, NodeType.CONSUMER);
        System.out.println("\nInitial number of consumers: " + initialConsumerCount);

        // Create event simulator
        EventSimulator simulator = new EventSimulator(5000, graph);
        simulator.setEventHandler(reallocationManager);

        // PART 1: Test CONSUMER_ADDED event with available energy
        System.out.println("\n===== PART 1: Adding Consumer with Available Energy =====");

        // Create and dispatch a consumer added event
        Event consumerAddedEvent = simulator.createEvent(EventType.CONSUMER_ADDED);
        if (consumerAddedEvent != null) {
            System.out.println("\nTriggering CONSUMER_ADDED event: " + consumerAddedEvent.getEventDescription());
            reallocationManager.handleEvent(consumerAddedEvent);
        } else {
            System.out.println("Failed to create CONSUMER_ADDED event!");
        }

        // Verify a new consumer was added
        int newConsumerCount = countNodesByType(graph, NodeType.CONSUMER);
        System.out.println("\nNumber of consumers after event: " + newConsumerCount);
        System.out.println("New consumers added: " + (newConsumerCount - initialConsumerCount));

        // Print allocation status after adding new consumer
        System.out.println("\n----- Allocation Status After Adding First Consumer -----");
        printAllocationStatus(allocationManager);

        // PART 2: Test CONSUMER_ADDED event with high priority requiring deallocation
        System.out.println("\n===== PART 2: Adding High-Priority Consumer (Priority 1) =====");

        // Create a high-priority consumer with high demand (needs selective deallocation)
        EnergyConsumer emergencyCenter = new EnergyConsumer("emergencyCenter", 1, 1000);
        ArrayList<EnergyNode> nodes = new ArrayList<>();
        nodes.add(emergencyCenter);
        Event highPriorityEvent = new Event(
                EventType.CONSUMER_ADDED,
                nodes,
                "New emergency center added with priority 1 and demand 1000.00",
                System.currentTimeMillis()
        );

        System.out.println("\nTriggering CONSUMER_ADDED event with high-priority consumer: " +
                highPriorityEvent.getEventDescription());
        reallocationManager.handleEvent(highPriorityEvent);

        // Print allocation status after adding high-priority consumer
        System.out.println("\n----- Allocation Status After Adding High-Priority Consumer -----");
        printAllocationStatus(allocationManager);

        // PART 3: Test CONSUMER_ADDED event with low priority (shouldn't trigger deallocation)
        System.out.println("\n===== PART 3: Adding Low-Priority Consumer (Priority 5) =====");

        // Create a low-priority consumer (shouldn't trigger deallocation)
        EnergyConsumer residentialComplex = new EnergyConsumer("residentialComplex", 5, 400);
        nodes = new ArrayList<>();
        nodes.add(residentialComplex);
        Event lowPriorityEvent = new Event(
                EventType.CONSUMER_ADDED,
                nodes,
                "New residential complex added with priority 5 and demand 400.00",
                System.currentTimeMillis()
        );

        System.out.println("\nTriggering CONSUMER_ADDED event with low-priority consumer: " +
                lowPriorityEvent.getEventDescription());
        reallocationManager.handleEvent(lowPriorityEvent);

        // Print final allocation status
        System.out.println("\n----- Final Allocation Status -----");
        printAllocationStatus(allocationManager);

        // Final consumer count
        int finalConsumerCount = countNodesByType(graph, NodeType.CONSUMER);
        System.out.println("\nFinal number of consumers: " + finalConsumerCount);
        System.out.println("Total new consumers added: " + (finalConsumerCount - initialConsumerCount));

        // Print final statistics
        System.out.println("\n===== Final Statistics =====");
        reallocationManager.printStatistics();
        System.out.println("\nCONSUMER_ADDED Event Test completed");
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
            double percentSatisfied = (demand > 0) ? (allocated / demand) * 100 : 0;

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