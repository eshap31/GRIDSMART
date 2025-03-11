package com.example.gridsmart;

public class Allocation
{
    private EnergySource source;
    private EnergyConsumer consumer;
    private double allocatedEnergy;  // Raw energy allocated, without fragmentation overhead.

    public Allocation(EnergySource source, EnergyConsumer consumer, double allocatedEnergy) {
        this.source = source;
        this.consumer = consumer;
        this.allocatedEnergy = allocatedEnergy;
    }

    public EnergySource getSource() { return source; }
    public EnergyConsumer getConsumer() { return consumer; }
    public double getAllocatedEnergy() { return allocatedEnergy; }
    public void setAllocatedEnergy(double allocatedEnergy) { this.allocatedEnergy = allocatedEnergy; }

    @Override
    public String toString() {
        return "Allocation{" +
                "source=" + source.getId() +
                ", consumer=" + consumer.getId() +
                ", allocatedEnergy=" + allocatedEnergy +
                '}';
    }
}
