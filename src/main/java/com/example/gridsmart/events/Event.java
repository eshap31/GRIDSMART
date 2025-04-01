package com.example.gridsmart.events;

import com.example.gridsmart.model.EnergyNode;

import java.util.ArrayList;

public class Event {
    public final EventType type;
    public ArrayList<EnergyNode> nodes; // the node/s that the event is related to
    private final String eventDescription;
    private final long timestamp; // the time at which the event occurred
    private Boolean handled; // whether the event has been handled

    public Event(EventType type, ArrayList<EnergyNode> node, String eventDescription, long timestamp) {
        this.type = type;
        this.nodes = node;
        this.eventDescription = eventDescription;
        this.timestamp = timestamp;
        this.handled = false;
    }

    // constructor for single-node events
    public Event(EventType type, EnergyNode node, String eventDescription, long timestamp) {
        this(type, new ArrayList<>(), eventDescription, timestamp);
        this.nodes.add(node);
    }

    public EventType getType() {
        return type;
    }

    public ArrayList<EnergyNode> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<EnergyNode> nodes) {
        this.nodes = nodes;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Boolean getHandled() {
        return handled;
    }

    public void setHandled(Boolean handled) {
        this.handled = handled;
    }

    @Override
    public String toString() {
        return "Event{" +
                "type=" + type +
                ", nodes=" + nodes +
                ", eventDescription='" + eventDescription + '\'' +
                ", timestamp=" + timestamp +
                ", handled=" + handled +
                '}';
    }
}
