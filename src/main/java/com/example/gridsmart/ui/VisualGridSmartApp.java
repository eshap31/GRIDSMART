package com.example.gridsmart.ui;

import com.example.gridsmart.MasterController.MasterController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Main entry point for the GridSmart visualization application.
 * Integrates the MasterController with the visualization interface.
 */
public class VisualGridSmartApp extends Application {

    private MasterController masterController;
    private VisualController visualController;

    @Override
    public void init() {
        // Initialize the MasterController
        masterController = new MasterController();

        // Create the visual controller
        visualController = new VisualController(masterController);

        // Start the master controller
        masterController.start();

        // Initialize the visual controller
        visualController.initialize();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Create the visualization app
            DirectVisualizationApp visualizationApp = new DirectVisualizationApp();

            // Set the visual controller
            visualizationApp.setVisualController(visualController);

            // Start the visualization app
            visualizationApp.start(new Stage());

            // Start event simulation after a short delay to allow UI to initialize
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        visualController.startEventSimulation();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Clean shutdown of the simulation
        if (visualController != null) {
            visualController.stopEventSimulation();
        }
    }

    /**
     * Main entry point for the application
     */
    public static void main(String[] args) {
        launch(args);
    }
}