package com.example.gridsmart.MasterController;

import com.example.gridsmart.DB.*;
import com.example.gridsmart.dynamic.*;
import com.example.gridsmart.events.*;
import com.example.gridsmart.graph.*;
import com.example.gridsmart.model.*;
import com.example.gridsmart.offline.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// master controller that orchestrates all the components
// in the system
public class MasterController
{
    // core attributes
    private Graph graph;
    private List<EnergySource> sources;
    private List<EnergyConsumer> consumers;
    private EnergyAllocationManager allocationManager;
    private DynamicReallocationManager reallocationManager;
    private EventSimulator eventSimulator;

    private long eventFrequencyMs = 5000; // 5 second in between each event

    public MasterController()
    {
        this.graph = new Graph();
    }

    // main entry point to start the system
    public void start()
    {
        try
        {
            // step 1: load data from the database
            System.out.println("Step 1: Loading data from the database...");
            loadDataFromDatabase();

            // step 2: add nodes to graph
            System.out.println("Step 2: Building energy grid graph...");
            buildGraph();

            // Step 3: Run global allocation algorithm
            System.out.println("Step 3: Running global allocation algorithm...");
            runGlobalAllocation();

            // step 4: set up dynamic reallocation
            System.out.println("Step 4: Initializing dynamic reallocation manager...");
            initializeDynamicReallocation();

            // step 5: set up event simulation
            System.out.println("Step 5: Setting up event simulator...");
            setupEventSimulation();

            // step 6: start event simulation
            System.out.println("Step 6: Starting event simulation...");
            startEventSimulation();

            System.out.println("GridSmart system initialization complete.");
        }
        catch (SQLException e)
        {
            System.out.println("error starting GRIDSMART ");
            e.printStackTrace();
        }
    }

    // function that loads energy sources and consumers from the database
    public void loadDataFromDatabase() throws SQLException{
        try
        {
            EnergySourceDB sourceDB = new EnergySourceDB();
            EnergyConsumerDB consumerDB = new EnergyConsumerDB();

            this.sources = sourceDB.selectAll();
            this.consumers = consumerDB.selectAll();

            System.out.println("Loaded " + sources.size() + " sources and " +
                    consumers.size() + " consumers from database.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildGraph()
    {
        // Add all sources to the graph
        for (EnergySource source : sources) {
            graph.addNode(source);
        }

        // Add all consumers to the graph
        for (EnergyConsumer consumer : consumers) {
            graph.addNode(consumer);
        }

        // Add edges from every source to every consumer with "infinite" capacity
        for (EnergySource source : sources) {
            for (EnergyConsumer consumer : consumers) {
                graph.addEdge(source.getId(), consumer.getId(), Double.MAX_VALUE);
            }
        }

        System.out.println("Graph built with " + graph.getAllNodes().size() + " nodes.");
    }

    public void runGlobalAllocation()
    {
        // Create a fresh graph for the allocation manager
        Graph managerGraph = new Graph();
        this.allocationManager = new EnergyAllocationManager(managerGraph);

        GlobalAllocationAlgorithm allocator = new GlobalAllocationAlgorithm(allocationManager);
        allocator.run(graph, consumers, sources);

        EnergyAllocationManager am = allocator.getAllocationManager();
        allocationManager = am;

        System.out.println("Global allocation completed.");
    }

    public void initializeDynamicReallocation()
    {
        this.reallocationManager = new DynamicReallocationManager(graph, allocationManager);
        System.out.println("Dynamic reallocation manager initialized.");
    }

    private void setupEventSimulation()
    {
        this.eventSimulator = new EventSimulator(eventFrequencyMs, graph);
        eventSimulator.setEventHandler(reallocationManager);
        System.out.println("Event simulator configured with frequency: " + eventFrequencyMs + "ms");
    }

    public void startEventSimulation()
    {
        if(eventSimulator != null)
        {
            eventSimulator.start();
            System.out.println("Event simulation started. Events will be generated every " +
                    eventFrequencyMs + "ms");
        }
        else {
            System.err.println("Error: Event simulator not initialized. Call setupEventSimulation() first.");
        }
    }

    public void stopEventSimulation() {
        if (eventSimulator != null) {
            eventSimulator.stop();
            System.out.println("Event simulation stopped.");
        }
    }

    // print summary of current energy allocation
    public void printAllocationStatus() {
        // Print allocations for each consumer
        System.out.println("\n----- Consumer Allocations from EnergyAllocationManager -----");
        Map<String, EnergyConsumer> allConsumers = allocationManager.getAllConsumers();

        for (EnergyConsumer consumer : allConsumers.values()) {
            System.out.printf("Consumer %s (Priority %d, Demand %.2f):%n",
                    consumer.getId(), consumer.getPriority(), consumer.getDemand());

            Map<EnergySource, Allocation> allocations = allocationManager.getAllocationsForConsumer(consumer);

            if (allocations.isEmpty()) {
                System.out.println("  No allocations");
            } else {
                double totalAllocation = 0;

                for (Map.Entry<EnergySource, Allocation> entry : allocations.entrySet()) {
                    EnergySource source = entry.getKey();
                    Allocation allocation = entry.getValue();
                    double amount = allocation.getAllocatedEnergy();

                    System.out.printf("  From %s (%s): %.2f%n",
                            source.getId(), source.getType(), amount);

                    totalAllocation += amount;
                }

                double fulfillmentPercentage = (consumer.getDemand() > 0) ?
                        (totalAllocation / consumer.getDemand()) * 100.0 : 0.0;

                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
                        totalAllocation, consumer.getDemand(), fulfillmentPercentage);
            }
        }

        // Print allocations for each source
        System.out.println("\n----- Source Allocations from EnergyAllocationManager -----");
        Map<String, EnergySource> allSources = allocationManager.getAllSources();

        for (EnergySource source : allSources.values()) {
            System.out.printf("Source %s (%s, Capacity %.2f):%n",
                    source.getId(), source.getType(), source.getCapacity());

            Map<EnergyConsumer, Allocation> allocations = allocationManager.getAllocationsForSource(source);

            if (allocations.isEmpty()) {
                System.out.println("  No consumers allocated");
            } else {
                double totalAllocated = 0;

                for (Map.Entry<EnergyConsumer, Allocation> entry : allocations.entrySet()) {
                    EnergyConsumer consumer = entry.getKey();
                    Allocation allocation = entry.getValue();
                    double amount = allocation.getAllocatedEnergy();

                    System.out.printf("  To %s (Priority %d): %.2f%n",
                            consumer.getId(), consumer.getPriority(), amount);

                    totalAllocated += amount;
                }

                double utilizationPercentage = (source.getCapacity() > 0) ?
                        (totalAllocated / source.getCapacity()) * 100.0 : 0.0;

                System.out.printf("  Total allocated: %.2f / %.2f (%.2f%%)%n",
                        totalAllocated, source.getCapacity(), utilizationPercentage);
            }
        }
    }


    // Prints statistics from the reallocation manager
    public void printStatistics() {
        if (reallocationManager != null) {
            reallocationManager.printStatistics();
        } else {
            System.out.println("Reallocation manager not initialized.");
        }
    }

    /**
     * Gets the EnergyAllocationManager instance
     * @return the allocation manager
     */
    public EnergyAllocationManager getAllocationManager() {
        return this.allocationManager;
    }

    /**
     * Gets the dynamic reallocation manager
     * @return the reallocation manager
     */
    public DynamicReallocationManager getReallocationManager() {
        return this.reallocationManager;
    }

    /**
     * Gets the event simulator
     * @return the event simulator
     */
    public EventSimulator getEventSimulator() {
        return this.eventSimulator;
    }

    /**
     * Gets the sources list
     * @return list of energy sources
     */
    public List<EnergySource> getSources() {
        return this.sources;
    }

    /**
     * Gets the consumers list
     * @return list of energy consumers
     */
    public List<EnergyConsumer> getConsumers() {
        return this.consumers;
    }

    /**
     * Gets the graph
     * @return the energy grid graph
     */
    public Graph getGraph() {
        return this.graph;
    }

    /**
     * Set a custom event handler
     * This allows the UI to receive events directly
     */
    public void setEventHandler(EventHandler handler) {
        if (eventSimulator != null) {
            eventSimulator.setEventHandler(handler);
        }
    }
}
