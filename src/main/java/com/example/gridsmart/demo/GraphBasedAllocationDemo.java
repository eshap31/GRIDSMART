package com.example.gridsmart.demo;

import com.example.gridsmart.graph.Allocation;
import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.graph.GraphEdge;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;
import com.example.gridsmart.util.EnergyConsumerQueue;
import com.example.gridsmart.util.EnergySourceQueue;

/**
 * Demonstrates how to use the updated graph-based energy allocation system.
 */
public class GraphBasedAllocationDemo {

    public static void main(String[] args) {
        System.out.println("============= GRID SMART GRAPH-BASED DEMO =============");

        // 1. Create the graph and core components
        Graph energyGraph = new Graph();
        EnergyAllocationManager allocationManager = new EnergyAllocationManager(energyGraph);
        EnergySourceQueue sourceQueue = new EnergySourceQueue();
        EnergyConsumerQueue consumerQueue = new EnergyConsumerQueue();

        // 2. Create some energy sources
        EnergySource solar = new EnergySource("Solar1", 500, SourceType.SOLAR);
        EnergySource wind = new EnergySource("Wind1", 300, SourceType.WIND);
        EnergySource hydro = new EnergySource("Hydro1", 600, SourceType.HYDRO);

        // 3. Create some energy consumers
        EnergyConsumer hospital = new EnergyConsumer("Hospital", 1, 400);  // Highest priority
        EnergyConsumer dataCenter = new EnergyConsumer("DataCenter", 2, 350);
        EnergyConsumer residential = new EnergyConsumer("Residential", 3, 250);

        // 4. Add nodes to the graph
        energyGraph.addNode(solar);
        energyGraph.addNode(wind);
        energyGraph.addNode(hydro);
        energyGraph.addNode(hospital);
        energyGraph.addNode(dataCenter);
        energyGraph.addNode(residential);

        // 5. Add sources and consumers to their queues
        sourceQueue.add(solar);
        sourceQueue.add(wind);
        sourceQueue.add(hydro);
        consumerQueue.add(hospital);
        consumerQueue.add(dataCenter);
        consumerQueue.add(residential);

        // 6. Check initial state
        System.out.println("\n--- Initial state ---");
        System.out.println("Highest priority consumer: " + consumerQueue.peekHighestPriorityConsumer().getId());
        System.out.println("Source with most available energy: " + sourceQueue.peekHighestEnergySource().getId()
                + " (" + sourceQueue.peekHighestEnergySource().getAvailableEnergy() + " kW)");
        System.out.println("Graph structure:\n" + energyGraph.toString());

        // 7. Perform allocations using the allocation manager
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

        // 8. Update queues from the graph
        sourceQueue.updateFromGraph(energyGraph);
        consumerQueue.updateFromGraph(energyGraph);

        // 9. Check state after allocations
        System.out.println("\n--- After allocations ---");
        System.out.println("Highest priority consumer: " + consumerQueue.peekHighestPriorityConsumer().getId());
        System.out.println("Source with most available energy: " + sourceQueue.peekHighestEnergySource().getId()
                + " (" + sourceQueue.peekHighestEnergySource().getAvailableEnergy() + " kW)");

        // 10. Test allocation queries through the graph
        System.out.println("\n--- Graph-based allocation queries ---");

        System.out.println("Hospital's allocations from graph edges:");
        for (GraphEdge edge : energyGraph.getIncomingEdges(hospital.getId())) {
            if (edge.getSource() instanceof EnergySource) {
                System.out.println("  - From " + edge.getSource().getId() + ": " + edge.getFlow() + " kW");
            }
        }

        System.out.println("\nConsumers using Hydro1 from graph edges:");
        for (GraphEdge edge : energyGraph.getOutgoingEdges(hydro.getId())) {
            if (edge.getTarget() instanceof EnergyConsumer) {
                System.out.println("  - " + edge.getTarget().getId() + ": " + edge.getFlow() + " kW");
            }
        }

        // 11. Test updating an allocation via the manager
        System.out.println("\n--- Updating an allocation ---");
        System.out.println("Original allocation from Solar1 to Hospital: " +
                allocationManager.getAllocation(hospital, solar).getAllocatedEnergy() + " kW");

        allocationManager.updateAllocation(hospital, solar, 250);
        System.out.println("Updated allocation: " +
                allocationManager.getAllocation(hospital, solar).getAllocatedEnergy() + " kW");

        // Verify the graph was updated
        GraphEdge edge = energyGraph.getEdge(solar.getId(), hospital.getId());
        System.out.println("Corresponding graph edge flow: " + edge.getFlow() + " kW");

        // 12. Check if consumers are fully allocated
        System.out.println("\n--- Checking allocation status ---");
        System.out.println("Is Hospital fully allocated? " + allocationManager.isFullyAllocated(hospital));
        System.out.println("Is DataCenter fully allocated? " + allocationManager.isFullyAllocated(dataCenter));

        // 13. Demonstrate building a graph from existing allocations
        System.out.println("\n--- Rebuilding graph from allocations ---");
        Graph newGraph = new Graph();
        EnergyAllocationManager newManager = new EnergyAllocationManager(newGraph);

        // Copy allocations to the new manager
        for (EnergyConsumer consumer : new EnergyConsumer[]{hospital, dataCenter, residential}) {
            for (EnergySource source : new EnergySource[]{solar, wind, hydro}) {
                Allocation allocation = allocationManager.getAllocation(consumer, source);
                if (allocation != null && allocation.getAllocatedEnergy() > 0) {
                    newManager.addAllocation(consumer, source, allocation.getAllocatedEnergy());
                }
            }
        }

        System.out.println("New graph structure:\n" + newGraph.toString());

        System.out.println("\n========== DEMO COMPLETE ==========");
    }
}