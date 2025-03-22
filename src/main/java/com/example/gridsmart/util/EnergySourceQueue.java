package com.example.gridsmart.util;

import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyNode;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.NodeType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/*
 * A priority queue for energy sources, with the source having the highest
 * available energy at the top. Updated to work with the graph-based model.
 */
public class EnergySourceQueue extends PriorityQueue<EnergySource> {

    // HashMap for fast lookups of sources
    private final Map<String, EnergySource> sourceMap;

    // Comparator for max heap - the source with the
    // highest available energy will be at the top
    private static final Comparator<EnergySource> energyComparator =
            Comparator.comparingDouble(EnergySource::getAvailableEnergy).reversed(); // .reversed() for max heap

    /*
     * Creates a new empty source queue
     */
    public EnergySourceQueue() {
        // Initialize the priority queue with the custom comparator
        super(energyComparator);
        this.sourceMap = new HashMap<>();
    }

    /*
     * Overrides add() to insert into both PriorityQueue & HashMap
     */
    @Override
    public boolean add(EnergySource source) {
        sourceMap.put(source.getId(), source);  // Store in HashMap
        return super.add(source);  // Add to PriorityQueue
    }

    /*
     * Optimized remove() - O(log n) instead of O(n)
     */
    @Override
    public boolean remove(Object obj) {
        if (obj instanceof EnergySource) {
            EnergySource source = (EnergySource) obj;
            if (sourceMap.containsKey(source.getId())) {
                sourceMap.remove(source.getId());  // Remove from HashMap
                return super.remove(source);  // Remove from PriorityQueue
            }
        }
        return false;
    }

    /*
     * Retrieves and removes the highest-energy source
     */
    public EnergySource pollHighestEnergySource() {
        // Remove from max queue (O(log n))
        EnergySource highest = super.poll();
        if (highest != null) {
            // Keep HashMap in sync
            sourceMap.remove(highest.getId());
        }
        return highest;
    }

    /*
     * Retrieves but does not remove the highest-energy source
     */
    public EnergySource peekHighestEnergySource() {
        // O(1)
        return super.peek();
    }

    /*
     * Updates a source's energy availability
     * and then reinserts to maintain heap order
     */
    public void updateSource(String sourceId, double newAvailableEnergy) {
        // O(1) lookup in HashMap
        EnergySource source = sourceMap.get(sourceId);

        if (source != null) {
            // O(log n) removal from heap
            super.remove(source);
            // Update energy value
            source.setAvailableEnergy(newAvailableEnergy);
            // O(log n) insertion into heap
            super.add(source);
        }
    }

    /*
     * Clears all data
     */
    @Override
    public void clear() {
        super.clear();
        sourceMap.clear();
    }

    /*
     * Updates the queue from the graph
     */
    public void updateFromGraph(Graph graph) {
        // Create a new queue to rebuild
        EnergySourceQueue newQueue = new EnergySourceQueue();

        // Get all source nodes from the graph
        for (EnergyNode node : graph.getNodesByType(NodeType.SOURCE)) {
            if (node instanceof EnergySource) {
                EnergySource source = (EnergySource) node;
                newQueue.add(source);
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
    public static EnergySourceQueue fromGraph(Graph graph) {
        EnergySourceQueue queue = new EnergySourceQueue();

        // Get all source nodes from the graph
        for (EnergyNode node : graph.getNodesByType(NodeType.SOURCE)) {
            if (node instanceof EnergySource) {
                EnergySource source = (EnergySource) node;
                queue.add(source);
            }
        }

        return queue;
    }
}