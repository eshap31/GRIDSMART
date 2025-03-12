package com.example.gridsmart;

import java.util.ArrayList;
import java.util.List;

/*
  implements the Modified First Fit Descending (MFFD) algorithm
  for allocating energy from sources to consumers
 */
public class OfflineAllocationAlgorithm
{

    private final EnergyAllocationManager allocationManager;

    // builder
    public OfflineAllocationAlgorithm(EnergyAllocationManager allocationManager)
    {
        this.allocationManager = allocationManager;
    }

    /*
     * Executes the MFFD algorithm to allocate energy
     * consumerQueue -  Queue of consumers sorted by priority and demand
     * sourceQueue - Queue of sources sorted by available energy
     */
    public void execute(EnergyConsumerQueue consumerQueue, EnergySourceQueue sourceQueue) {
        // create copies of the queues
        EnergyConsumerQueue consumerTemp = cloneConsumerQueue(consumerQueue);
        EnergySourceQueue sourceTemp = cloneSourceQueue(sourceQueue);

        // process until no more consumers or sources
        while (!consumerTemp.isEmpty() && !sourceTemp.isEmpty())
        {
            // get the highest priority consumer in the queue
            EnergyConsumer consumer = consumerTemp.poll();

            if (consumer.getRemainingDemand() > 0)
            {
                // try to allocate energy to this consumer
                boolean fullyAllocated = allocateEnergyToConsumer(consumer, sourceTemp);

                // if consumer wasn't fully allocated, add to the queue for later processing
                if (!fullyAllocated && consumer.getRemainingDemand() > 0) {
                    consumerTemp.add(consumer);
                }
            }
        }

        // update the original queues with updated allocations
        updateOriginalQueues(consumerQueue, sourceQueue, consumerTemp, sourceTemp);
    }

    /*
     * allocates energy to a specific consumer from available sources.
     * return true if consumer is fully allocated, false otherwise
     */
    private boolean allocateEnergyToConsumer(EnergyConsumer consumer, EnergySourceQueue sourceQueue) {
        // Calculate remaining demand
        double remainingDemand = consumer.getRemainingDemand();

        // If already fully allocated, nothing to do
        if (remainingDemand <= 0) {
            return true;
        }

        // If no sources available, can't allocate
        if (sourceQueue.isEmpty()) {
            return false;
        }

        // Temp storage for sources we examine
        List<EnergySource> examinedSources = new ArrayList<>();
        boolean consumerFullyAllocated = false;

        // Continue until consumer is satisfied or no more sources
        while (!sourceQueue.isEmpty() && !consumerFullyAllocated)
        {
            // Get largest available source
            EnergySource source = sourceQueue.poll();

            if(source.getStatus())
            {
                double availableEnergy = source.getAvailableEnergy();

                // if source has energy then allocate
                if (availableEnergy > 0) {
                    // determine allocation amount
                    double allocatedAmount = Math.min(availableEnergy, remainingDemand);

                    // create allocation in the manager
                    allocationManager.addAllocation(consumer, source, allocatedAmount);

                    // update remaining demand
                    remainingDemand = consumer.getRemainingDemand();

                    // check if consumer is now fully allocated
                    if (remainingDemand <= 0)
                    {
                        consumerFullyAllocated = true;
                    }
                }

                // If source still has energy, keep it for future allocations
                if (source.getAvailableEnergy() > 0) {
                    examinedSources.add(source);
                }
            }
        }

        // Return examined sources to the queue
        sourceQueue.addAll(examinedSources);

        return consumerFullyAllocated;
    }

    /*
     creates a clone of the consumer queue for processing.
     */
    private EnergyConsumerQueue cloneConsumerQueue(EnergyConsumerQueue original) {
        EnergyConsumerQueue clone = new EnergyConsumerQueue();
        EnergyConsumerQueue temp = new EnergyConsumerQueue();

        // Extract all consumers
        while (!original.isEmpty()) {
            EnergyConsumer consumer = original.poll();
            temp.add(consumer);
        }

        // Add to clone and restore original
        while (!temp.isEmpty()) {
            EnergyConsumer consumer = temp.poll();
            clone.add(consumer);
            original.add(consumer);
        }

        return clone;
    }

    /*
     creates a clone of the source queue for processing.
     */
    private EnergySourceQueue cloneSourceQueue(EnergySourceQueue original) {
        EnergySourceQueue clone = new EnergySourceQueue();
        EnergySourceQueue temp = new EnergySourceQueue();

        // Extract all sources
        while (!original.isEmpty()) {
            EnergySource source = original.poll();
            temp.add(source);
        }

        // Add to clone and restore original
        while (!temp.isEmpty()) {
            EnergySource source = temp.poll();
            clone.add(source);
            original.add(source);
        }

        return clone;
    }

    /*
     * updates the original queues to reflect allocations.
     */
    private void updateOriginalQueues(EnergyConsumerQueue consumerQueue, EnergySourceQueue sourceQueue, EnergyConsumerQueue workingConsumerQueue, EnergySourceQueue workingSourceQueue) {

        // Clear and rebuild consumer queue
        consumerQueue.clear();
        while (!workingConsumerQueue.isEmpty()) {
            consumerQueue.add(workingConsumerQueue.poll());
        }

        // Clear and rebuild source queue
        sourceQueue.clear();
        while (!workingSourceQueue.isEmpty()) {
            sourceQueue.add(workingSourceQueue.poll());
        }
    }
}