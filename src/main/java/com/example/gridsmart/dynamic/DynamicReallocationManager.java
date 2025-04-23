package com.example.gridsmart.dynamic;

import com.example.gridsmart.events.*;
import com.example.gridsmart.graph.*;
import com.example.gridsmart.model.*;
import com.example.gridsmart.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicReallocationManager implements EventHandler{
    // graph that represents the grid
    private final Graph graph;
    private final EnergyAllocationManager allocationManager;

    // queues for greedy reallocation
    private final EnergyConsumerQueue consumerQueue;
    private final EnergySourceQueue sourceQueue;

    private final GreedyReallocator greedyReallocator;
    private final SelectiveDeallocator selectiveDeallocator;


    // Statistics tracking
    private int eventsProcessed = 0;
    private int successfulReallocations = 0;

    // Update constructor
    public DynamicReallocationManager(Graph graph, EnergyAllocationManager allocationManager) {
        this.graph = graph;
        this.allocationManager = allocationManager;

        // Initialize queues from the current graph state
        this.consumerQueue = EnergyConsumerQueue.fromGraph(graph);
        this.sourceQueue = EnergySourceQueue.fromGraph(graph);

        // create selective deallocator with 15% disturbance budget
        this.selectiveDeallocator = new SelectiveDeallocator(allocationManager, 0.15);

        System.out.println("DynamicReallocationManager initialized");

        // Pass selective deallocator to greedy reallocator
        this.greedyReallocator = new GreedyReallocator(
                allocationManager, consumerQueue, sourceQueue, selectiveDeallocator);
    }

    @Override
    public void handleEvent(Event event)
    {
        System.out.println("Received event: " + event.getType() + " - " + event.getEventDescription());
        eventsProcessed++;

        // dispatch to correct handler
        // based on the event type
        switch(event.getType())
        {
            case SOURCE_FAILURE:
                handleSourceFailure(event);
                break;
            /*case SOURCE_ADDED:
                handleSourceAdded(event);
                break;
            case CONSUMER_ADDED:
                handleConsumerAdded(event);
                break;
            case DEMAND_INCREASE:
                handleDemandIncrease(event);
                break;
            case DEMAND_DECREASE:
                handleDemandDecrease(event);
                break;*/
            default:
                System.out.println("WARNING: Unknown event type: " + event.getType());
        }
        event.setHandled(true);
    }

    private void handleSourceFailure(Event event) {
        // identify the source that failed
        List<EnergyNode> nodes = event.getNodes();
        if (nodes.isEmpty() || !(nodes.get(0) instanceof EnergySource)) {
            System.out.println("!!! invalid source failure event: no source node provided !!!");
            return;
        }

        EnergySource source = (EnergySource) nodes.getFirst();
        System.out.println("Processing source failure for: " + source.getId());

        // deactivate the source
        source.deactivate();

        // find affected consumers
        Map<EnergyConsumer, Allocation> affectedConsumerMap = this.allocationManager.getAllocationsForSource(source);

        // create a copy of the affected consumers
        // to avoid concurrent modification
        List<EnergyConsumer> consumersToReallocate = new ArrayList<>(affectedConsumerMap.keySet());

        // log the affected consumers and their lost energy
        for (Map.Entry<EnergyConsumer, Allocation> entry : affectedConsumerMap.entrySet()) {
            EnergyConsumer consumer = entry.getKey();
            Allocation allocation = entry.getValue();

            double lostEnergy = allocation.getAllocatedEnergy();
            System.out.println("Consumer " + consumer.getId() + " lost " + lostEnergy + " energy");
        }

        // remove the source from the graph
        graph.removeNode(source.getId());

        // now handle the allocations
        // this automatically updates allocation maps
        for (EnergyConsumer consumer : consumersToReallocate) {
            double beforeAllocated = consumer.getAllocatedEnergy();
            double beforeRemaining = consumer.getRemainingDemand();

            Allocation allocation = this.allocationManager.getAllocation(consumer, source);
            double allocationAmount = (allocation != null) ? allocation.getAllocatedEnergy() : 0;

            System.out.println("Consumer " + consumer.getId() +
                    " - Before: allocated=" + beforeAllocated +
                    ", remaining=" + beforeRemaining +
                    ", allocation amount=" + allocationAmount);

            this.allocationManager.removeAllocation(consumer, source);

            double afterAllocated = consumer.getAllocatedEnergy();
            double afterRemaining = consumer.getRemainingDemand();

            System.out.println("Consumer " + consumer.getId() +
                    " - After: allocated=" + afterAllocated +
                    ", remaining=" + afterRemaining);
        }


        // Ensure source is completely removed from the allocation manager
        allocationManager.removeSourceCompletely(source);

        // update the queues
        consumerQueue.updateFromGraph(graph);
        sourceQueue.updateFromGraph(graph);

        // execute reallocation
        System.out.println("Executing greedy reallocation for " + consumersToReallocate.size() + " affected consumers");
        int satisfiedConsumers = greedyReallocator.reallocate(consumersToReallocate);

        // update grid state
        consumerQueue.updateFromGraph(graph);
        sourceQueue.updateFromGraph(graph);

        System.out.println("Reallocation complete. " + satisfiedConsumers +
                " out of " + consumersToReallocate.size() + " consumers fully satisfied");

        printAllocationStatus();

        successfulReallocations++;
    }

    private void handleSourceAdded(Event event) {
        // STUB implementation
        System.out.println("Source added event received (not implemented yet)");
    }

    private void handleConsumerAdded(Event event) {
        // STUB implementation
        System.out.println("Consumer added event received (not implemented yet)");
    }

    private void handleDemandIncrease(Event event) {
        // STUB implementation
        System.out.println("Demand increase event received (not implemented yet)");
    }

    private void handleDemandDecrease(Event event) {
        // STUB implementation
        System.out.println("Demand decrease event received (not implemented yet)");
    }

    // functions that will be implemented in next sprint

    /**
     * Determines which reallocation strategy to use based on the event.
     * To be implemented in Sprint 2.
     */
    private ReallocationStrategy selectStrategy(Event event) {
        // Default to greedy for now
        return ReallocationStrategy.GREEDY;
    }

    /**
     * Executes a greedy reallocation algorithm.
     * To be implemented in Sprint 2.
     */
    private boolean executeGreedyReallocation(List<EnergyConsumer> affectedConsumers) {
        System.out.println("Greedy reallocation would be executed here");
        return true; // Stub implementation always succeeds
    }

    /**
     * Executes selective deallocation when greedy allocation is insufficient.
     * To be implemented in Sprint 3.
     */
    private boolean executeSelectiveDeallocation(List<EnergyConsumer> criticalConsumers) {
        System.out.println("Selective deallocation would be executed here");
        return true; // Stub implementation always succeeds
    }

    // Statistics and metrics methods

    public int getEventsProcessed() {
        return eventsProcessed;
    }

    public int getSuccessfulReallocations() {
        return successfulReallocations;
    }

    public GreedyReallocator getGreedyReallocator() {
        return greedyReallocator;
    }

    public void printStatistics() {
        System.out.println("===== Dynamic Reallocation Statistics =====");
        System.out.println("Events processed: " + eventsProcessed);
        System.out.println("Successful reallocations: " + successfulReallocations);
    }

    public void printAllocationStatus() {
        System.out.println("\n\n\n----- Allocation -----");
        // Print allocations for each consumer
        System.out.println("\n----- Consumer Allocations from EnergyAllocationManager -----");
        Map<String, EnergyConsumer> allConsumers = allocationManager.getAllConsumers();

        for (EnergyConsumer consumer : allConsumers.values()) {
            System.out.printf("Consumer %s (Priority %d, Demand %.2f):%n",
                    consumer.getId(), consumer.getPriority(), consumer.getDemand());

            Map<EnergySource, Allocation> allocations = allocationManager.getAllocationsForConsumer(consumer);

            if (allocations.isEmpty()) {
                System.out.println("  No allocations");
            } else {
                double totalAllocation = 0;

                for (Map.Entry<EnergySource, Allocation> entry : allocations.entrySet()) {
                    EnergySource source = entry.getKey();
                    Allocation allocation = entry.getValue();
                    double amount = allocation.getAllocatedEnergy();

                    System.out.printf("  From %s (%s): %.2f%n",
                            source.getId(), source.getType(), amount);

                    totalAllocation += amount;
                }

                double fulfillmentPercentage = (consumer.getDemand() > 0) ?
                        (totalAllocation / consumer.getDemand()) * 100.0 : 0.0;

                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
                        totalAllocation, consumer.getDemand(), fulfillmentPercentage);
            }
        }

        // Print allocations for each source
        System.out.println("\n----- Source Allocations from EnergyAllocationManager -----");
        Map<String, EnergySource> allSources = allocationManager.getAllSources();

        for (EnergySource source : allSources.values()) {
            System.out.printf("Source %s (%s, Capacity %.2f):%n",
                    source.getId(), source.getType(), source.getCapacity());

            Map<EnergyConsumer, Allocation> allocations = allocationManager.getAllocationsForSource(source);

            if (allocations.isEmpty()) {
                System.out.println("  No consumers allocated");
            } else {
                double totalAllocated = 0;

                for (Map.Entry<EnergyConsumer, Allocation> entry : allocations.entrySet()) {
                    EnergyConsumer consumer = entry.getKey();
                    Allocation allocation = entry.getValue();
                    double amount = allocation.getAllocatedEnergy();

                    System.out.printf("  To %s (Priority %d): %.2f%n",
                            consumer.getId(), consumer.getPriority(), amount);

                    totalAllocated += amount;
                }

                double utilizationPercentage = (source.getCapacity() > 0) ?
                        (totalAllocated / source.getCapacity()) * 100.0 : 0.0;

                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
                        totalAllocated, source.getCapacity(), utilizationPercentage);
            }
        }
        System.out.println("\n\n\n----- Finished printing Allocations -----");
    }
}
