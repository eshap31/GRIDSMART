# GridSmart - Energy Allocation System

## Project Overview

GridSmart is a sophisticated energy allocation system designed to efficiently distribute energy from various sources to consumers in a smart grid environment. The system prioritizes high-priority consumers (like hospitals and emergency services) while optimizing the overall grid efficiency. It handles dynamic events such as source failures, demand changes, and addition of new sources or consumers through intelligent reallocation algorithms.

## Key Features

- **Priority-Based Energy Allocation**: Allocates energy based on consumer priority levels
- **Dynamic Reallocation**: Responds to grid events (source failures, demand changes) in real-time
- **Multiple Allocation Strategies**: 
  - Greedy allocation for efficient distribution
  - Selective deallocation for high-priority consumers
- **Graph-Based Model**: Leverages network flow algorithms for optimal allocation
- **Event-Driven Architecture**: Handles various grid events through a flexible event system
- **Database Integration**: Stores and retrieves energy sources and consumers from a database
- **Visualization Support**: Framework for visual representation of the energy grid (JavaFX)

## Architecture

The project is organized into several packages:

```
com.example.gridsmart
├── DB                  // Database connectivity and data access
├── dynamic             // Dynamic reallocation algorithms
├── events              // Event system and simulation
├── graph               // Graph-based model and algorithms
├── model               // Core domain entities
├── MasterController    // System orchestration
├── offline             // Offline/global allocation algorithms
├── tests               // Demo and test classes
├── ui                  // UI components
└── util                // Utility classes
```

### Core Components

- **Graph Model**: Represents the energy grid as a directed graph with capacity and flow
- **Energy Allocation Manager**: Central component for managing allocations between sources and consumers
- **Dynamic Reallocation Manager**: Handles real-time reallocation in response to events
- **Event System**: Generates and processes grid events like source failures
- **Master Controller**: Orchestrates the entire system

## Allocation Algorithms

GridSmart implements multiple allocation strategies:

1. **Global Allocation Algorithm**: Uses Edmonds-Karp maximum flow algorithm for optimal initial allocation
2. **Greedy Reallocation**: Prioritizes high-priority consumers during dynamic reallocation
3. **Selective Deallocation**: Deallocates energy from lower-priority consumers to satisfy critical needs

## Getting Started

### Prerequisites

- Java 11 or higher
- MySQL database
- Maven for dependency management

### Database Setup

1. Create a MySQL database named `gridsmart`
2. Execute the SQL script in `database/setup.sql` to create necessary tables
3. Update database connection parameters in `DatabaseManager.java` if needed

### Running the Application

```
# Compile the project
mvn clean install

# Run the main application
java -cp target/gridsmart-1.0.jar com.example.gridsmart.MasterController.GridSmartMain
```

### Running the Visualization

```
java -cp target/gridsmart-1.0.jar com.example.gridsmart.ui.VisualGridSmartApp
```

## Demo Classes

The project includes several demo classes to showcase different aspects of the system:

- `Sprint1Demo.java`: Basic grid setup and event simulation
- `Sprint_1and2_Demo.java`: Demonstrates event system and greedy reallocation
- `sprint3_demo.java`: Shows selective deallocation for high-priority consumers
- `DBdemo.java`: Demonstrates database connectivity
- `OfflineAllocationTest.java`: Tests the graph-based allocation system
- `EventSimulatorTest.java`: Tests the event generation system
- `ConsumerAddedTest.java`: Tests the addition of new consumers
- `SourceAddedTest.java`: Tests the addition of new energy sources
- `DemandChangeTest.java`: Tests consumer demand changes

## Event Types

The system handles several types of grid events:

- `SOURCE_FAILURE`: An energy source goes offline
- `SOURCE_ADDED`: A new energy source is added to the grid
- `CONSUMER_ADDED`: A new consumer is added to the grid
- `DEMAND_INCREASE`: A consumer's energy demand increases
- `DEMAND_DECREASE`: A consumer's energy demand decreases

## Core Classes

### Energy Nodes

- `EnergySource`: Represents an energy producer with capacity and type
- `EnergyConsumer`: Represents an energy consumer with priority and demand

### Allocation

- `Allocation`: Represents energy allocation from a source to a consumer
- `EnergyAllocationManager`: Central manager for all energy allocations
- `EnergyAllocationMap`: Maps consumers to their source allocations
- `ReverseAllocationMap`: Maps sources to their consumer allocations

### Graph

- `Graph`: Core representation of the energy grid network
- `GraphEdge`: Represents a directed edge with capacity and flow
- `GlobalAllocationAlgorithm`: Implements priority-based energy allocation

### Events

- `Event`: Represents a grid event
- `EventHandler`: Interface for components that process grid events
- `EventSimulator`: Generates random events for testing
- `EventType`: Enumeration of supported event types

### Reallocation

- `DynamicReallocationManager`: Manages real-time reallocation
- `GreedyReallocator`: Implements greedy algorithm for energy reallocation
- `SelectiveDeallocator`: Manages deallocation from low-priority consumers
- `ReallocationStrategy`: Enumeration of available reallocation strategies

## Future Enhancements

- Web-based UI for grid monitoring and management
- Machine learning for predictive demand and failure analysis
- Integration with real IoT sensors and actuators
- Distributed architecture for large-scale deployments
- Blockchain-based energy trading between consumers

## License

[MIT License](LICENSE)

## Contributors

- Eitam Shapsa
