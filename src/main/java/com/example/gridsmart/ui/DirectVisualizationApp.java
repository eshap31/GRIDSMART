package com.example.gridsmart.ui;

import com.example.gridsmart.graph.Allocation;
import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/**
 * Standalone visualization app that creates its own test data directly
 */
public class DirectVisualizationApp extends Application {

    // Constants for node display
    private static final double SOURCE_RADIUS = 30;
    private static final double CONSUMER_RADIUS = 20;
    private static final double HORIZONTAL_SPACING = 180;
    private static final double VERTICAL_SPACING = 80;

    // Maps to track UI representations of nodes
    private final Map<String, StackPane> sourceNodes = new HashMap<>();
    private final Map<String, StackPane> consumerNodes = new HashMap<>();
    private final Map<String, Map<String, Line>> allocationLines = new HashMap<>();

    // Container for all grid elements
    private final Group gridGroup = new Group();
    private final VBox eventLogContainer = new VBox(10);
    private final ScrollPane scrollPane = new ScrollPane(gridGroup);

    // Data models
    private Graph graph;
    private EnergyAllocationManager allocationManager;

    @Override
    public void start(Stage primaryStage) {
        // Create the main layout
        BorderPane root = new BorderPane();

        // Set up scrolling container for the grid
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Create event log area
        VBox eventBox = new VBox(5);
        Label eventTitle = new Label("Event Log");
        eventTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        eventBox.getChildren().addAll(eventTitle, eventLogContainer);
        eventBox.setPadding(new Insets(10));
        eventBox.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        VBox.setVgrow(eventLogContainer, Priority.ALWAYS);
        eventBox.setPrefHeight(150);

        // Set up the main layout
        root.setCenter(scrollPane);
        root.setBottom(eventBox);

        // Create a scene with the grid view
        Scene scene = new Scene(root, 900, 700);

        // Configure the main stage
        primaryStage.setTitle("GridSmart Energy Allocation System");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Create test data in a background thread
        new Thread(() -> {
            try {
                createTestData();
                Platform.runLater(() -> {
                    updateVisualization();
                    logEvent("Test data created and displayed successfully");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> logEvent("Error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Create test data directly
     */
    private void createTestData() {
        System.out.println("Creating test data...");

        // Create a new graph
        graph = new Graph();

        // Create energy sources with different capacities
        EnergySource solar1 = new EnergySource("solar1", 800, SourceType.SOLAR);
        EnergySource wind1 = new EnergySource("wind1", 600, SourceType.WIND);
        EnergySource hydro1 = new EnergySource("hydro1", 1200, SourceType.HYDRO);

        // Add sources to graph
        graph.addNode(solar1);
        graph.addNode(wind1);
        graph.addNode(hydro1);

        // Create consumers with different priorities and demands
        EnergyConsumer hospital = new EnergyConsumer("hospital", 1, 700);
        EnergyConsumer fireStation = new EnergyConsumer("fireStation", 1, 300);
        EnergyConsumer school = new EnergyConsumer("school", 3, 400);
        EnergyConsumer mall = new EnergyConsumer("mall", 4, 600);

        // Add consumers to graph
        graph.addNode(hospital);
        graph.addNode(fireStation);
        graph.addNode(school);
        graph.addNode(mall);

        // Create allocation manager
        allocationManager = new EnergyAllocationManager(graph);

        // Set up initial allocations
        allocationManager.addAllocation(hospital, hydro1, 700);
        allocationManager.addAllocation(fireStation, wind1, 300);
        allocationManager.addAllocation(school, solar1, 400);
        allocationManager.addAllocation(mall, solar1, 400);

        System.out.println("Test data created successfully.");
        System.out.println("Sources: " + allocationManager.getAllSources().size());
        System.out.println("Consumers: " + allocationManager.getAllConsumers().size());
    }

    /**
     * Update the visualization with current data
     */
    private void updateVisualization() {
        try {
            clearVisualization();

            // Get data from allocation manager
            Map<String, EnergySource> sources = allocationManager.getAllSources();
            Map<String, EnergyConsumer> consumers = allocationManager.getAllConsumers();

            System.out.println("Updating visualization with " + sources.size() +
                    " sources and " + consumers.size() + " consumers");

            // Create source nodes on the left side
            int sourceIndex = 0;
            for (EnergySource source : sources.values()) {
                double yPos = 50 + sourceIndex * VERTICAL_SPACING;
                StackPane sourceNode = createSourceNode(source);
                sourceNode.setLayoutX(100);
                sourceNode.setLayoutY(yPos);
                sourceNodes.put(source.getId(), sourceNode);
                gridGroup.getChildren().add(sourceNode);
                sourceIndex++;
            }

            // Create consumer nodes on the right side
            int consumerIndex = 0;
            for (EnergyConsumer consumer : consumers.values()) {
                double yPos = 50 + consumerIndex * VERTICAL_SPACING;
                StackPane consumerNode = createConsumerNode(consumer);
                consumerNode.setLayoutX(100 + HORIZONTAL_SPACING * 2);
                consumerNode.setLayoutY(yPos);
                consumerNodes.put(consumer.getId(), consumerNode);
                gridGroup.getChildren().add(consumerNode);
                consumerIndex++;
            }

            // Draw allocation lines
            for (EnergyConsumer consumer : consumers.values()) {
                Map<EnergySource, Allocation> consumerAllocations =
                        allocationManager.getAllocationsForConsumer(consumer);

                if (consumerAllocations != null) {
                    for (Map.Entry<EnergySource, Allocation> entry : consumerAllocations.entrySet()) {
                        EnergySource source = entry.getKey();
                        Allocation allocation = entry.getValue();

                        if (allocation != null && allocation.getAllocatedEnergy() > 0) {
                            createAllocationLine(source, consumer, allocation);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error in updateVisualization: " + e.getMessage());
            e.printStackTrace();
            logEvent("Error updating visualization: " + e.getMessage());
        }
    }

    /**
     * Clear the visualization of all nodes and connections
     */
    private void clearVisualization() {
        gridGroup.getChildren().clear();
        sourceNodes.clear();
        consumerNodes.clear();
        allocationLines.clear();
    }

    /**
     * Create a visual representation of an energy source
     */
    private StackPane createSourceNode(EnergySource source) {
        StackPane sourcePane = new StackPane();

        // Create source circle
        Circle circle = new Circle(SOURCE_RADIUS);
        circle.setFill(Color.DODGERBLUE);
        circle.setStroke(Color.BLACK);

        // Create status indicator
        Circle statusIndicator = new Circle(5);
        statusIndicator.setFill(source.isActive() ? Color.GREEN : Color.RED);

        // Label with source info
        VBox infoBox = new VBox(2);
        infoBox.setAlignment(Pos.CENTER);

        Text idText = new Text(source.getId());
        idText.setFont(Font.font("System", FontWeight.BOLD, 12));

        Text typeText = new Text(source.getType().toString());
        Text loadText = new Text(String.format("%.1f/%.1f", source.getCurrentLoad(), source.getCapacity()));

        infoBox.getChildren().addAll(idText, typeText, loadText);

        // Organize elements in stack pane
        StackPane.setAlignment(statusIndicator, Pos.TOP_RIGHT);
        sourcePane.getChildren().addAll(circle, infoBox, statusIndicator);

        return sourcePane;
    }

    /**
     * Create a visual representation of an energy consumer
     */
    private StackPane createConsumerNode(EnergyConsumer consumer) {
        StackPane consumerPane = new StackPane();

        // Determine color based on fulfillment
        Color nodeColor;
        double fulfillment = 0;
        if (consumer.getDemand() > 0) {
            fulfillment = consumer.getAllocatedEnergy() / consumer.getDemand();
        }

        if (fulfillment >= 0.99) {  // Fully satisfied (allow for small floating point errors)
            nodeColor = Color.GREEN;
        } else if (fulfillment > 0) {  // Partially satisfied
            nodeColor = Color.YELLOW;
        } else {  // Not satisfied
            nodeColor = Color.RED;
        }

        // Create consumer circle
        Circle circle = new Circle(CONSUMER_RADIUS);
        circle.setFill(nodeColor);
        circle.setStroke(Color.BLACK);

        // Label with consumer info
        VBox infoBox = new VBox(2);
        infoBox.setAlignment(Pos.CENTER);

        Text idText = new Text(consumer.getId());
        idText.setFont(Font.font("System", FontWeight.BOLD, 12));

        Text priorityText = new Text("Priority: " + consumer.getPriority());
        Text demandText = new Text(String.format("%.1f/%.1f", consumer.getAllocatedEnergy(), consumer.getDemand()));

        infoBox.getChildren().addAll(idText, priorityText, demandText);

        consumerPane.getChildren().addAll(circle, infoBox);

        return consumerPane;
    }

    /**
     * Create a line representing energy allocation between source and consumer
     */
    private void createAllocationLine(EnergySource source, EnergyConsumer consumer, Allocation allocation) {
        // Get node positions
        StackPane sourceNode = sourceNodes.get(source.getId());
        StackPane consumerNode = consumerNodes.get(consumer.getId());

        if (sourceNode == null || consumerNode == null) {
            System.out.println("Warning: Cannot create allocation line - nodes not found");
            return;
        }

        // Create line between source and consumer
        double startX = sourceNode.getLayoutX() + SOURCE_RADIUS;
        double startY = sourceNode.getLayoutY();
        double endX = consumerNode.getLayoutX() - CONSUMER_RADIUS;
        double endY = consumerNode.getLayoutY();

        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(Color.GREEN);
        line.setStrokeWidth(Math.min(5, 1 + allocation.getAllocatedEnergy() / 100));

        // Add line to tracking map
        allocationLines.computeIfAbsent(source.getId(), k -> new HashMap<>())
                .put(consumer.getId(), line);

        // Add label with allocation amount
        Text allocationText = new Text(String.format("%.1f", allocation.getAllocatedEnergy()));
        allocationText.setX((startX + endX) / 2 - 15);
        allocationText.setY((startY + endY) / 2 - 5);

        // Add elements to grid
        gridGroup.getChildren().addAll(line, allocationText);
    }

    /**
     * Log an event to the event display area
     */
    private void logEvent(String eventDescription) {
        Label eventLabel = new Label(eventDescription);
        eventLabel.setTextFill(Color.RED);
        eventLabel.setWrapText(true);

        // Add to top of log (newest first)
        eventLogContainer.getChildren().add(0, eventLabel);

        // Keep log size manageable
        if (eventLogContainer.getChildren().size() > 10) {
            eventLogContainer.getChildren().remove(eventLogContainer.getChildren().size() - 1);
        }
    }

    /**
     * Main method to launch the application
     */
    public static void main(String[] args) {
        launch(args);
    }
}