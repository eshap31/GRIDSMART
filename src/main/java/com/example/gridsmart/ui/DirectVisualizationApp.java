package com.example.gridsmart.ui;
import com.example.gridsmart.graph.Allocation;
import com.example.gridsmart.graph.EnergyAllocationManager;
import com.example.gridsmart.graph.Graph;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
 * JavaFX application for visualizing the energy grid.
 * Modified to accept an external EnergyAllocationManager instead of creating test data.
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

    // Table view for allocations
    private TableView<AllocationRow> allocationTable;
    private ObservableList<AllocationRow> allocationData = FXCollections.observableArrayList();

    // Data models
    private EnergyAllocationManager allocationManager;
    private VisualController visualController;

    /**
     * Sets the visual controller that manages this app.
     * @param visualController The controller that bridges backend and frontend
     */
    public void setVisualController(VisualController visualController) {
        this.visualController = visualController;
        this.allocationManager = visualController.getAllocationManager();
        visualController.setVisualizationApp(this);
    }

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

        // Create allocation table
        VBox allocationTableContainer = createAllocationTable();

        // Create a split pane for right side (allocation table and event log)
        VBox rightPanel = new VBox(10);
        rightPanel.getChildren().addAll(allocationTableContainer, eventBox);
        rightPanel.setPrefWidth(600);  // Increased from 400 to 600
        rightPanel.setMinWidth(550);  // Set minimum width
        rightPanel.setPadding(new Insets(10));

        // Create a horizontal split pane for main content
        HBox mainContent = new HBox(10);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        mainContent.getChildren().addAll(scrollPane, rightPanel);

        // Set up the main layout
        root.setCenter(mainContent);

        // Create a scene with the grid view
        Scene scene = new Scene(root, 1500, 800);  // Increased from 1300x700 to 1500x800

        // Configure the main stage
        primaryStage.setTitle("GridSmart Energy Allocation System");
        primaryStage.setScene(scene);
        primaryStage.show();

        // If we have an allocation manager, update the visualization
        if (allocationManager != null) {
            updateVisualization();
            logEvent("System initialized with live data");
        } else {
            logEvent("Waiting for system initialization...");
        }
    }

    /**
     * Creates the allocation table component
     */
    private VBox createAllocationTable() {
        VBox container = new VBox(5);
        Label tableTitle = new Label("Allocation Details");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Create the table
        allocationTable = new TableView<>();
        allocationTable.setItems(allocationData);

        // Create columns
        TableColumn<AllocationRow, String> sourceCol = new TableColumn<>("Source");
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("sourceId"));
        sourceCol.setPrefWidth(100);  // Increased from 80 to 100

        TableColumn<AllocationRow, String> consumerCol = new TableColumn<>("Consumer");
        consumerCol.setCellValueFactory(new PropertyValueFactory<>("consumerId"));
        consumerCol.setPrefWidth(110);  // Increased from 80 to 110

        TableColumn<AllocationRow, Integer> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityCol.setPrefWidth(70);  // Increased from 60 to 70

        TableColumn<AllocationRow, Double> allocatedCol = new TableColumn<>("Allocated");
        allocatedCol.setCellValueFactory(new PropertyValueFactory<>("allocatedEnergy"));
        allocatedCol.setPrefWidth(95);  // Increased from 75 to 95

        TableColumn<AllocationRow, Double> demandCol = new TableColumn<>("Demand");
        demandCol.setCellValueFactory(new PropertyValueFactory<>("demand"));
        demandCol.setPrefWidth(95);  // Increased from 75 to 95

        TableColumn<AllocationRow, String> satisfactionCol = new TableColumn<>("Satisfaction");
        satisfactionCol.setCellValueFactory(new PropertyValueFactory<>("satisfaction"));
        satisfactionCol.setPrefWidth(100);  // Increased from 85 to 100

        allocationTable.getColumns().addAll(sourceCol, consumerCol, priorityCol,
                allocatedCol, demandCol, satisfactionCol);

        // Configure table properties
        allocationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        allocationTable.setPrefHeight(400);  // Increased from 300 to 400

        container.getChildren().addAll(tableTitle, allocationTable);
        return container;
    }

    /**
     * Updates the allocation table with current data
     */
    private void updateAllocationTable() {
        allocationData.clear();

        if (allocationManager == null) {
            return;
        }

        // Iterate through all consumers and their allocations
        for (EnergyConsumer consumer : allocationManager.getAllConsumers().values()) {
            Map<EnergySource, Allocation> allocations =
                    allocationManager.getAllocationsForConsumer(consumer);

            if (allocations.isEmpty()) {
                // Add a row showing consumer has no allocations
                allocationData.add(new AllocationRow(
                        "-",
                        consumer.getId(),
                        consumer.getPriority(),
                        0.0,
                        consumer.getDemand(),
                        "0.0%"
                ));
            } else {
                // Add a row for each allocation
                for (Map.Entry<EnergySource, Allocation> entry : allocations.entrySet()) {
                    EnergySource source = entry.getKey();
                    Allocation allocation = entry.getValue();

                    double allocated = allocation.getAllocatedEnergy();
                    double demand = consumer.getDemand();
                    double satisfaction = demand > 0 ? (allocated / demand) * 100 : 0;

                    allocationData.add(new AllocationRow(
                            source.getId(),
                            consumer.getId(),
                            consumer.getPriority(),
                            allocated,
                            demand,
                            String.format("%.1f%%", satisfaction)
                    ));
                }
            }
        }

        // Sort by priority, then by consumer ID
        allocationData.sort((a, b) -> {
            int priorityCompare = Integer.compare(a.getPriority(), b.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return a.getConsumerId().compareTo(b.getConsumerId());
        });
    }

    /**
     * Update the visualization with current data from the allocation manager
     */
    public void updateVisualization() {
        try {
            if (allocationManager == null) {
                logEvent("Error: No allocation manager available");
                return;
            }

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

            // Update the allocation table
            updateAllocationTable();
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
    public void logEvent(String eventDescription) {
        Label eventLabel = new Label(eventDescription);
        eventLabel.setTextFill(Color.RED);
        eventLabel.setWrapText(true);

        // Add to top of log (newest first)
        Platform.runLater(() -> {
            eventLogContainer.getChildren().add(0, eventLabel);

            // Keep log size manageable
            if (eventLogContainer.getChildren().size() > 10) {
                eventLogContainer.getChildren().remove(eventLogContainer.getChildren().size() - 1);
            }
        });
    }

    /**
     * Data model class for the allocation table
     */
    public static class AllocationRow {
        private String sourceId;
        private String consumerId;
        private int priority;
        private double allocatedEnergy;
        private double demand;
        private String satisfaction;

        public AllocationRow(String sourceId, String consumerId, int priority,
                             double allocatedEnergy, double demand, String satisfaction) {
            this.sourceId = sourceId;
            this.consumerId = consumerId;
            this.priority = priority;
            this.allocatedEnergy = allocatedEnergy;
            this.demand = demand;
            this.satisfaction = satisfaction;
        }

        // Getters for property binding
        public String getSourceId() { return sourceId; }
        public String getConsumerId() { return consumerId; }
        public int getPriority() { return priority; }
        public double getAllocatedEnergy() { return allocatedEnergy; }
        public double getDemand() { return demand; }
        public String getSatisfaction() { return satisfaction; }
    }
}