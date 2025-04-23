package com.example.gridsmart.tests;

import com.example.gridsmart.events.Event;
import com.example.gridsmart.events.EventHandler;
import com.example.gridsmart.events.EventSimulator;
import com.example.gridsmart.events.EventType;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergyNode;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class EventSimulatorTest {
    public static void main(String[] args) {
        System.out.println("===== Starting Event Simulator Test =====");

        // Create a test graph with some sources and consumers
        Graph graph = createTestGraph();

        // Create a map to track event counts by type
        Map<EventType, Integer> eventCounts = new HashMap<>();
        for (EventType type : EventType.values()) {
            eventCounts.put(type, 0);
        }

        // Create a countdown latch to wait for events
        int numEvents = 20;
        CountDownLatch latch = new CountDownLatch(numEvents);

        // Create an event handler to count events
        EventHandler testHandler = new EventHandler() {
            @Override
            public void handleEvent(Event event) {
                // Count this event type
                EventType type = event.getType();
                eventCounts.put(type, eventCounts.get(type) + 1);

                // Print event details
                System.out.println("Event received: " + type + " - " + event.getEventDescription());

                // Print basic info about the affected node(s)
                if (!event.getNodes().isEmpty()) {
                    EnergyNode node = event.getNodes().get(0);
                    if (node instanceof EnergySource) {
                        EnergySource source = (EnergySource) node;
                        System.out.println("  Source details: " + source.getId() +
                                " (" + source.getType() + ") with capacity " + source.getCapacity());
                    } else if (node instanceof EnergyConsumer) {
                        EnergyConsumer consumer = (EnergyConsumer) node;
                        System.out.println("  Consumer details: " + consumer.getId() +
                                " (Priority " + consumer.getPriority() +
                                ") with demand " + consumer.getDemand());
                    }
                }

                // Count down the latch
                latch.countDown();
            }
        };

        // Create the event simulator with a short interval (100ms between events)
        EventSimulator simulator = new EventSimulator(100, graph);
        simulator.setEventHandler(testHandler);

        // Start the simulator
        System.out.println("\nStarting simulator to generate " + numEvents + " events...");
        simulator.start();

        try {
            // Wait for all events or timeout after 10 seconds
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("Test interrupted while waiting for events");
        } finally {
            // Stop the simulator
            simulator.stop();
        }

        // Print summary of events
        System.out.println("\n===== Event Generation Summary =====");
        int totalEvents = 0;
        for (EventType type : EventType.values()) {
            int count = eventCounts.get(type);
            totalEvents += count;
            System.out.println(type + ": " + count + " events (" +
                    String.format("%.1f", (count * 100.0 / numEvents)) + "%)");
        }

        System.out.println("\nTotal events: " + totalEvents);

        // Test specific event creation
        System.out.println("\n===== Testing Specific Event Creation =====");
        for (EventType type : EventType.values()) {
            Event event = simulator.createEvent(type);
            if (event != null) {
                System.out.println("Successfully created event: " + type + " - " + event.getEventDescription());
            } else {
                System.out.println("Failed to create event: " + type);
            }
        }

        System.out.println("\nEvent Simulator Test completed");
    }

    private static Graph createTestGraph() {
        Graph graph = new Graph();

        // Add some energy sources
        EnergySource solar = new EnergySource("solar1", 500, SourceType.SOLAR);
        EnergySource wind = new EnergySource("wind1", 300, SourceType.WIND);
        EnergySource hydro = new EnergySource("hydro1", 700, SourceType.HYDRO);

        graph.addNode(solar);
        graph.addNode(wind);
        graph.addNode(hydro);

        // Add some energy consumers
        EnergyConsumer hospital = new EnergyConsumer("hospital", 1, 400);
        EnergyConsumer school = new EnergyConsumer("school", 3, 200);
        EnergyConsumer mall = new EnergyConsumer("mall", 4, 300);

        graph.addNode(hospital);
        graph.addNode(school);
        graph.addNode(mall);

        // Add connections
        graph.addEdge(solar.getId(), hospital.getId(), 400);
        graph.addEdge(solar.getId(), school.getId(), 100);
        graph.addEdge(wind.getId(), school.getId(), 100);
        graph.addEdge(wind.getId(), mall.getId(), 200);
        graph.addEdge(hydro.getId(), mall.getId(), 100);
        graph.addEdge(hydro.getId(), hospital.getId(), 300);

        return graph;
    }
}