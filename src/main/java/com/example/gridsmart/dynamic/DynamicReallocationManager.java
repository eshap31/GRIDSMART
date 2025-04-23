package com.example.gridsmart.dynamic;

import com.example.gridsmart.events.*;
import com.example.gridsmart.graph.*;
import com.example.gridsmart.model.*;
import com.example.gridsmart.util.*;

import java.util.ArrayList;
import java.util.HashMap;
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

    // Define a map at class level
    private final Map<EventType, EventHandlerStrategy> eventHandlers = new HashMap<>();

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

        // Set up event handlers
        eventHandlers.put(EventType.SOURCE_FAILURE, this::handleSourceFailure);
        eventHandlers.put(EventType.SOURCE_ADDED, this::handleSourceAdded);
        eventHandlers.put(EventType.CONSUMER_ADDED, this::handleConsumerAdded);
    }

    // Define a functional interface for the handlers
    @FunctionalInterface
    private interface EventHandlerStrategy {
        void handle(Event event);
    }

    @Override
    public void handleEvent(Event event) {
        System.out.println("Received event: " + event.getType() + " - " + event.getEventDescription());
        eventsProcessed++;

        EventHandlerStrategy handler = eventHandlers.getOrDefault(event.getType(),
                e -> System.out.println("WARNING: Unknown event type: " + e.getType()));
        handler.handle(event);

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
        // Get the new source from the event
        List<EnergyNode> nodes = event.getNodes();
        if (nodes.isEmpty() || !(nodes.get(0) instanceof EnergySource)) {
            System.out.println("!!! invalid source added event: no source node provided !!!");
            return;
        }

        EnergySource newSource = (EnergySource) nodes.getFirst();
        System.out.println("Processing new source addition: " + newSource.getId() +
                " (" + newSource.getType() + ") with capacity " + newSource.getCapacity());

        // Add the source to the graph
        graph.addNode(newSource);

        // Create connections to all consumers
        // This allows the source to potentially provide energy to any consumer
        for (EnergyNode node : graph.getNodesByType(NodeType.CONSUMER)) {
            EnergyConsumer consumer = (EnergyConsumer) node;
            graph.addEdge(newSource.getId(), consumer.getId(), newSource.getCapacity());
        }

        // Update the queues
        sourceQueue.updateFromGraph(graph);
        consumerQueue.updateFromGraph(graph);

        // Identify consumers that could benefit from more energy
        List<EnergyConsumer> unsatisfiedConsumers = new ArrayList<>();
        for (EnergyNode node : graph.getNodesByType(NodeType.CONSUMER)) {
            EnergyConsumer consumer = (EnergyConsumer) node;
            if (consumer.getRemainingDemand() > 0) {
                unsatisfiedConsumers.add(consumer);
            }
        }

        System.out.println("Found " + unsatisfiedConsumers.size() +
                " consumers with unmet demand that could benefit from the new source");

        // Use the greedy reallocator to allocate energy from the new source
        if (!unsatisfiedConsumers.isEmpty()) {
            System.out.println("Executing greedy reallocation for consumers with unmet demand");
            int satisfiedConsumers = greedyReallocator.reallocate(unsatisfiedConsumers);

            System.out.println("Reallocation complete. " + satisfiedConsumers +
                    " out of " + unsatisfiedConsumers.size() + " consumers fully satisfied");
        } else {
            System.out.println("All consumers are already fully satisfied - no reallocation needed");
        }

        // Print the new allocation status
        printAllocationStatus();
    }

    private void handleConsumerAdded(Event event) {
        // Get the new consumer from the event
        List<EnergyNode> nodes = event.getNodes();
        if (nodes.isEmpty() || !(nodes.get(0) instanceof EnergyConsumer)) {
            System.out.println("!!! invalid consumer added event: no consumer node provided !!!");
            return;
        }

        EnergyConsumer newConsumer = (EnergyConsumer) nodes.getFirst();
        System.out.println("Processing new consumer addition: " + newConsumer.getId() +
                " (Priority " + newConsumer.getPriority() +
                ") with demand " + newConsumer.getDemand());

        // Add the consumer to the graph
        graph.addNode(newConsumer);

        // Create connections from all sources to this consumer
        for (EnergyNode node : graph.getNodesByType(NodeType.SOURCE)) {
            EnergySource source = (EnergySource) node;
            if (source.isActive()) {
                // Create edge with source's capacity
                graph.addEdge(source.getId(), newConsumer.getId(), source.getCapacity());
            }
        }

        // Update the queues
        sourceQueue.updateFromGraph(graph);
        consumerQueue.updateFromGraph(graph);

        // Attempt to allocate energy to the new consumer
        System.out.println("Attempting to allocate energy to new consumer...");
        double allocated = allocateEnergyToNewConsumer(newConsumer);

        System.out.println("Allocated " + allocated + " energy to new consumer " +
                newConsumer.getId() + " (demand: " + newConsumer.getDemand() + ")");

        // Check if fully satisfied
        if (Math.abs(newConsumer.getRemainingDemand()) < 0.001) {
            System.out.println("Consumer " + newConsumer.getId() + " fully satisfied with available energy");
        } else {
            // Consumer not fully satisfied and has high priority, try selective deallocation
            if (newConsumer.getPriority() <= 2) {
                System.out.println("High-priority consumer not fully satisfied. Attempting selective deallocation...");
                double deallocated = selectiveDeallocator.deallocateEnergy(newConsumer, newConsumer.getRemainingDemand());

                if (deallocated > 0) {
                    // Try to allocate the deallocated energy
                    System.out.println("Deallocated " + deallocated + " energy. Reallocating to consumer...");

                    // Now use greedyReallocator to allocate the deallocated energy
                    double additionalAllocated = allocateEnergyToNewConsumer(newConsumer);

                    System.out.println("Additional " + additionalAllocated +
                            " energy allocated after deallocation");

                    allocated += additionalAllocated;
                } else {
                    System.out.println("No energy could be deallocated from lower priority consumers");
                }
            } else {
                System.out.println("Lower-priority consumer not fully satisfied, but selective deallocation not attempted");
            }
        }

        double fulfillmentPercentage = 0;
        if (newConsumer.getDemand() > 0) {
            fulfillmentPercentage = (allocated / newConsumer.getDemand()) * 100;
        }

        System.out.println("Final allocation for new consumer: " + allocated + "/" +
                newConsumer.getDemand() + " (" + String.format("%.1f", fulfillmentPercentage) + "%)");

        // Print the new allocation status
        printAllocationStatus();
    }

    // Helper method to allocate available energy to the new consumer
    private double allocateEnergyToNewConsumer(EnergyConsumer consumer) {
        double totalAllocated = 0;
        double remainingDemand = consumer.getRemainingDemand();

        if (remainingDemand <= 0) {
            return 0; // No allocation needed
        }

        // Create a temporary queue with all available sources
        EnergySourceQueue tempQueue = new EnergySourceQueue();
        for (EnergyNode node : graph.getNodesByType(NodeType.SOURCE)) {
            EnergySource source = (EnergySource) node;
            if (source.isActive() && source.getAvailableEnergy() > 0) {
                tempQueue.add(source);
            }
        }

        // Allocate energy from each source until demand is met or no more sources available
        while (!tempQueue.isEmpty() && remainingDemand > 0) {
            EnergySource source = tempQueue.pollHighestEnergySource();
            double availableEnergy = source.getAvailableEnergy();

            if (availableEnergy > 0) {
                double allocateAmount = Math.min(availableEnergy, remainingDemand);

                System.out.println("  Allocating " + allocateAmount + " energy from source " +
                        source.getId() + " to consumer " + consumer.getId());

                // Update allocation in the allocation manager
                allocationManager.addAllocation(consumer, source, allocateAmount);

                totalAllocated += allocateAmount;
                remainingDemand -= allocateAmount;
            }
        }

        return totalAllocated;
    }

    private void handleDemandIncrease(Event event) {
        // STUB implementation
        System.out.println("Demand increase event received (not implemented yet)");
    }

    private void handleDemandDecrease(Event event) {
        // STUB implementation
        System.out.println("Demand decrease event received (not implemented yet)");
    }

// ___________________________________________________________________________________________________
// ___________________________________________________________________________________________________
// ___________________________________________________________________________________________________

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
