package com.example.gridsmart.demo;

import com.example.gridsmart.dynamic.DynamicReallocationManager;
import com.example.gridsmart.events.Event;
import com.example.gridsmart.events.EventSimulator;
import com.example.gridsmart.events.EventType;
import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;

public class Sprint_1and2_Demo {
    public static void main(String[] args) {
        System.out.println("===== Starting Sprint 2 Demo =====");
        System.out.println("Testing Event System and Greedy Reallocation");

        // Create a more complex graph for testing
        Graph graph = new Graph();
        EnergyAllocationManager allocationManager = new EnergyAllocationManager(graph);

        // Create energy sources with different capacities
        EnergySource solar1 = new EnergySource("solar1", 800, SourceType.SOLAR);
        EnergySource solar2 = new EnergySource("solar2", 500, SourceType.SOLAR);
        EnergySource wind1 = new EnergySource("wind1", 600, SourceType.WIND);
        EnergySource hydro1 = new EnergySource("hydro1", 1200, SourceType.HYDRO);
        EnergySource battery1 = new EnergySource("battery1", 400, SourceType.BATTERY);

        // Add sources to graph
        graph.addNode(solar1);
        graph.addNode(solar2);
        graph.addNode(wind1);
        graph.addNode(hydro1);
        graph.addNode(battery1);

        // Create consumers with different priorities and demands
        // Priority 1 is highest priority
        EnergyConsumer hospital = new EnergyConsumer("hospital", 1, 700);
        EnergyConsumer fireStation = new EnergyConsumer("fireStation", 1, 300);
        EnergyConsumer dataCenter = new EnergyConsumer("dataCenter", 2, 550);
        EnergyConsumer school = new EnergyConsumer("school", 3, 400);
        EnergyConsumer mall = new EnergyConsumer("mall", 4, 600);
        EnergyConsumer office = new EnergyConsumer("office", 3, 350);

        // Add consumers to graph
        graph.addNode(hospital);
        graph.addNode(fireStation);
        graph.addNode(dataCenter);
        graph.addNode(school);
        graph.addNode(mall);
        graph.addNode(office);

        // Set up initial allocations
        System.out.println("\n----- Creating Initial Allocations -----");

        // Hospital gets power from hydro
        allocationManager.addAllocation(hospital, hydro1, 700);
        System.out.println("Hospital allocated 700 from Hydro1");

        // Fire station gets power from battery
        allocationManager.addAllocation(fireStation, battery1, 300);
        System.out.println("Fire Station allocated 300 from Battery1");

        // Data center gets power from solar1
        allocationManager.addAllocation(dataCenter, solar1, 550);
        System.out.println("Data Center allocated 550 from Solar1");

        // School gets power from solar2
        allocationManager.addAllocation(school, solar2, 400);
        System.out.println("School allocated 400 from Solar2");

        // Mall gets power from wind1
        allocationManager.addAllocation(mall, wind1, 600);
        System.out.println("Mall allocated 600 from Wind1");

        // Office gets remaining energy from solar1 and solar2
        allocationManager.addAllocation(office, solar1, 250);
        allocationManager.addAllocation(office, solar2, 100);
        System.out.println("Office allocated 250 from Solar1 and 100 from Solar2");

        // Create reallocation manager
        DynamicReallocationManager reallocationManager = new DynamicReallocationManager(graph, allocationManager);

        // Print initial allocation status
        System.out.println("\n----- Initial Energy Allocation Status -----");
        printAllocationStatus(allocationManager);

        // PART 1: Manual event testing - Source failure
        System.out.println("\n===== PART 1: Testing Manual Source Failure =====");
        System.out.println("Triggering failure of Solar1, which powers Data Center and partially Office");

        // Create event simulator but don't start automatic generation
        EventSimulator simulator = new EventSimulator(5000, graph);
        simulator.setEventHandler(reallocationManager);

        // Create and dispatch a source failure event for solar1
        Event sourceFailureEvent = simulator.createEvent(EventType.SOURCE_FAILURE);
        if (sourceFailureEvent != null) {
            reallocationManager.handleEvent(sourceFailureEvent);
        }

        // Print allocation status after failure
        System.out.println("\n----- Allocation Status After Solar1 Failure -----");
        printAllocationStatus(allocationManager);

        // PART 2: Automatic event generation
        System.out.println("\n===== PART 2: Testing Automatic Event Generation =====");
        System.out.println("Starting automatic event generation (one event every 3 seconds)");
        simulator.start();

        // Run for 9 seconds (should generate approximately 3 events)
        try {
            for (int i = 0; i < 3; i++) {
                System.out.println("\nWaiting for next event...");
                Thread.sleep(3000);
                System.out.println("\n----- Current Allocation Status -----");
                printAllocationStatus(allocationManager);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Stop automatic event generation
        simulator.stop();

        // Print final statistics
        System.out.println("\n===== Final Statistics =====");
        reallocationManager.printStatistics();
        System.out.println("\nSprint 2 Demo completed");
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
                System.out.printf("%s: %.1f/%.1f units used (%.1f%% capacity)\n",
                        source.getId(),
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