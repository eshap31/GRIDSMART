package com.example.gridsmart;

public class Fragment {
    private EnergyConsumer consumer; // consumer that the fragment is a part of
    private double size;  // size of the fragment in kW
    private EnergySource assignedSource; // energy source that the fragment is assigned to

    public Fragment(EnergyConsumer consumer, double size, EnergySource assignedSource) {
        this.consumer = consumer;
        this.size = size;
        this.assignedSource = assignedSource;
    }

    public EnergyConsumer getConsumer() { return consumer; }
    public double getSize() { return size; }
    public EnergySource getAssignedSource() { return assignedSource; }

    @Override
    public String toString() {
        return "Fragment{" +
                "consumer=" + consumer.getId() +
                ", size=" + size +
                ", assignedSource=" + assignedSource.getId() +
                '}';
    }
}

