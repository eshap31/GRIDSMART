package com.example.gridsmart.tests;

import com.example.gridsmart.dynamic.DynamicReallocationManager;
import com.example.gridsmart.events.Event;
import com.example.gridsmart.events.EventSimulator;
import com.example.gridsmart.events.EventType;
import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;

public class sprint3_demo {
    public static void main(String[] args) {
        System.out.println("===== Starting Sprint 3 Demo =====");
        System.out.println("Testing Selective Deallocation");

        // Create graph
        Graph graph = new Graph();
        EnergyAllocationManager allocationManager = new EnergyAllocationManager(graph);

        // Create energy sources with limited capacity
        EnergySource solar = new EnergySource("solar", 1000, SourceType.SOLAR);
        EnergySource wind = new EnergySource("wind", 800, SourceType.WIND);

        // Add sources to graph
        graph.addNode(solar);
        graph.addNode(wind);

        // Create consumers with different priorities (1 is highest priority)
        EnergyConsumer hospital = new EnergyConsumer("hospital", 1, 600);   // High priority
        EnergyConsumer fireStation = new EnergyConsumer("fireStation", 1, 400); // High priority
        EnergyConsumer dataCenter = new EnergyConsumer("dataCenter", 2, 300);  // Medium priority
        EnergyConsumer mall = new EnergyConsumer("mall", 4, 500);      // Low priority

        // Add consumers to graph
        graph.addNode(hospital);
        graph.addNode(fireStation);
        graph.addNode(dataCenter);
        graph.addNode(mall);

        // Set up initial allocations - fully allocate all available energy
        System.out.println("\n----- Creating Initial Allocations -----");

        // Hospital gets power from solar
        allocationManager.addAllocation(hospital, solar, 600);
        System.out.println("Hospital allocated 600 from Solar");

        // Fire station gets power from wind
        allocationManager.addAllocation(fireStation, wind, 400);
        System.out.println("Fire Station allocated 400 from Wind");

        // Data center gets power from solar
        allocationManager.addAllocation(dataCenter, solar, 300);
        System.out.println("Data Center allocated 300 from Solar");

        // Mall gets power from wind
        allocationManager.addAllocation(mall, wind, 400);
        System.out.println("Mall allocated 400 from Wind");

        // Create reallocation manager (with selective deallocator)
        DynamicReallocationManager reallocationManager = new DynamicReallocationManager(graph, allocationManager);

        // Print initial allocation status
        System.out.println("\n----- Initial Energy Allocation Status -----");
        printAllocationStatus(allocationManager);

        // PART 1: Test scenario - add a new high-priority consumer
        System.out.println("\n===== Adding New High-Priority Consumer =====");

        // Create a new high-priority hospital that needs power
        EnergyConsumer emergencyHospital = new EnergyConsumer("emergencyHospital", 1, 500);
        graph.addNode(emergencyHospital);

        System.out.println("New Emergency Hospital needs 500 units of energy");
        System.out.println("All sources are fully allocated - selective deallocation needed");

        // Create event simulator
        EventSimulator simulator = new EventSimulator(5000, graph);
        simulator.setEventHandler(reallocationManager);


        // If not, you can perform allocation directly:
        allocationManager.addAllocation(emergencyHospital, solar, 0); // Add with 0 energy initially
        double allocated = reallocationManager.getGreedyReallocator().allocateEnergyToConsumer(
                emergencyHospital, emergencyHospital.getDemand());

        System.out.println("\nAllocated " + allocated + " energy to Emergency Hospital through selective deallocation");

        // Print allocation status after deallocation
        System.out.println("\n----- Allocation Status After Selective Deallocation -----");
        printAllocationStatus(allocationManager);

        // PART 2: Source failure with selective deallocation
        System.out.println("\n===== Testing Source Failure with Selective Deallocation =====");
        System.out.println("Triggering failure of Solar source");

        // Create and dispatch a source failure event for solar
        Event sourceFailureEvent = new Event(
                EventType.SOURCE_FAILURE,
                solar,
                "Solar source failure",
                System.currentTimeMillis()
        );

        reallocationManager.handleEvent(sourceFailureEvent);

        // Print allocation status after failure
        System.out.println("\n----- Allocation Status After Solar Failure -----");
        printAllocationStatus(allocationManager);

        // Print final statistics
        System.out.println("\n===== Final Statistics =====");
        reallocationManager.printStatistics();
        System.out.println("\nSprint 3 Demo completed");
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