package com.example.gridsmart.dynamic;

import com.example.gridsmart.graph.*;
import com.example.gridsmart.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SelectiveDeallocator {
    private final EnergyAllocationManager allocationManager;
    private double disturbanceBudget; // amount of energy that can be reallocated

    public SelectiveDeallocator(EnergyAllocationManager allocationManager, double disturbanceBudget) {
        this.allocationManager = allocationManager;
        this.disturbanceBudget = disturbanceBudget;
    }

    // find energy consumers that can be deallocated
    // in order to help a higher priority consumer
    public List<DeallocationCandidate> getDeallocationCandidates(EnergyConsumer highPriorityConsumer, double energyNeeded)
    {
        List<DeallocationCandidate> candidates = new ArrayList<>();

        // get all sources in the system
        Map<String, EnergySource> allSources = allocationManager.getAllSources();

        // iterate through all the sources
        for(EnergySource source: allSources.values())
        {
            // if source is active
            if(source.isActive())
            {
                // and has no available energy
                if(source.getAvailableEnergy()==0)
                {
                    // get all the consumers connected to this source
                    Map<EnergyConsumer, Allocation> consumersOfSource = allocationManager.getAllocationsForSource(source);

                    // iterate through all the consumers of this source
                    for (Map.Entry<EnergyConsumer, Allocation> entry : consumersOfSource.entrySet()){
                        EnergyConsumer consumer = entry.getKey();
                        Allocation allocation = entry.getValue();

                        if(!consumer.equals(highPriorityConsumer) && consumer.getPriority()>highPriorityConsumer.getPriority()){
                            // calculate priority difference
                            int priorityDifference = consumer.getPriority() - highPriorityConsumer.getPriority();

                            double deallocatableEnergy = allocation.getAllocatedEnergy();

                            DeallocationCandidate candidate = new DeallocationCandidate(consumer, source, deallocatableEnergy, priorityDifference);

                            candidates.add(candidate);
                        }
                    }
                }
            }
        }

        // sort the candidates by priority difference in descending order
        // lower priority candidates will be first
        candidates.sort((c1, c2) -> Integer.compare(c2.getPriorityDifference(), c1.getPriorityDifference()));

        return candidates;
    }

    // execute deallocation to help a higher priority consumer
    // returns the amount of energy that was deallocated
    public double deallocateEnergy(EnergyConsumer highPriorityConsumer, double energyNeeded){
        // get candidates for deallocation
        List<DeallocationCandidate> candidates = getDeallocationCandidates(highPriorityConsumer, energyNeeded);

        if(candidates.isEmpty()){
            System.out.println("No candidates for deallocation found for: " + highPriorityConsumer.getId());
            return 0.0;
        }

        double totalSystemEnergy = calculateTotalAllocatedEnergy();
        double maxDisturbance = totalSystemEnergy * disturbanceBudget;
        System.out.println("Deallocation budget: " + maxDisturbance +
                " (" + (disturbanceBudget * 100) + "% of " + totalSystemEnergy + ")");

        double energyDeallocated = 0.0;
        int consumersAffected = 0;

        // iterate through the candidates
        for(DeallocationCandidate candidate: candidates){
            // make sure we don't deallocate more than needed
            if(energyDeallocated < energyNeeded && energyDeallocated < maxDisturbance){
                EnergyConsumer lowPriorityConsumer = candidate.getConsumer();
                EnergySource source = candidate.getSource();

                double remainingNeeded = energyNeeded - energyDeallocated;

                // calculate how much energy can be deallocated
                double deallocatedEnergy = Math.min(candidate.getDeallocatableEnergy(), maxDisturbance - energyDeallocated);

                // calculate how much energy to take
                double energyToTake = Math.min(deallocatedEnergy, remainingNeeded);

                if(energyToTake > 0)
                {
                    // Get the current allocation
                    Allocation allocation = allocationManager.getAllocation(lowPriorityConsumer, source);
                    if(allocation != null)
                    {
                        double currentAllocation = allocation.getAllocatedEnergy();
                        double newAllocation = currentAllocation - energyToTake;

                        System.out.println("Deallocating " + energyToTake + " energy from " +
                                lowPriorityConsumer.getId() + " (priority " + lowPriorityConsumer.getPriority() +
                                ") to help " + highPriorityConsumer.getId() +
                                " (priority " + highPriorityConsumer.getPriority() + ")");

                        // update the allocation
                        if (newAllocation > 0) {
                            allocationManager.updateAllocation(lowPriorityConsumer, source, newAllocation);
                        }
                        else {
                            // if completely deallocating, remove the allocation
                            allocationManager.removeAllocation(lowPriorityConsumer, source);
                        }

                        energyDeallocated += energyToTake;
                        consumersAffected++;
                    }
                }
            }
        }
        System.out.println("Deallocated " + energyDeallocated + " energy from " +
                consumersAffected + " consumers to help " +
                highPriorityConsumer.getId());

        return energyDeallocated;
    }

    private double calculateTotalAllocatedEnergy(){
        double total = 0;

        // get all the consumers
        Map<String, EnergyConsumer> allconsumers = allocationManager.getAllConsumers();

        // sum total allocated energy
        for(EnergyConsumer consumer: allconsumers.values()){
            total += consumer.getAllocatedEnergy();
        }
        return total;
    }

    public void setDisturbanceBudget(double disturbanceBudget) {
        this.disturbanceBudget = disturbanceBudget;
    }

    public double getDisturbanceBudget() {
        return disturbanceBudget;
    }
}
