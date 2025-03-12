package com.example.gridsmart;

import java.util.HashMap;
import java.util.Map;

 /*
 class that makes sure that the EnergyAllocationMap
 and the reverseAllocationMap stay synchronized
 all functions, and changes to these data structures
 will be done through this structure
  */
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
         Allocation allocation = new Allocation(source, consumer, amount);
         allocationMap.addAllocation(consumer, source, allocation);
         reverseAllocationMap.addAllocation(source, consumer, allocation);

         // Update the consumer and source states
         consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() + amount);
         source.setCurrentLoad(source.getCurrentLoad() + amount);
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
         double totalAllocated = 0;

         for (Map.Entry<EnergySource, Allocation> entry : allocations.entrySet()) {
             EnergySource source = entry.getKey();
             Allocation allocation = entry.getValue();

             // Update source load
             source.setCurrentLoad(source.getCurrentLoad() - allocation.getAllocatedEnergy());

             // Track total energy being deallocated
             totalAllocated += allocation.getAllocatedEnergy();

             // Remove from reverse map
             reverseAllocationMap.removeAllocation(source, consumer);
         }

         // Update consumer's allocated energy
         consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() - totalAllocated);

         // Remove from main map
         allocationMap.removeAllocations(consumer);
     }

     // Remove a specific allocation
     public void removeAllocation(EnergyConsumer consumer, EnergySource source) {
         Allocation allocation = allocationMap.getAllocation(consumer, source);
         if (allocation != null) {
             double amount = allocation.getAllocatedEnergy();

             // Update consumer and source states
             consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() - amount);
             source.setCurrentLoad(source.getCurrentLoad() - amount);

             // Remove from both maps
             allocationMap.removeAllocation(consumer, source);
             reverseAllocationMap.removeAllocation(source, consumer);
         }
     }

     // Update an allocation (keeps both maps in sync)
     public void updateAllocation(EnergyConsumer consumer, EnergySource source, double newAmount) {
         Allocation allocation = allocationMap.getAllocation(consumer, source);
         if (allocation != null) {
             double oldAmount = allocation.getAllocatedEnergy();
             double difference = newAmount - oldAmount;

             // Update allocation amount
             allocation.setAllocatedEnergy(newAmount);

             // Both maps reference the same Allocation object, so we only need to update it once

             // Update the consumer and source states
             consumer.setAllocatedEnergy(consumer.getAllocatedEnergy() + difference);
             source.setCurrentLoad(source.getCurrentLoad() + difference);
         }
     }

     // Check if a consumer is fully allocated
     public boolean isFullyAllocated(EnergyConsumer consumer) {
         Map<EnergySource, Allocation> allocations = allocationMap.getAllocations(consumer);
         double totalAllocated = 0;

         for (Allocation allocation : allocations.values()) {
             totalAllocated += allocation.getAllocatedEnergy();
         }

         return totalAllocated >= consumer.getDemand();
     }

     // Get all consumers in the system
     public Map<String, EnergyConsumer> getAllConsumers() {
         Map<String, EnergyConsumer> result = new HashMap<>();
         for (EnergyConsumer consumer : allocationMap.keySet()) {
             result.put(consumer.getId(), consumer);
         }
         return result;
     }

 }
