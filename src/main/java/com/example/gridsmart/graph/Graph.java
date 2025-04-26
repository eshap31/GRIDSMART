package com.example.gridsmart.graph;

import com.example.gridsmart.model.*;

import java.util.*;

/*
 * Represents the energy grid as a graph with energy sources and consumers as nodes,
 * and energy flows as directed edges with capacity constraints.
 */
public class Graph {
    private final Map<String, EnergyNode> nodes;
    private final Map<String, List<GraphEdge>> outgoingEdges; // Map that holds a list of outgoing nodes for each node
    private final Map<String, List<GraphEdge>> incomingEdges; // Map that holds a list of incoming edges for each node

    // Super nodes for network flow algorithms
    private SuperSource superSource;
    private SuperSink superSink;
    private boolean hasSuperNodes;

    /*
     * Creates a new empty graph
     */
    public Graph() {
        this.nodes = new HashMap<>();
        this.outgoingEdges = new HashMap<>();
        this.incomingEdges = new HashMap<>();
        this.hasSuperNodes = false;
    }

    /*
     * Adds a node to the graph
     * node - EnergyNode implementation
     */
    public void addNode(EnergyNode node) {
        nodes.put(node.getId(), node);
        // add to the adjecency list
        outgoingEdges.putIfAbsent(node.getId(), new ArrayList<>());
        incomingEdges.putIfAbsent(node.getId(), new ArrayList<>());
    }

    /*
     * Removes a node and all its connected edges from the graph
     */
    public void removeNode(String nodeId) {
        EnergyNode node = nodes.get(nodeId);
        if (node == null) {
            return;
        }

        // Remove all edges connected to this node
        List<GraphEdge> outEdges = outgoingEdges.getOrDefault(nodeId, Collections.emptyList());
        for (GraphEdge edge : new ArrayList<>(outEdges)) {
            removeEdge(nodeId, edge.getTarget().getId());
        }

        List<GraphEdge> inEdges = incomingEdges.getOrDefault(nodeId, Collections.emptyList());
        for (GraphEdge edge : new ArrayList<>(inEdges)) {
            removeEdge(edge.getSource().getId(), nodeId);
        }

        // Remove the node
        nodes.remove(nodeId);
        outgoingEdges.remove(nodeId);
        incomingEdges.remove(nodeId);
    }

    /*
     * Adds a directed edge from source to target
     */
    public GraphEdge addEdge(String sourceId, String targetId, double capacity) {
        EnergyNode source = nodes.get(sourceId);
        EnergyNode target = nodes.get(targetId);

        if (source == null || target == null) {
            throw new IllegalArgumentException("Source or target node not found in graph");
        }

        GraphEdge edge = new GraphEdge(source, target, capacity);
        outgoingEdges.get(sourceId).add(edge);
        incomingEdges.get(targetId).add(edge);

        return edge;
    }

    /*
     * Removes an edge from the graph
     */
    public void removeEdge(String sourceId, String targetId) {
        List<GraphEdge> outEdges = outgoingEdges.getOrDefault(sourceId, Collections.emptyList());
        List<GraphEdge> inEdges = incomingEdges.getOrDefault(targetId, Collections.emptyList());

        // Find the edge to remove
        GraphEdge edgeToRemove = null;
        boolean found = false;

        for (GraphEdge edge : outEdges) {
            if (!found && edge.getTarget().getId().equals(targetId)) {
                edgeToRemove = edge;
                found = true;
            }
        }

        if (edgeToRemove != null) {
            outEdges.remove(edgeToRemove);
            inEdges.remove(edgeToRemove);
        }
    }

    /*
     * Returns the node with the given ID
     */
    public EnergyNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /*
     * Returns all nodes in the graph
     */
    public Collection<EnergyNode> getAllNodes() {
        return nodes.values();
    }

    /*
     * Returns all nodes of a specific type (SOURCE or CONSUMER)
     */
    public List<EnergyNode> getNodesByType(NodeType type) {
        List<EnergyNode> result = new ArrayList<>();
        for (EnergyNode node : nodes.values()) {
            if (node.getNodeType() == type) {
                result.add(node);
            }
        }
        return result;
    }

    /*
     * Returns all source nodes
     */
    public List<EnergyNode> getSourceNodes() {
        return getNodesByType(NodeType.SOURCE);
    }

    /*
     * Returns all consumer nodes
     */
    public List<EnergyNode> getConsumerNodes() {
        return getNodesByType(NodeType.CONSUMER);
    }

    /*
     * Returns all edges outgoing from the given node
     */
    public List<GraphEdge> getOutgoingEdges(String nodeId) {
        return outgoingEdges.getOrDefault(nodeId, Collections.emptyList());
    }

    /*
     * Returns all edges incoming to the given node
     */
    public List<GraphEdge> getIncomingEdges(String nodeId) {
        return incomingEdges.getOrDefault(nodeId, Collections.emptyList());
    }

    /*
     * Returns the edge connecting source to target, or null if none exists
     */
    public GraphEdge getEdge(String sourceId, String targetId) {
        for (GraphEdge edge : outgoingEdges.getOrDefault(sourceId, Collections.emptyList())) {
            if (edge.getTarget().getId().equals(targetId)) {
                return edge;
            }
        }
        return null;
    }

    /*
     * Clears all flow values in the graph (sets all to 0)
     */
    public void resetAllFlows() {
        for (List<GraphEdge> edges : outgoingEdges.values()) {
            for (GraphEdge edge : edges) {
                edge.setFlow(0);
            }
        }
    }

    /*
     * Creates and returns a residual graph based on the current graph and flow
     * Used in flow algorithms like Ford-Fulkerson
     */
    public Graph createResidualGraph() {
        Graph residualGraph = new Graph();

        // Add all nodes
        for (EnergyNode node : nodes.values()) {
            residualGraph.addNode(node);
        }

        // Add forward and backward edges with correct residual capacities
        for (String sourceId : outgoingEdges.keySet()) {
            for (GraphEdge edge : outgoingEdges.get(sourceId)) {
                String targetId = edge.getTarget().getId();

                // Forward edge with residual capacity (if there's remaining capacity)
                double residualCapacity = edge.getResidualCapacity();
                if (residualCapacity > 0) {
                    GraphEdge forwardResidualEdge = residualGraph.addEdge(
                            sourceId,
                            targetId,
                            residualCapacity
                    );
                    // Mark that this is a forward edge (not reverse)
                    forwardResidualEdge.setReverse(false);
                }

                // Reverse edge with flow as capacity (if there's existing flow to cancel)
                double flow = edge.getFlow();
                if (flow > 0) {
                    GraphEdge reverseResidualEdge = residualGraph.addEdge(
                            targetId,
                            sourceId,
                            flow
                    );
                    // Mark that this is a reverse edge
                    reverseResidualEdge.setReverse(true);

                    // Set up the relationship between forward and reverse edges if both exist
                    GraphEdge forwardEdge = residualGraph.getEdge(sourceId, targetId);
                    if (forwardEdge != null) {
                        forwardEdge.setReverseEdge(reverseResidualEdge);
                        reverseResidualEdge.setReverseEdge(forwardEdge);
                    }
                }
            }
        }

        return residualGraph;
    }

    // Methods for super nodes

    /*
     * Adds a SuperSource node to the graph with connections to all energy sources.
     * Flow should be 0 , and capacity should the sources capacity
     * return The created SuperSource node
     */
    public SuperSource addSuperSource(String superSourceId) {
        // Create SuperSource if it doesn't exist
        if (superSource == null) {
            superSource = new SuperSource(superSourceId);
            addNode(superSource);
        }

        // Connect SuperSource to all energy sources
        List<EnergyNode> sources = getNodesByType(NodeType.SOURCE);
        for (EnergyNode node : sources) {
            if (node instanceof EnergySource) {
                EnergySource source = (EnergySource) node;
                if (source.isActive()) {
                    // Capacity equals available energy from the source
                    double availableEnergy = source.getAvailableEnergy();
                    addEdge(superSource.getId(), source.getId(), availableEnergy);
                }
            }
        }

        hasSuperNodes = true;
        return superSource;
    }

    /*
     * Adds a SuperSink node to the graph with connections from all energy consumers.
     * Adds edges from each energy consumer to SuperSink with capacity equal to consumer demand.
     * superSinkId - The ID to use for the super sink node
     * The created -SuperSink node
     */
    public SuperSink addSuperSink(String superSinkId) {
        // Create SuperSink if it doesn't exist
        if (superSink == null) {
            superSink = new SuperSink(superSinkId);
            addNode(superSink);
        }

        // Connect all energy consumers to SuperSink
        List<EnergyNode> consumers = getNodesByType(NodeType.CONSUMER);
        for (EnergyNode node : consumers) {
            if (node instanceof EnergyConsumer) {
                EnergyConsumer consumer = (EnergyConsumer) node;
                if (consumer.isActive()) {
                    // Capacity equals consumer demand
                    double demand = consumer.getDemand();
                    addEdge(consumer.getId(), superSink.getId(), demand);
                }
            }
        }

        hasSuperNodes = true;
        return superSink;
    }

    /*
     * Updates the capacities of edges connecting from SuperSource to energy sources
     * based on current available energy.
     */
    public void updateSuperSourceEdges() {
        if (superSource == null) {
            return;
        }

        List<GraphEdge> edges = getOutgoingEdges(superSource.getId());
        for (GraphEdge edge : edges) {
            EnergyNode targetNode = edge.getTarget();
            if (targetNode instanceof EnergySource) {
                EnergySource source = (EnergySource) targetNode;
                double availableEnergy = source.getAvailableEnergy();
                edge.setCapacity(availableEnergy);
            }
        }
    }

    /*
     * Updates the capacities of edges connecting from energy consumers to SuperSink
     * based on current demand.
     */
    public void updateSuperSinkEdges() {
        if (superSink == null) {
            return;
        }

        List<GraphEdge> edges = getIncomingEdges(superSink.getId());
        for (GraphEdge edge : edges) {
            EnergyNode sourceNode = edge.getSource();
            if (sourceNode instanceof EnergyConsumer) {
                EnergyConsumer consumer = (EnergyConsumer) sourceNode;
                double demand = consumer.getDemand();
                edge.setCapacity(demand);
            }
        }
    }

    /*
     * Updates all super node edges based on current energy availability and demand
     */
    public void updateSuperNodeEdges() {
        updateSuperSourceEdges();
        updateSuperSinkEdges();
    }

    /*
     * Adds both SuperSource and SuperSink nodes to the graph and connects them appropriately
     * superSourceId - The ID to use for the super source node
     * superSinkId  - The ID to use for the super sink node
     */
    public void addSuperNodes(String superSourceId, String superSinkId) {
        addSuperSource(superSourceId);
        addSuperSink(superSinkId);
    }

    /*
     * Removes super nodes and their associated edges from the graph
     */
    public void removeSuperNodes() {
        if (superSource != null) {
            removeNode(superSource.getId());
            superSource = null;
        }

        if (superSink != null) {
            removeNode(superSink.getId());
            superSink = null;
        }

        hasSuperNodes = false;
    }

    /*
     * Check if the graph has super nodes
     * return true if super nodes are present, false otherwise
     */
    public boolean hasSuperNodes() {
        return hasSuperNodes;
    }

    /*
     * Get the super source node
     * return the super source node or null if not present
     */
    public SuperSource getSuperSource() {
        return superSource;
    }

    /*
     * Get the super sink node
     * return the super sink node or null if not present
     */
    public SuperSink getSuperSink() {
        return superSink;
    }

    /*
     * Creates a graph with super nodes for max flow calculations
     * return a new graph with super source and sink
     */
    public Graph createFlowNetworkWithSuperNodes() {
        Graph flowNetwork = new Graph();

        // Copy all nodes and edges
        for (EnergyNode node : nodes.values()) {
            flowNetwork.addNode(node);
        }

        for (String sourceId : outgoingEdges.keySet()) {
            for (GraphEdge edge : outgoingEdges.get(sourceId)) {
                String targetId = edge.getTarget().getId();
                flowNetwork.addEdge(sourceId, targetId, edge.getCapacity());
            }
        }

        // Add super nodes
        flowNetwork.addSuperNodes("super_source", "super_sink");

        return flowNetwork;
    }


    /*
     * Finds an augmenting path from superSource to superSink using Breadth-First Search.
     * Only considers edges with residual capacity > 0.
     * superSource - The super source node
     * superSink -  The super sink node
     * parentEdges -  Map to store the parent edge of each node (for path reconstruction)
     * return true if an augmenting path exists, false otherwise
     */
    public boolean BFS(EnergyNode superSource, EnergyNode superSink,
                       Map<String, GraphEdge> parentEdges) {
        System.out.println("\n------ BFS Path Search ------");

        // Clear the parent edges map
        parentEdges.clear();

        // Set to keep track of visited nodes
        Set<String> visited = new HashSet<>();

        // Queue for BFS traversal
        Queue<EnergyNode> queue = new LinkedList<>();

        // Start from superSource
        queue.add(superSource);
        visited.add(superSource.getId());

        // BFS traversal
        while (!queue.isEmpty()) {
            EnergyNode current = queue.poll();

            // Get all outgoing edges from the current node
            List<GraphEdge> edges = getOutgoingEdges(current.getId());

            for (GraphEdge edge : edges) {
                EnergyNode target = edge.getTarget();
                String targetId = target.getId();

                // Only process if not visited and has residual capacity
                if (!visited.contains(targetId) && edge.getResidualCapacity() > 0) {
                    // Store the parent edge for this node
                    parentEdges.put(targetId, edge);
                    System.out.println("Added to path: " + current.getId() + " -> " + targetId +
                            " (capacity: " + edge.getCapacity() +
                            ", flow: " + edge.getFlow() +
                            ", residual: " + edge.getResidualCapacity() + ")");

                    // If we've reached the superSink, we found a path
                    if (targetId.equals(superSink.getId())) {
                        System.out.println("Found path to sink!");
                        return true;
                    }

                    // Mark as visited and add to queue
                    visited.add(targetId);
                    queue.add(target);
                }
            }
        }

        System.out.println("No augmenting path found");

        // No augmenting path found
        return false;
    }
}