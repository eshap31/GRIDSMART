package com.example.gridsmart.ui;

import com.example.gridsmart.MasterController.MasterController;
import com.example.gridsmart.events.Event;
import com.example.gridsmart.events.EventHandler;
import com.example.gridsmart.graph.EnergyAllocationManager;
import javafx.application.Platform;

/**
 * Integrated controller that bridges the MasterController's backend
 * functionality with the DirectVisualizationApp frontend.
 */
public class VisualController implements EventHandler {

    private final MasterController masterController;
    private DirectVisualizationApp visualizationApp;

    /**
     * Creates a new VisualController with the specified master controller.
     * @param masterController The system's master controller
     */
    public VisualController(MasterController masterController) {
        this.masterController = masterController;
    }

    /**
     * Initializes the controller and sets up event handling.
     */
    public void initialize() {
        // Register this controller as an event handler with the master controller
        masterController.setEventHandler(this);
    }

    /**
     * Sets the visualization app to be controlled by this controller.
     * @param visualizationApp The JavaFX visualization app
     */
    public void setVisualizationApp(DirectVisualizationApp visualizationApp) {
        this.visualizationApp = visualizationApp;
    }

    /**
     * Gets the allocation manager from the master controller.
     * @return The energy allocation manager
     */
    public EnergyAllocationManager getAllocationManager() {
        return masterController.getAllocationManager();
    }

    /**
     * Handles events from the master controller and updates the visualization.
     * @param event The event to handle
     */
    @Override
    public void handleEvent(Event event) {
        // First, let the master controller handle the event
        masterController.getReallocationManager().handleEvent(event);

        // Then update the visualization on the JavaFX thread
        if (visualizationApp != null) {
            Platform.runLater(() -> {
                visualizationApp.logEvent(event.getEventDescription());
                visualizationApp.updateVisualization();
            });
        }
    }

    /**
     * Starts the event simulation.
     */
    public void startEventSimulation() {
        masterController.startEventSimulation();
    }

    /**
     * Stops the event simulation.
     */
    public void stopEventSimulation() {
        masterController.stopEventSimulation();
    }
}