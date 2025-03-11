package com.example.gridsmart;

import java.util.Map;

// class that makes sure that the EnergyAllocationMap
// and the reverseAllocationMap stay synchronized
// all functions, and changes to these data structures
// will be done through this structure
public class EnergyAllocationManager {

    private final EnergyAllocationMap allocationMap;
    private final ReverseAllocationMap reverseAllocationMap;

    // Constructor
    public EnergyAllocationManager() {
        this.allocationMap = new EnergyAllocationMap();
        this.reverseAllocationMap = new ReverseAllocationMap();
    }

    // Add an allocation (keeps both maps in sync)
    public void addAllocation(EnergyConsumer consumer, EnergySource source, double amount) {
        allocationMap.addAllocation(consumer, source, amount);
        reverseAllocationMap.addAllocation(source, consumer, amount);
    }

    // Get all allocations for a consumer
    public Map<EnergySource, Allocation> getAllocationsForConsumer(EnergyConsumer consumer) {
        return allocationMap.getAllocations(consumer);
    }

    // Get all consumers that rely on a specific source
    public Map<EnergyConsumer, Allocation> getAllocationsForSource(EnergySource source) {
        return reverseAllocationMap.getAllocations(source);
    }

    //  Get a specific allocation for a consumer and source
    public Allocation getAllocation(EnergyConsumer consumer, EnergySource source) {
        return allocationMap.getAllocation(consumer, source);
    }

    // Remove all allocations for a consumer
    public void removeAllocationsForConsumer(EnergyConsumer consumer) {
        Map<EnergySource, Allocation> allocations = allocationMap.getAllocations(consumer);
        for (EnergySource source : allocations.keySet()) {
            reverseAllocationMap.removeAllocation(source, consumer); // Remove from reverse map
        }
        allocationMap.removeAllocations(consumer); // Remove from main map
    }

    // Remove a specific allocation
    public void removeAllocation(EnergyConsumer consumer, EnergySource source) {
        allocationMap.removeAllocation(consumer, source);
        reverseAllocationMap.removeAllocation(source, consumer);
    }

    // Update an allocation (keeps both maps in sync)
    public void updateAllocation(EnergyConsumer consumer, EnergySource source, double newAmount) {
        double oldAmount = allocationMap.getAllocation(consumer, source).getAllocatedEnergy();
        double difference = newAmount - oldAmount;

        // Update allocation in both maps
        allocationMap.updateAllocation(consumer, source, newAmount);
        reverseAllocationMap.updateAllocation(source, consumer, newAmount);

        // Ensure that the consumer's allocatedEnergy and source's currentLoad are updated
        consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() + difference);
        source.setCurrentLoad(source.getCurrentLoad() + difference);
    }

    // Check if a consumer is fully allocated
    public boolean isFullyAllocated(EnergyConsumer consumer) {
        return allocationMap.isFullyAllocated(consumer);
    }

}
