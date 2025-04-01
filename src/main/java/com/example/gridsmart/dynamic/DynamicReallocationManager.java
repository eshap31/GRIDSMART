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

    // In DynamicReallocationManager.java, modify the handleSourceFailure method:

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
            this.allocationManager.removeAllocation(consumer, source);
        }

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
}
