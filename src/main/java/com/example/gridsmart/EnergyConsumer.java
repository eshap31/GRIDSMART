package com.example.gridsmart;

public class EnergyConsumer
{
    private final String id;
    private int priority; // Priority of the consumer
    private double demand; // Energy demand of the consumer in kW
    private double allocatedEnergy; // Energy allocated to the consumer in kW

    public EnergyConsumer(String id, int priority, double demand)
    {
        this.id = id;
        this.priority = priority;
        this.demand = demand;
        this.allocatedEnergy = 0;
    }

    public void allocateEnergy(double energy)
    {
        this.allocatedEnergy = energy;
    }

    public boolean isFullyAllocated()
    {
        return this.demand == this.allocatedEnergy;
    }

    public double getRemainingDemand()
    {
        return this.demand - this.allocatedEnergy;
    }

    public String getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public double getDemand() {
        return demand;
    }

    public double getAllocatedEnergy() {
        return allocatedEnergy;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public void setAllocatedEnergy(double allocatedEnergy) {
        this.allocatedEnergy = allocatedEnergy;
    }

    @Override
    public String toString() {
        return "EnergyConsumer{" +
                "id=" + id +
                ", priority=" + priority +
                ", demand=" + demand +
                ", allocatedEnergy=" + allocatedEnergy +
                '}';
    }
}
