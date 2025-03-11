package com.example.gridsmart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// HashMap<EnergySource, HashMap<EnergyConsumer, Allocation>>
// stores for each energy sorce, all the consumers that depend on it
public class ReverseAllocationMap extends HashMap<EnergySource, HashMap<EnergyConsumer, Allocation>>
{
    // builder
    public ReverseAllocationMap() {
        super();
    }

    // add an allocation (tracks consumers depending on a source)
    public void addAllocation(EnergySource source, EnergyConsumer consumer, double amount) {
        this.computeIfAbsent(source, k -> new HashMap<EnergyConsumer, Allocation>())
                .put(consumer, new Allocation(source, consumer, amount));
    }

    // get all consumers that rely on a specific source
    public Map<EnergyConsumer, Allocation> getAllocations(EnergySource source) {
        // Use containsKey check to avoid type issues with Collections.emptyMap()
        return this.containsKey(source) ? this.get(source) : Collections.<EnergyConsumer, Allocation>emptyMap();
    }

    // retrieve a specific consumer's allocation from a source
    public Allocation getAllocation(EnergySource source, EnergyConsumer consumer) {
        // Handle null case explicitly
        Map<EnergyConsumer, Allocation> consumerMap = this.get(source);
        return (consumerMap != null) ? consumerMap.get(consumer) : null;
    }

    // remove all allocations for a given source
    public void removeAllocations(EnergySource source) {
        this.remove(source);
    }

    // remove a specific consumer's allocation from a source
    public void removeAllocation(EnergySource source, EnergyConsumer consumer) {
        Map<EnergyConsumer, Allocation> allocations = this.get(source);
        if (allocations != null) {
            allocations.remove(consumer);
            if (allocations.isEmpty()) {
                this.remove(source);  // Clean up empty sources
            }
        }
    }

    // update an existing allocation amount in O(1)
    public void updateAllocation(EnergySource source, EnergyConsumer consumer, double newAmount) {
        Map<EnergyConsumer, Allocation> allocations = this.get(source);
        if (allocations != null && allocations.containsKey(consumer)) {
            allocations.get(consumer).setAllocatedEnergy(newAmount);
        }
    }
}
