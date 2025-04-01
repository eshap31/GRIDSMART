package com.example.gridsmart.dynamic;

import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.util.EnergyConsumerQueue;
import com.example.gridsmart.util.EnergySourceQueue;

import java.util.List;

public class GreedyReallocator {
    private final EnergyAllocationManager allocationManager;
    private final EnergyConsumerQueue consumerQueue;
    private final EnergySourceQueue sourceQueue;

    private final SelectiveDeallocator selectiveDeallocator;

    public GreedyReallocator(EnergyAllocationManager allocationManager, EnergyConsumerQueue consumerQueue, EnergySourceQueue sourceQueue, SelectiveDeallocator selectiveDeallocator) {
        this.allocationManager = allocationManager;
        this.consumerQueue = consumerQueue;
        this.sourceQueue = sourceQueue;
        this.selectiveDeallocator = selectiveDeallocator;
    }

    // reallocate energy to the affected consumers
    public int reallocate(List<EnergyConsumer> consumersToReallocate) {
        int reallocatedConsumers = 0;

        // sort consumers in consumerToReallocate by priority
        // use heap sort
        EnergyConsumerQueue tempQueue = new EnergyConsumerQueue();
        for (EnergyConsumer consumer : consumersToReallocate) {
            tempQueue.add(consumer);
        }

        // process the consumers by priority
        while (!tempQueue.isEmpty()) {
            // get the highest priority consumer
            EnergyConsumer consumer = tempQueue.pollHighestPriorityConsumer();

            // get the remaining energy needed
            double energyNeeded = consumer.getRemainingDemand();

            if (energyNeeded > 0)
            {
                System.out.println("Reallocating for consumer " + consumer.getId() +
                        " (priority " + consumer.getPriority() +
                        ") - Needs " + energyNeeded + " energy");

                // try to allocate energy to this consumer from available sources
                double allocatedInThisRound = allocateEnergyToConsumer(consumer, energyNeeded);

                // check if consumer was fully satisfied
                if (Math.abs(consumer.getRemainingDemand()) < 0.001) {
                    reallocatedConsumers++;
                    System.out.println("Consumer " + consumer.getId() + " fully satisfied");
                }
                else {
                    System.out.println("Consumer " + consumer.getId() +
                            " partially satisfied - still needs " +
                            consumer.getRemainingDemand() + " energy");
                }
            }

            else
            {
                reallocatedConsumers++;
            }

        }



        return reallocatedConsumers;
    }

    // allocate energy to the consumer from available sources
    public double allocateEnergyToConsumer(EnergyConsumer consumer, double energyNeeded)
    {
        double totalAllocated = 0;

        // create temporary queue of the energy sources that could allocate energy
        EnergySourceQueue tempSourceQueue = new EnergySourceQueue();
        for(EnergySource source : sourceQueue)
        {
            if(source.isActive() && source.getAvailableEnergy() > 0)
            {
                tempSourceQueue.add(source);
            }
        }

        // try to allocate energy from the sources
        while(!tempSourceQueue.isEmpty() && energyNeeded > 0)
        {
            // get the source with the most available energy
            EnergySource source = tempSourceQueue.pollHighestEnergySource();

            // make sure source is active
            if(source.isActive())
            {
                double availableEnergy = source.getAvailableEnergy();
                // make sure source has available energy
                if(availableEnergy > 0)
                {
                    // determine how much energy to allocate from current source
                    double toAllocate = Math.min(availableEnergy, energyNeeded);

                    System.out.println("  Allocating " + toAllocate + " energy from source " +
                            source.getId() + " to consumer " + consumer.getId());

                    // update allocation in the allocation manager
                    allocationManager.addAllocation(consumer, source, toAllocate);

                    totalAllocated += toAllocate;
                    energyNeeded -= toAllocate;
                }
            }

        }

        // Check if we've fully satisfied the consumer
        double remainingNeeded = energyNeeded - totalAllocated;
        // if consumers still needs energy and is of high priority (2 or 1)
        if(remainingNeeded > 0 && consumer.getPriority() <=2)
        {
            System.out.println("Greedy allocation insufficient for high-priority consumer " +
                    consumer.getId() + ". Attempting selective deallocation.");

            // try to deallocate energy from lower priority consumers
            double deallocatedEnergy = selectiveDeallocator.deallocateEnergy(consumer, remainingNeeded);

            if (deallocatedEnergy > 0) {
                // reattempt allocation with newly available energy
                System.out.println("Freed up " + deallocatedEnergy +
                        " energy through deallocation. Reallocating to " + consumer.getId());

                // create a new queue with sources that now have available energy
                tempSourceQueue = new EnergySourceQueue();
                for(EnergySource source : sourceQueue) {
                    if(source.isActive() && source.getAvailableEnergy() > 0) {
                        tempSourceQueue.add(source);
                    }
                }

                // try to allocate the newly available energy
                double additionalAllocated = 0;
                double additionalNeeded = remainingNeeded;

                while(!tempSourceQueue.isEmpty() && additionalNeeded > 0) {
                    // get the source with the most available energy
                    EnergySource source = tempSourceQueue.pollHighestEnergySource();

                    if(source.isActive()) {
                        double availableEnergy = source.getAvailableEnergy();
                        if(availableEnergy > 0) {
                            double toAllocate = Math.min(availableEnergy, additionalNeeded);

                            System.out.println("  Allocating " + toAllocate +
                                    " deallocated energy from source " +
                                    source.getId() + " to consumer " + consumer.getId());

                            allocationManager.addAllocation(consumer, source, toAllocate);

                            additionalAllocated += toAllocate;
                            additionalNeeded -= toAllocate;
                        }
                    }
                }

                totalAllocated += additionalAllocated;
            }
        }


        return totalAllocated;
    }
}
