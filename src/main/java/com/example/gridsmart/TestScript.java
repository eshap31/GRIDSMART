package com.example.gridsmart;

// testing script for the GridSmart project
public class TestScript
{
    public static void main(String[] args)
    {
        EnergySourceQueue queue = new EnergySourceQueue();

        // Create EnergySources
        EnergySource source1 = new EnergySource("s1", 100, SourceType.SOLAR);
        EnergySource source2 = new EnergySource("s2", 200, SourceType.NUCLEAR);
        EnergySource source3 = new EnergySource("s3", 150, SourceType.WIND);

        // Add to queue
        queue.add(source1);
        queue.add(source2);
        queue.add(source3);

        System.out.println("Highest Energy Source: " + queue.peekHighestEnergySource()); // Expected: S2

        // Remove an energy source
        queue.remove(source2);
        System.out.println("After removing S2, highest: " + queue.peekHighestEnergySource()); // Expected: S3

        // Update an energy source
        queue.updateSource("s1", 250);
        System.out.println("After updating source1, highest: " + queue.peekHighestEnergySource()); // Expected: S1
    }
}
