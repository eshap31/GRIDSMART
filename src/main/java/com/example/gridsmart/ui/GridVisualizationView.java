package com.example.gridsmart.ui;

import com.example.gridsmart.graph.Allocation;
import com.example.gridsmart.model.EnergyConsumer;
import com.example.gridsmart.model.EnergySource;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Visualization component for displaying energy grid allocations
 */
public class GridVisualizationView extends BorderPane {

    // Constants for node display
    private static final double SOURCE_RADIUS = 30;
    private static final double CONSUMER_RADIUS = 20;
    private static final double HORIZONTAL_SPACING = 180;
    private static final double VERTICAL_SPACING = 80;

    // Maps to track UI representations of nodes
    private final Map<String, StackPane> sourceNodes = new HashMap<>();
    private final Map<String, StackPane> consumerNodes = new HashMap<>();
    private final Map<String, Map<String, Line>> allocationLines = new HashMap<>();

    // Container for all grid elements (sources, consumers, lines)
    private final Group gridGroup = new Group();
    private final VBox eventLogContainer = new VBox(10);
    private final ScrollPane scrollPane = new ScrollPane(gridGroup);

    /**
     * Create a new grid visualization
     */
    public GridVisualizationView() {
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
        setCenter(scrollPane);
        setBottom(eventBox);
    }

    /**
     * Clear the visualization of all nodes and connections
     */
    public void clearVisualization() {
        Platform.runLater(() -> {
            gridGroup.getChildren().clear();
            sourceNodes.clear();
            consumerNodes.clear();
            allocationLines.clear();
        });
    }

    /**
     * Update the visualization with current sources, consumers, and allocations
     */
    public void updateVisualization(Map<String, EnergySource> sources,
                                    Map<String, EnergyConsumer> consumers,
                                    Map<EnergyConsumer, Map<EnergySource, Allocation>> allocations) {
        Platform.runLater(() -> {
            clearVisualization();

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
            for (Map.Entry<EnergyConsumer, Map<EnergySource, Allocation>> entry : allocations.entrySet()) {
                EnergyConsumer consumer = entry.getKey();
                Map<EnergySource, Allocation> sourceAllocations = entry.getValue();

                for (Map.Entry<EnergySource, Allocation> sourceEntry : sourceAllocations.entrySet()) {
                    EnergySource source = sourceEntry.getKey();
                    Allocation allocation = sourceEntry.getValue();

                    if (allocation.getAllocatedEnergy() > 0) {
                        createAllocationLine(source, consumer, allocation);
                    }
                }
            }
        });
    }

    /**
     * Log an event to the event display area
     */
    public void logEvent(String eventDescription) {
        Platform.runLater(() -> {
            // Create event display
            Label eventLabel = new Label(eventDescription);
            eventLabel.setTextFill(Color.RED);
            eventLabel.setWrapText(true);

            // Add to top of log (newest first)
            eventLogContainer.getChildren().add(0, eventLabel);

            // Keep log size manageable
            if (eventLogContainer.getChildren().size() > 10) {
                eventLogContainer.getChildren().remove(eventLogContainer.getChildren().size() - 1);
            }
        });
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
}