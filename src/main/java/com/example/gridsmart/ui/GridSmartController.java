package com.example.gridsmart.ui;

import com.example.gridsmart.MasterController.MasterController;
import com.example.gridsmart.events.Event;
import com.example.gridsmart.events.EventHandler;
import com.example.gridsmart.graph.Allocation;
import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Controller for the GridSmart UI, integrating with the MasterController
 * and handling updates to the visualization
 */
public class GridSmartController implements EventHandler {

    private final GridVisualizationView gridView;
    private MasterController masterController;
    private EnergyAllocationManager allocationManager;
    private Timer updateTimer;

    /**
     * Create a new GridSmart controller
     */
    public GridSmartController() {
        this.gridView = new GridVisualizationView();
    }

    /**
     * Initialize the GridSmart system
     */
    public void initSystem() {
        // Run initialization in a background thread to avoid blocking the UI
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Initialize the master controller
                masterController = new MasterController();
                masterController.start();

                // Get the allocation manager
                allocationManager = masterController.getAllocationManager();

                // Register as event handler
                masterController.setEventHandler(GridSmartController.this);

                // Schedule periodic UI updates
                scheduleUpdates();

                return null;
            }
        };

        // Handle task completion
        initTask.setOnSucceeded(e -> {
            updateVisualization();
            gridView.logEvent("System initialized successfully");
        });

        initTask.setOnFailed(e -> {
            Throwable exception = initTask.getException();
            gridView.logEvent("Failed to initialize: " + exception.getMessage());
            exception.printStackTrace();
        });

        // Start the initialization
        new Thread(initTask).start();
    }

    /**
     * Shutdown the system
     */
    public void shutdown() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }

        if (masterController != null) {
            masterController.stopEventSimulation();
        }
    }

    /**
     * Get the grid visualization view
     */
    public GridVisualizationView getGridView() {
        return gridView;
    }

    /**
     * Schedule periodic UI updates
     */
    private void scheduleUpdates() {
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(GridSmartController.this::updateVisualization);
            }
        }, 1000, 2000); // Update every 2 seconds
    }

    /**
     * Update the visualization with current system state
     */
    private void updateVisualization() {
        if (allocationManager == null) {
            return;
        }

        // Get data from allocation manager
        Map<String, EnergySource> sources = allocationManager.getAllSources();
        Map<String, EnergyConsumer> consumers = allocationManager.getAllConsumers();

        // Create a structure to represent all allocations
        Map<EnergyConsumer, Map<EnergySource, Allocation>> allAllocations = new HashMap<>();

        for (EnergyConsumer consumer : consumers.values()) {
            Map<EnergySource, Allocation> consumerAllocations = allocationManager.getAllocationsForConsumer(consumer);
            allAllocations.put(consumer, consumerAllocations);
        }

        // Update the view
        gridView.updateVisualization(sources, consumers, allAllocations);
    }

    /**
     * Handle events from the GridSmart system
     */
    @Override
    public void handleEvent(Event event) {
        Platform.runLater(() -> {
            String eventDescription = "⚠️ " + event.getEventDescription();
            gridView.logEvent(eventDescription);

            // Short delay before updating to let the reallocation happen
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        updateVisualization();
                        gridView.logEvent("Reallocation completed after event");
                    });
                }
            }, 500);
        });
    }
}