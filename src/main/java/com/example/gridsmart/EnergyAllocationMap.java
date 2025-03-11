package com.example.gridsmart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EnergyAllocationMap extends HashMap<EnergyConsumer, HashMap<EnergySource, Allocation>>
{
    // builder
    public EnergyAllocationMap() {
        super();
    }

    // add an allocation
    public void addAllocation(EnergyConsumer consumer, EnergySource source, double amount) {
        this.computeIfAbsent(consumer, k -> new HashMap<EnergySource, Allocation>())
                .put(source, new Allocation(source, consumer, amount));
    }

    // get all allocations for a specific consumer
    public Map<EnergySource, Allocation> getAllocations(EnergyConsumer consumer)
    {
        return this.containsKey(consumer) ? this.get(consumer) : Collections.<EnergySource, Allocation>emptyMap();
    }

    // retrieve a specific allocation in O(1) time
    public Allocation getAllocation(EnergyConsumer consumer, EnergySource source)
    {
        Map<EnergySource, Allocation> consumerMap = this.get(consumer);
        return (consumerMap != null) ? consumerMap.get(source) : null;
    }

    // remove all allocations for a consumer
    public void removeAllocations(EnergyConsumer consumer) {
        this.remove(consumer);
    }

    // remove a specific allocation in O(1) time
    public void removeAllocation(EnergyConsumer consumer, EnergySource source) {
        Map<EnergySource, Allocation> allocations = this.get(consumer);
        if (allocations != null) {
            allocations.remove(source);
            if (allocations.isEmpty()) {
                // Clean up empty consumers
                this.remove(consumer);
            }
        }
    }

    // update an existing allocation amount in O(1)
    public void updateAllocation(EnergyConsumer consumer, EnergySource source, double newAmount) {
        Map<EnergySource, Allocation> allocations = this.get(consumer);
        if (allocations != null && allocations.containsKey(source)) {
            Allocation allocation = allocations.get(source);

            double oldAmount = allocation.getAllocatedEnergy();
            double difference = newAmount - oldAmount;

            // update the allocation amount
            allocation.setAllocatedEnergy(newAmount);

            // update the EnergyConsumer’s allocated energy
            consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() + difference);

            // update the EnergySource’s current load
            source.setCurrentLoad(source.getCurrentLoad() + difference);
        }
    }


    // check if a consumer is fully allocated
    public boolean isFullyAllocated(EnergyConsumer consumer) {
        double totalAllocated = this.getAllocations(consumer).values().stream()
                .mapToDouble(Allocation::getAllocatedEnergy)
                .sum();
        return totalAllocated >= consumer.getRemainingDemand();
    }
}