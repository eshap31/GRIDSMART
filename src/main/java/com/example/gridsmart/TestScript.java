package com.example.gridsmart;

// testing script for the GridSmart project
public class TestScript
{
    public static void main(String[] args)
    {
        ReverseAllocationMap reverseMap = new ReverseAllocationMap();

        // Create EnergySources
        EnergySource source1 = new EnergySource("S1", 100, SourceType.HYDRO);
        EnergySource source2 = new EnergySource("S2", 200, SourceType.FOSSIL_FUEL);

        // Create EnergyConsumers
        EnergyConsumer consumer1 = new EnergyConsumer("C1", 1, 150);
        EnergyConsumer consumer2 = new EnergyConsumer("C2", 2, 100);

        // Add allocations (fragmentation example)
        reverseMap.addAllocation(source1, consumer1, 50);
        reverseMap.addAllocation(source2, consumer1, 100);
        reverseMap.addAllocation(source1, consumer2, 75);

        System.out.println("Consumers relying on S1: " + reverseMap.getAllocations(source1));
        System.out.println("Consumers relying on S2: " + reverseMap.getAllocations(source2));

        // Retrieve a specific allocation
        System.out.println("C1 allocation from S1: " + reverseMap.getAllocation(source1, consumer1));

        // Update an allocation
        reverseMap.updateAllocation(source1, consumer1, 60);
        System.out.println("After updating, C1 allocation from S1: " + reverseMap.getAllocation(source1, consumer1));

        // Remove a specific allocation
        reverseMap.removeAllocation(source1, consumer1);
        System.out.println("After removing C1 from S1, consumers on S1: " + reverseMap.getAllocations(source1));

        // Remove all allocations for S2
        reverseMap.removeAllocations(source2);
        System.out.println("After removing S2, consumers on S2: " + reverseMap.getAllocations(source2)); // Expected: []
    }
}
