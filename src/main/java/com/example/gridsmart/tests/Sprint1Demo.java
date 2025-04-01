package com.example.gridsmart.tests;

import com.example.gridsmart.dynamic.DynamicReallocationManager;
import com.example.gridsmart.events.EventSimulator;
import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;

public class Sprint1Demo {
    public static void main(String[] args) {
        System.out.println("Starting Sprint 1 Demo");

        // Create a graph
        Graph graph = new Graph();

        // Add some energy sources
        EnergySource solar = new EnergySource("source1", 1000, SourceType.SOLAR);
        EnergySource wind = new EnergySource("source2", 800, SourceType.WIND);
        EnergySource hydro = new EnergySource("source3", 1500, SourceType.HYDRO);

        graph.addNode(solar);
        graph.addNode(wind);
        graph.addNode(hydro);

        // Add some energy consumers
        EnergyConsumer hospital = new EnergyConsumer("consumer1", 1, 700); // Priority 1 (highest)
        EnergyConsumer mall = new EnergyConsumer("consumer2", 3, 500);     // Priority 3
        EnergyConsumer school = new EnergyConsumer("consumer3", 2, 900);   // Priority 2

        graph.addNode(hospital);
        graph.addNode(mall);
        graph.addNode(school);

        // Create some connections (edges)
        graph.addEdge(solar.getId(), hospital.getId(), 700);
        graph.addEdge(wind.getId(), mall.getId(), 500);
        graph.addEdge(hydro.getId(), school.getId(), 900);

        // Set up the allocation manager
        EnergyAllocationManager allocationManager = new EnergyAllocationManager(graph);

        // Set up initial allocations
        allocationManager.addAllocation(hospital, solar, 700);
        allocationManager.addAllocation(mall, wind, 500);
        allocationManager.addAllocation(school, hydro, 900);

        // Create the DynamicReallocationManager
        DynamicReallocationManager reallocationManager = new DynamicReallocationManager(graph, allocationManager);

        // Create the EventSimulator with events every 2 seconds
        EventSimulator simulator = new EventSimulator(2000, graph);

        // Connect simulator to reallocation manager
        simulator.setEventHandler(reallocationManager);

        // Start the simulation
        System.out.println("Starting event simulation...");
        simulator.start();

        // Run for 10 seconds
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Stop the simulation
        simulator.stop();

        // Print statistics
        reallocationManager.printStatistics();

        System.out.println("Sprint 1 Demo completed");
    }
}