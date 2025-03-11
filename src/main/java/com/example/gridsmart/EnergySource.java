package com.example.gridsmart;

public class EnergySource
{
    private final String id; // ID for the energy source
    private double capacity; // maximum energy source can provide in kW
    private double currentLoad; // current load of the energy source in kW
    private boolean status; // true = Active, false = Offline
    private SourceType type; // Type of the energy source

    public EnergySource(String id, double capacity, SourceType type)
    {
        this.id = id;
        this.capacity = capacity;
        this.currentLoad = 0;
        this.status = true;
        this.type = type;
    }

    public double getAvailableEnergy()
    {
        return capacity - currentLoad;
    }

    public void setAvailableEnergy(double availableEnergy)
    {
        this.currentLoad = this.capacity - availableEnergy;
    }

    public String getId() {
        return id;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(double currentLoad) {
        this.currentLoad = currentLoad;
    }

    public boolean getStatus() {
        return status;
    }

    public void deactivate()
    {
        this.status = false;
    }

    public void active()
    {
        this.status = true;
    }

    @Override
    public String toString() {
        return "EnergySource{" +
                "id=" + id +
                ", capacity=" + capacity +
                ", currentLoad=" + currentLoad +
                ", status=" + status +
                '}';
    }

}
