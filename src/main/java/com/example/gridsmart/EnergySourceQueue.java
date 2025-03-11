package com.example.gridsmart;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class EnergySourceQueue extends PriorityQueue<EnergySource>
{
    // hash map for fast lookups of sources
    private final Map<String, EnergySource> sourceMap;

    // Comparator for max heap - the source with the
    // source with the highest available energy will be at the top
    private static final Comparator<EnergySource> energyComparator =
            Comparator.comparingDouble(EnergySource::getAvailableEnergy).reversed(); // .reversed() for max heap

    // builder method
    public EnergySourceQueue()
    {
        // initialize the priority queue with the custom comparator
        super(energyComparator);
        this.sourceMap=new HashMap<>();
    }

//_____________________________________________________________________________________________________________________
    // override functions to optimize heap operations
    // usage of hashmap to optimize remove and update operations


    // Override add() to insert into both PriorityQueue & HashMap
    @Override
    public boolean add(EnergySource source)
    {
        sourceMap.put(source.getId(), source);  // store in HashMap
        return super.add(source);  // add to PriorityQueue
    }

    // Optimized remove() - O(log n) instead of O(n)
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

// _____________________________________________________________________________________________________________________
    // heap functions

    // retrieve and remove the highest-energy source
    public EnergySource pollHighestEnergySource()
    {
        // remove from max queue (log n)
        EnergySource highest = super.poll();
        if (highest != null)
        {
            // keep hashmap in sync
            sourceMap.remove(highest.getId());
        }
        return highest;
    }

    // retrieve but do not remove the highest-energy source
    public EnergySource peekHighestEnergySource()
    {
        // O(1)
        return super.peek();
    }

    // update a source's energy availability
    // and then reinsert to maintain heap order
    public void updateSource(String sourceId, double newAvailableEnergy)
    {
        // log(n) lookup in hashmap
        EnergySource source = sourceMap.get(sourceId);

        if (source != null) {
            // log(n) removal from heap
            super.remove(source);
            // update energy value
            source.setAvailableEnergy(newAvailableEnergy);
            // log(n) insertion into heap
            super.add(source);
        }
    }

    // Clear all data
    @Override
    public void clear() {
        super.clear();
        sourceMap.clear();
    }

}
