package com.example.gridsmart.dynamic;

import com.example.gridsmart.model.*;

public class DeallocationCandidate {
    private final EnergyConsumer consumer;
    private final EnergySource source;
    private final double deallocatableEnergy;
    private int priorityDifference;

    public DeallocationCandidate(EnergyConsumer consumer, EnergySource source,
                                 double deallocatableEnergy, int priorityDifference) {
        this.consumer = consumer;
        this.source = source;
        this.deallocatableEnergy = deallocatableEnergy;
        this.priorityDifference = priorityDifference;
    }

    public EnergyConsumer getConsumer() {
        return consumer;
    }

    public EnergySource getSource() {
        return source;
    }

    public double getDeallocatableEnergy() {
        return deallocatableEnergy;
    }

    public int getPriorityDifference() {
        return priorityDifference;
    }
}
