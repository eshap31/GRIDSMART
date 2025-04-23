package com.example.gridsmart.events;

import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.*;

import java.util.*;
import java.util.function.Supplier;

public class EventSimulator {
    private final long frequency; // the frequency of events
    private final Random random; // random number generator
    private final Graph graph;

    // the event handler that will handle the events
    private EventHandler eventHandler;

    private boolean isRunning;
    private Timer timer; // the timer that will schedule the events

    // Constants for new source/consumer generation
    private static final double MIN_SOURCE_CAPACITY = 300;
    private static final double MAX_SOURCE_CAPACITY = 1200;
    private static final double MIN_CONSUMER_DEMAND = 200;
    private static final double MAX_CONSUMER_DEMAND = 800;
    private static final double DEMAND_CHANGE_PERCENTAGE = 0.3; // 30% increase/decrease

    // map to associate event types with their event creation methods
    private final Map<EventType, Supplier<Event>> eventFactories = new HashMap<>();

    public EventSimulator(long frequency, Graph graph) {
        this.frequency = frequency;
        this.random = new Random();
        this.graph = graph;
        this.isRunning = false;

        // Set up event factories
        eventFactories.put(EventType.SOURCE_FAILURE, this::createSourceFailureEvent);
        eventFactories.put(EventType.SOURCE_ADDED, this::createSourceAddedEvent);
        eventFactories.put(EventType.CONSUMER_ADDED, this::createConsumerAddedEvent);
    }

    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    // generate events
    public void start()
    {
        if (isRunning) {
            return;
        }

        isRunning = true;
        // create a new timer, that will generate and
        // dispatch events every frequency ms
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            // implement the run method of the Timer class
            // runs in a separate thread
            @Override
            public void run()
            {
                if(isRunning)
                {
                    Event event = generateRandomEvent();
                    // send the event to the handler
                    dispatchEvent(event);
                }
            }
        }, 5000, frequency);
    }

    // stop generating events
    public void stop()
    {
        isRunning = false;
        if(timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }

    // trigger event manually
    public void triggerEvent(EventType type)
    {
        Event event = createEvent(type);
    }

    // send event to handler
    private void dispatchEvent(Event event)
    {
        if(eventHandler != null && event != null)
        {
            eventHandler.handleEvent(event);
        }
    }

    // generate random event
    private Event generateRandomEvent() {
        EventType[] types = EventType.values();
        EventType type = types[random.nextInt(types.length)];
        return createEvent(type);
    }

    // create an event base on the type
    public Event createEvent(EventType type) {
        Supplier<Event> factory = eventFactories.get(type);
        if (factory != null) {
            return factory.get();
        } else {
            System.out.println("unknown event type: " + type);
            return null;
        }
    }


    // create a source failure event
    // by choosing a random source
    private Event createSourceFailureEvent() {
        // get all the sources in the graph
        List<EnergyNode> sources = graph.getNodesByType(NodeType.SOURCE);
        if (sources.isEmpty()) {
            System.out.println("No sources to fail");
            return null; // No sources to fail
        }

        // select a random source
        EnergyNode source = sources.get(random.nextInt(sources.size()));

        // Create event
        ArrayList<EnergyNode> nodes = new ArrayList<>();
        nodes.add(source);
        return new Event(
                EventType.SOURCE_FAILURE,
                nodes,
                "Source " + source.getId() + " has failed",
                System.currentTimeMillis()
        );
    }

    // create a source added event
    // by generating a random source
    private Event createSourceAddedEvent() {
        // Generate a unique ID for the new source
        String sourceId = "source_" + System.currentTimeMillis();

        // Generate random capacity between 200 and 1000
        double capacity = 200 + random.nextDouble() * 800;

        // Select a random source type
        SourceType[] sourceTypes = SourceType.values();
        SourceType sourceType = sourceTypes[random.nextInt(sourceTypes.length)];

        // Create the new source
        EnergySource newSource = new EnergySource(sourceId, capacity, sourceType);

        // Create event
        ArrayList<EnergyNode> nodes = new ArrayList<>();
        nodes.add(newSource);
        return new Event(
                EventType.SOURCE_ADDED,
                nodes,
                "New " + sourceType + " source added with " + String.format("%.2f", capacity) + " capacity",
                System.currentTimeMillis()
        );
    }

    private Event createConsumerAddedEvent() {
        // Generate a unique ID for the new consumer
        String consumerId = "consumer_" + System.currentTimeMillis();

        // Generate random priority between 1 and 5 (1 is highest)
        int priority = 1 + random.nextInt(5);

        // Generate random demand between 100 and 800
        double demand = 100 + random.nextDouble() * 700;

        // Create the new consumer
        EnergyConsumer newConsumer = new EnergyConsumer(consumerId, priority, demand);

        // Create event
        ArrayList<EnergyNode> nodes = new ArrayList<>();
        nodes.add(newConsumer);
        return new Event(
                EventType.CONSUMER_ADDED,
                nodes,
                "New consumer added with priority " + priority + " and demand " + String.format("%.2f", demand),
                System.currentTimeMillis()
        );
    }

}
