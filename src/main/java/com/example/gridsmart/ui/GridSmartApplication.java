package com.example.gridsmart.ui;

import com.example.gridsmart.MasterController.MasterController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX Application for GridSmart energy allocation visualization
 */
public class GridSmartApplication extends Application {

    private GridSmartController controller;

    @Override
    public void start(Stage primaryStage) {
        // Create main controller
        controller = new GridSmartController();

        // Get the grid visualization view from the controller
        GridVisualizationView gridView = controller.getGridView();

        // Create a scene with the grid view
        Scene scene = new Scene(gridView, 900, 700);

        // Configure the main stage
        primaryStage.setTitle("GridSmart Energy Allocation System");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize the system
        controller.initSystem();
    }

    @Override
    public void stop() {
        // Clean shutdown when application closes
        if (controller != null) {
            controller.shutdown();
        }
    }

    /**
     * Main method to launch the application
     */
    public static void main(String[] args) {
        launch(args);
    }
}