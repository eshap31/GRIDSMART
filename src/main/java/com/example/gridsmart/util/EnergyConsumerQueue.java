package com.example.gridsmart.util;

import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergyNode;
import com.example.gridsmart.model.NodeType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/*
 * A priority queue for energy consumers, with the highest priority consumer
 * at the top of the queue. Updated to work with the graph-based model.
 */
public class EnergyConsumerQueue extends PriorityQueue<EnergyConsumer> {

    // HashMap for fast lookups of consumers
    private final Map<String, EnergyConsumer> consumerMap;

    // Comparator for combined sorting:
    // 1. Sort by priority - ascending (lower number is higher priority)
    // 2. Then by remaining demand (demand-allocatedEnergy) descending (higher demand is higher priority)
    private static final Comparator<EnergyConsumer> comparator =
            Comparator.comparingInt(EnergyConsumer::getPriority)
                    .thenComparingDouble((c) -> -c.getRemainingDemand()); // Negative for descending order

    /*
     * Creates a new empty consumer queue
     */
    public EnergyConsumerQueue() {
        super(comparator);
        this.consumerMap = new HashMap<>();
    }

    /*
     * Overrides add() to insert into both PriorityQueue & HashMap
     */
    @Override
    public boolean add(EnergyConsumer consumer) {
        // Store in HashMap
        consumerMap.put(consumer.getId(), consumer);
        // Add to PriorityQueue
        return super.add(consumer);
    }

    /*
     * Optimized remove() - O(log n) instead of O(n)
     */
    @Override
    public boolean remove(Object obj) {
        if (obj instanceof EnergyConsumer) {
            EnergyConsumer consumer = (EnergyConsumer) obj;
            if (consumerMap.containsKey(consumer.getId())) {
                // Remove from HashMap
                consumerMap.remove(consumer.getId());
                // Remove from PriorityQueue
                return super.remove(consumer);
            }
        }
        return false;
    }

    /*
     * Optimized updateConsumer - O(log n) instead of O(n)
     */
    public void updateConsumer(String consumerId, double newAllocatedEnergy) {
        // O(1) lookup
        EnergyConsumer consumer = consumerMap.get(consumerId);
        if (consumer != null) {
            // O(log n) removal from heap
            super.remove(consumer);
            // Update allocated energy
            consumer.setAllocatedEnergy(newAllocatedEnergy);
            // O(log n) reinsertion into heap
            super.add(consumer);
        }
    }

    /*
     * Retrieves and removes the highest-priority consumer
     */
    public EnergyConsumer pollHighestPriorityConsumer() {
        // O(log n)
        EnergyConsumer highest = super.poll();
        if (highest != null) {
            // Keep HashMap in sync
            consumerMap.remove(highest.getId());
        }
        return highest;
    }

    /*
     * Retrieves but does not remove the highest-priority consumer
     */
    public EnergyConsumer peekHighestPriorityConsumer() {
        // O(1)
        return super.peek();
    }

    /*
     * Clears all data
     */
    @Override
    public void clear() {
        super.clear();
        consumerMap.clear();
    }

    /*
     * Updates the queue from the graph
     */
    public void updateFromGraph(Graph graph) {
        // Create a new queue to rebuild
        EnergyConsumerQueue newQueue = new EnergyConsumerQueue();

        // Get all consumer nodes from the graph
        for (EnergyNode node : graph.getNodesByType(NodeType.CONSUMER)) {
            if (node instanceof EnergyConsumer) {
                EnergyConsumer consumer = (EnergyConsumer) node;
                newQueue.add(consumer);
            }
        }

        // Clear this queue
        this.clear();

        // Add all from the new queue
        this.addAll(newQueue);
    }

    /*
     * Creates a queue from nodes in a graph
     */
    public static EnergyConsumerQueue fromGraph(Graph graph) {
        EnergyConsumerQueue queue = new EnergyConsumerQueue();

        // Get all consumer nodes from the graph
        for (EnergyNode node : graph.getNodesByType(NodeType.CONSUMER)) {
            if (node instanceof EnergyConsumer) {
                EnergyConsumer consumer = (EnergyConsumer) node;
                queue.add(consumer);
            }
        }

        return queue;
    }
}