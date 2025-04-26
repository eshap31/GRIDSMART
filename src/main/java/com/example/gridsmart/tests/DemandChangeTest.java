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

public class DemandChangeTest {
    public static void main(String[] args) {
        System.out.println("===== Starting Demand Change Event Test =====");

        // Create a graph with sources and consumers
        Graph graph = new Graph();
        EnergyAllocationManager allocationManager = new EnergyAllocationManager(graph);

        // Create energy sources with varying capacities
        EnergySource solar = new EnergySource("solar1", 500, SourceType.SOLAR);
        EnergySource wind = new EnergySource("wind1", 400, SourceType.WIND);
        EnergySource hydro = new EnergySource("hydro1", 800, SourceType.HYDRO);

        // Add sources to graph
        graph.addNode(solar);
        graph.addNode(wind);
        graph.addNode(hydro);

        // Create consumers with different priorities and initial demands
        EnergyConsumer hospital = new EnergyConsumer("hospital", 1, 400);
        EnergyConsumer dataCenter = new EnergyConsumer("dataCenter", 2, 300);
        EnergyConsumer school = new EnergyConsumer("school", 3, 250);
        EnergyConsumer mall = new EnergyConsumer("mall", 4, 350);
        EnergyConsumer residential = new EnergyConsumer("residential", 5, 200);

        // Add consumers to graph
        graph.addNode(hospital);
        graph.addNode(dataCenter);
        graph.addNode(school);
        graph.addNode(mall);
        graph.addNode(residential);

        // Connect all sources to all consumers
        for (EnergyNode sourceNode : graph.getNodesByType(NodeType.SOURCE)) {
            for (EnergyNode consumerNode : graph.getNodesByType(NodeType.CONSUMER)) {
                graph.addEdge(sourceNode.getId(), consumerNode.getId(),
                        ((EnergySource)sourceNode).getCapacity());
            }
        }

        // Initial allocations - almost fully allocated, leaving some capacity
        System.out.println("\n----- Creating Initial Allocations -----");

        // Hospital (highest priority)
        allocationManager.addAllocation(hospital, solar, 300);
        allocationManager.addAllocation(hospital, wind, 100);
        System.out.println("Hospital allocated 300 from Solar and 100 from Wind");

        // Data center (high priority)
        allocationManager.addAllocation(dataCenter, solar, 200);
        allocationManager.addAllocation(dataCenter, hydro, 100);
        System.out.println("Data Center allocated 200 from Solar and 100 from Hydro");

        // School (medium priority)
        allocationManager.addAllocation(school, wind, 250);
        System.out.println("School allocated 250 from Wind");

        // Mall (low priority)
        allocationManager.addAllocation(mall, hydro, 350);
        System.out.println("Mall allocated 350 from Hydro");

        // Residential (lowest priority)
        allocationManager.addAllocation(residential, hydro, 200);
        System.out.println("Residential allocated 200 from Hydro");

        // Create the reallocation manager
        DynamicReallocationManager reallocationManager = new DynamicReallocationManager(graph, allocationManager);

        // Print initial allocation status
        System.out.println("\n----- Initial Energy Allocation Status -----");
        printAllocationStatus(allocationManager);

        // Calculate initial satisfaction
        System.out.println("\nINITIAL SATISFACTION LEVELS:");
        reportSatisfactionLevels(allocationManager);

        // Create event simulator
        EventSimulator simulator = new EventSimulator(5000, graph);
        simulator.setEventHandler(reallocationManager);

        // PART 1: Test DEMAND_INCREASE for high priority consumer (should trigger selective deallocation)
        System.out.println("\n===== PART 1: Testing DEMAND_INCREASE for High Priority Consumer =====");

        // Manually create a demand increase event for the hospital (highest priority)
        hospital.setDemand(700); // Increase from 400 to 700

        ArrayList<EnergyNode> nodes = new ArrayList<>();
        nodes.add(hospital);

        Event increaseEvent = new Event(
                EventType.DEMAND_INCREASE,
                nodes,
                "Demand increased for hospital from 400.00 to 700.00 (+75.0%)",
                System.currentTimeMillis()
        );

        System.out.println("\nTriggering DEMAND_INCREASE event for high-priority consumer: " +
                increaseEvent.getEventDescription());
        reallocationManager.handleEvent(increaseEvent);

        // Print allocation status after high-priority increase
        System.out.println("\n----- Allocation Status After High-Priority Demand Increase -----");
        printAllocationStatus(allocationManager);

        // Report satisfaction levels
        System.out.println("\nSATISFACTION LEVELS AFTER HIGH-PRIORITY INCREASE:");
        reportSatisfactionLevels(allocationManager);

        // PART 2: Test DEMAND_DECREASE (should free up energy for others)
        System.out.println("\n===== PART 2: Testing DEMAND_DECREASE =====");

        // Manually create a demand decrease event for the data center
        dataCenter.setDemand(150); // Decrease from 300 to 150

        nodes = new ArrayList<>();
        nodes.add(dataCenter);

        Event decreaseEvent = new Event(
                EventType.DEMAND_DECREASE,
                nodes,
                "Demand decreased for dataCenter from 300.00 to 150.00 (-50.0%)",
                System.currentTimeMillis()
        );

        System.out.println("\nTriggering DEMAND_DECREASE event: " +
                decreaseEvent.getEventDescription());
        reallocationManager.handleEvent(decreaseEvent);

        // Print allocation status after demand decrease
        System.out.println("\n----- Allocation Status After Demand Decrease -----");
        printAllocationStatus(allocationManager);

        // Report satisfaction levels
        System.out.println("\nSATISFACTION LEVELS AFTER DEMAND DECREASE:");
        reportSatisfactionLevels(allocationManager);

        // PART 3: Test DEMAND_INCREASE for low priority consumer
        System.out.println("\n===== PART 3: Testing DEMAND_INCREASE for Low Priority Consumer =====");

        // Manually create a demand increase event for the mall (low priority)
        mall.setDemand(500); // Increase from 350 to 500

        nodes = new ArrayList<>();
        nodes.add(mall);

        Event lowPriorityIncreaseEvent = new Event(
                EventType.DEMAND_INCREASE,
                nodes,
                "Demand increased for mall from 350.00 to 500.00 (+42.9%)",
                System.currentTimeMillis()
        );

        System.out.println("\nTriggering DEMAND_INCREASE event for low-priority consumer: " +
                lowPriorityIncreaseEvent.getEventDescription());
        reallocationManager.handleEvent(lowPriorityIncreaseEvent);

        // Print final allocation status
        System.out.println("\n----- Final Allocation Status -----");
        printAllocationStatus(allocationManager);

        // Final satisfaction report
        System.out.println("\nFINAL SATISFACTION LEVELS:");
        reportSatisfactionLevels(allocationManager);

        // Print final statistics
        System.out.println("\n===== Final Statistics =====");
        reallocationManager.printStatistics();
        System.out.println("\nDemand Change Event Test completed");
    }

    // Helper method to print current allocation status
    private static void printAllocationStatus(EnergyAllocationManager allocationManager) {
        System.out.println("CONSUMER STATUS:");
        for (EnergyConsumer consumer : allocationManager.getAllConsumers().values()) {
            double demand = consumer.getDemand();
            double allocated = consumer.getAllocatedEnergy();
            double percentSatisfied = demand > 0 ? (allocated / demand) * 100 : 0;

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

    // Helper method to report satisfaction levels for all consumers
    private static void reportSatisfactionLevels(EnergyAllocationManager allocationManager) {
        // Collect total satisfaction metrics
        int totalConsumers = 0;
        int fullySatisfied = 0;
        int partiallySatisfied = 0;
        int unsatisfied = 0;

        // Group consumers by priority for reporting
        Map<Integer, Integer> priorityTotals = new java.util.HashMap<>();
        Map<Integer, Integer> prioritySatisfied = new java.util.HashMap<>();

        for (EnergyConsumer consumer : allocationManager.getAllConsumers().values()) {
            totalConsumers++;

            int priority = consumer.getPriority();
            double demand = consumer.getDemand();
            double allocated = consumer.getAllocatedEnergy();
            double percentSatisfied = demand > 0 ? (allocated / demand) * 100 : 0;

            // Update totals for this priority
            priorityTotals.put(priority, priorityTotals.getOrDefault(priority, 0) + 1);

            if (Math.abs(allocated - demand) < 0.001) {
                fullySatisfied++;
                prioritySatisfied.put(priority, prioritySatisfied.getOrDefault(priority, 0) + 1);
            } else if (allocated > 0) {
                partiallySatisfied++;
            } else {
                unsatisfied++;
            }

            System.out.printf("%s (Priority %d): %.1f%% satisfied\n",
                    consumer.getId(), priority, percentSatisfied);
        }

        // Print summary statistics
        System.out.println("\nSUMMARY:");
        System.out.printf("Total consumers: %d\n", totalConsumers);
        System.out.printf("Fully satisfied: %d (%.1f%%)\n",
                fullySatisfied, (fullySatisfied * 100.0 / totalConsumers));
        System.out.printf("Partially satisfied: %d (%.1f%%)\n",
                partiallySatisfied, (partiallySatisfied * 100.0 / totalConsumers));
        System.out.printf("Unsatisfied: %d (%.1f%%)\n",
                unsatisfied, (unsatisfied * 100.0 / totalConsumers));

        // Print satisfaction by priority
        System.out.println("\nSATISFACTION BY PRIORITY:");
        for (int i = 1; i <= 5; i++) {
            if (priorityTotals.containsKey(i)) {
                int total = priorityTotals.get(i);
                int satisfied = prioritySatisfied.getOrDefault(i, 0);
                double percent = (satisfied * 100.0 / total);

                System.out.printf("Priority %d: %d/%d (%.1f%%) fully satisfied\n",
                        i, satisfied, total, percent);
            }
        }
    }
}