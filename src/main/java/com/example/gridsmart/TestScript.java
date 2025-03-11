package com.example.gridsmart;

// testing script for the GridSmart project
public class TestScript
{
    public static void main(String[] args)
    {
        EnergyAllocationManager manager = new EnergyAllocationManager();

        // Create EnergySources
        EnergySource source1 = new EnergySource("S1", 100, SourceType.SOLAR);
        EnergySource source2 = new EnergySource("S2", 200, SourceType.NUCLEAR);

        // Create EnergyConsumers
        EnergyConsumer consumer1 = new EnergyConsumer("C1", 1, 150);
        EnergyConsumer consumer2 = new EnergyConsumer("C2", 2, 100);

        // Add allocations
        manager.addAllocation(consumer1, source1, 50);
        manager.addAllocation(consumer1, source2, 100);
        manager.addAllocation(consumer2, source1, 75);

        System.out.println("Allocations for C1: " + manager.getAllocationsForConsumer(consumer1));
        System.out.println("Allocations for S1: " + manager.getAllocationsForSource(source1));

        // Update an allocation
        manager.updateAllocation(consumer1, source1, 60);
        System.out.println("After updating, C1 allocation from S1: " + manager.getAllocation(consumer1, source1));

        // Remove a specific allocation
        manager.removeAllocation(consumer1, source1);
        System.out.println("After removing S1 allocation, C1 allocations: " + manager.getAllocationsForConsumer(consumer1));

        // Remove all allocations for C2
        manager.removeAllocationsForConsumer(consumer2);
        System.out.println("After removing C2, allocations for C2: " + manager.getAllocationsForConsumer(consumer2)); // Expected: []
    }
}
