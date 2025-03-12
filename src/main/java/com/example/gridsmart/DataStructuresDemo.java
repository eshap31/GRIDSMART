package com.example.gridsmart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// testing script for the GridSmart project
public class DataStructuresDemo
{
    public static void main(String[] args) {
        System.out.println("============= GRID SMART DEMO =============");

        // 1. Create the core components
        EnergySourceQueue sourceQueue = new EnergySourceQueue();
        EnergyConsumerQueue consumerQueue = new EnergyConsumerQueue();
        EnergyAllocationManager allocationManager = new EnergyAllocationManager();

        // 2. Create some energy sources
        EnergySource solar = new EnergySource("Solar1", 500, SourceType.SOLAR);
        EnergySource wind = new EnergySource("Wind1", 300, SourceType.WIND);
        EnergySource hydro = new EnergySource("Hydro1", 600, SourceType.HYDRO);

        // 3. Create some energy consumers
        EnergyConsumer hospital = new EnergyConsumer("Hospital", 1, 400);  // Highest priority
        EnergyConsumer dataCenter = new EnergyConsumer("DataCenter", 2, 350);
        EnergyConsumer residential = new EnergyConsumer("Residential", 3, 250);

        // 4. Add sources and consumers to their queues
        sourceQueue.add(solar);
        sourceQueue.add(wind);
        sourceQueue.add(hydro);

        consumerQueue.add(hospital);
        consumerQueue.add(dataCenter);
        consumerQueue.add(residential);

        // 5. Check initial state
        System.out.println("\n--- Initial state ---");
        System.out.println("Highest priority consumer: " + consumerQueue.peekHighestPriorityConsumer().getId());
        System.out.println("Source with most available energy: " + sourceQueue.peekHighestEnergySource().getId()
                + " (" + sourceQueue.peekHighestEnergySource().getAvailableEnergy() + " kW)");

        // 6. Perform allocations
        System.out.println("\n--- Making allocations ---");

        // Hospital allocations
        allocationManager.addAllocation(hospital, solar, 200);
        allocationManager.addAllocation(hospital, hydro, 200);
        System.out.println("Allocated energy to Hospital: 400 kW");

        // Data center allocations
        allocationManager.addAllocation(dataCenter, wind, 200);
        allocationManager.addAllocation(dataCenter, hydro, 150);
        System.out.println("Allocated energy to DataCenter: 350 kW");

        // Residential allocations
        allocationManager.addAllocation(residential, solar, 150);
        allocationManager.addAllocation(residential, wind, 100);
        System.out.println("Allocated energy to Residential: 250 kW");

        // 7. Update queues (normally would happen automatically)
        updateQueues(sourceQueue, consumerQueue);

        // 8. Check state after allocations
        System.out.println("\n--- After allocations ---");
        System.out.println("Highest priority consumer: " + consumerQueue.peekHighestPriorityConsumer().getId());
        System.out.println("Source with most available energy: " + sourceQueue.peekHighestEnergySource().getId()
                + " (" + sourceQueue.peekHighestEnergySource().getAvailableEnergy() + " kW)");

        // 9. Test allocation queries
        System.out.println("\n--- Allocation queries ---");
        System.out.println("Hospital's allocations:");
        var hospitalAllocations = allocationManager.getAllocationsForConsumer(hospital);
        hospitalAllocations.forEach((source, allocation) -> {
            System.out.println("  - From " + source.getId() + ": " + allocation.getAllocatedEnergy() + " kW");
        });

        System.out.println("\nConsumers using Hydro1:");
        var hydroConsumers = allocationManager.getAllocationsForSource(hydro);
        hydroConsumers.forEach((consumer, allocation) -> {
            System.out.println("  - " + consumer.getId() + ": " + allocation.getAllocatedEnergy() + " kW");
        });

        // 10. Test updating an allocation
        System.out.println("\n--- Updating an allocation ---");
        System.out.println("Original allocation from Solar1 to Hospital: " +
                allocationManager.getAllocation(hospital, solar).getAllocatedEnergy() + " kW");

        allocationManager.updateAllocation(hospital, solar, 250);
        System.out.println("Updated allocation: " +
                allocationManager.getAllocation(hospital, solar).getAllocatedEnergy() + " kW");

        // 11. Check if consumers are fully allocated
        System.out.println("\n--- Checking allocation status ---");
        System.out.println("Is Hospital fully allocated? " + allocationManager.isFullyAllocated(hospital));
        System.out.println("Is DataCenter fully allocated? " + allocationManager.isFullyAllocated(dataCenter));

        // 12. Print all allocations using toString()
        System.out.println("\n--- All allocations in the system ---");
        printAllAllocations(allocationManager);

        System.out.println("\n========== DEMO COMPLETE ==========");
    }

    /**
     * Helper method to update queues after allocations
     */
    private static void updateQueues(EnergySourceQueue sourceQueue, EnergyConsumerQueue consumerQueue) {
        // Re-add all sources to update their positions in the queue
        EnergySourceQueue tempSourceQueue = new EnergySourceQueue();
        tempSourceQueue.addAll(sourceQueue);
        sourceQueue.clear();
        sourceQueue.addAll(tempSourceQueue);

        // Re-add all consumers to update their positions in the queue
        EnergyConsumerQueue tempConsumerQueue = new EnergyConsumerQueue();
        tempConsumerQueue.addAll(consumerQueue);
        consumerQueue.clear();
        consumerQueue.addAll(tempConsumerQueue);
    }

    /**
     * Helper method to print all allocations in the system
     */
    private static void printAllAllocations(EnergyAllocationManager allocationManager) {
        Map<String, EnergyConsumer> consumers = allocationManager.getAllConsumers();

        if (consumers.isEmpty()) {
            System.out.println("No allocations found.");
            return;
        }

        for (EnergyConsumer consumer : consumers.values()) {
            Map<EnergySource, Allocation> allocations = allocationManager.getAllocationsForConsumer(consumer);
            if (!allocations.isEmpty()) {
                for (Allocation allocation : allocations.values()) {
                    System.out.println(allocation.toString());
                }
            }
        }
    }
}
