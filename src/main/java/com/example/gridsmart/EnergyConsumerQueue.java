package com.example.gridsmart;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/*
save all the energy consumers in a priority queue
the highest priority consumer will be at the top
 */
public class EnergyConsumerQueue extends PriorityQueue<EnergyConsumer>
{
    // hash map for fast lookups of consumers
    private final Map<String, EnergyConsumer> consumerMap;

    // comparator for combined sorting
    // first sort by priority - ascending (lower number is higher priority)
    // then by remaining demand (demand-allocatedEnergy) descending  (higher demand is higher priority)
    private static final Comparator<EnergyConsumer> comparator =
            Comparator.comparingInt(EnergyConsumer::getPriority)
                    .thenComparingDouble((c) -> -c.getRemainingDemand()); // Negative for descending order

    // builder method
    public EnergyConsumerQueue()
    {
        super(comparator);
        this.consumerMap = new HashMap<>();
    }

//_____________________________________________________________________________________________________________________
    // override functions to optimize heap operations
    // usage of hashmap to optimize remove and update operations

    // Override add() to insert into both PriorityQueue & HashMap
    @Override
    public boolean add(EnergyConsumer consumer) {
        // Store in HashMap
        consumerMap.put(consumer.getId(), consumer);
        // add to PriorityQueue
        return super.add(consumer);
    }

    // optimized remove() - log n
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

//_____________________________________________________________________________________________________________________

    // Optimized updateConsumer - log n instead of O(n)
    public void updateConsumer(String consumerId, double newAllocatedEnergy) {
        // O(1) lookup
        EnergyConsumer consumer = consumerMap.get(consumerId);
        if (consumer != null) {
            // log n removal from heap
            super.remove(consumer);
            // update allocated energy
            consumer.setAllocatedEnergy(newAllocatedEnergy);
            // log n reinsertion into heap
            super.add(consumer);
        }
    }

    // retrieve and remove the highest-priority consumer
    public EnergyConsumer pollHighestPriorityConsumer()
    {
        // log n
        EnergyConsumer highest = super.poll();
        if (highest != null) {
            // Keep HashMap in sync
            consumerMap.remove(highest.getId());
        }
        return highest;
    }

    // Retrieve but do not remove the highest-priority consumer
    public EnergyConsumer peekHighestPriorityConsumer()
    {
        // O(1)
        return super.peek();
    }

    // Clear all data
    @Override
    public void clear() {
        super.clear();
        consumerMap.clear();
    }
}
