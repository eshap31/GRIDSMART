package com.example.gridsmart;

import java.util.*;

/*
 * Represents the energy grid as a graph with energy sources and consumers as nodes,
 * and energy flows as directed edges with capacity constraints.
 */
public class Graph {
    private final Map<String, EnergyNode> nodes;
    private final Map<String, List<GraphEdge>> outgoingEdges; // Adjacency list representation
    private final Map<String, List<GraphEdge>> incomingEdges; // Reverse adjacency list for easy lookup

    /*
     * Creates a new empty graph
     */
    public Graph() {
        this.nodes = new HashMap<>();
        this.outgoingEdges = new HashMap<>();
        this.incomingEdges = new HashMap<>();
    }

    /*
     * Adds a node to the graph
     */
    public void addNode(EnergyNode node) {
        nodes.put(node.getId(), node);
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
    public GraphEdge addEdge(String sourceId, String targetId, double capacity, double weight) {
        EnergyNode source = nodes.get(sourceId);
        EnergyNode target = nodes.get(targetId);

        if (source == null || target == null) {
            throw new IllegalArgumentException("Source or target node not found in graph");
        }

        GraphEdge edge = new GraphEdge(source, target, capacity, weight);
        outgoingEdges.get(sourceId).add(edge);
        incomingEdges.get(targetId).add(edge);

        return edge;
    }

    /*
     * Adds a directed edge from source to target with default weight 1.0
     */
    public GraphEdge addEdge(String sourceId, String targetId, double capacity) {
        return addEdge(sourceId, targetId, capacity, 1.0);
    }

    /*
     * Removes an edge from the graph
     */
    public void removeEdge(String sourceId, String targetId) {
        List<GraphEdge> outEdges = outgoingEdges.getOrDefault(sourceId, Collections.emptyList());
        List<GraphEdge> inEdges = incomingEdges.getOrDefault(targetId, Collections.emptyList());

        // Find the edge to remove
        GraphEdge edgeToRemove = null;
        for (GraphEdge edge : outEdges) {
            if (edge.getTarget().getId().equals(targetId)) {
                edgeToRemove = edge;
                break;
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

                // Forward edge with residual capacity
                if (edge.getResidualCapacity() > 0) {
                    residualGraph.addEdge(sourceId, targetId, edge.getResidualCapacity(), edge.getWeight());
                }

                // Backward edge with flow as capacity
                if (edge.getFlow() > 0) {
                    residualGraph.addEdge(targetId, sourceId, edge.getFlow(), -edge.getWeight());
                }
            }
        }

        return residualGraph;
    }

    /*
     * Returns a string representation of the graph
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph with ").append(nodes.size()).append(" nodes and ");

        int edgeCount = 0;
        for (List<GraphEdge> edges : outgoingEdges.values()) {
            edgeCount += edges.size();
        }

        sb.append(edgeCount).append(" edges:\n");

        // Print nodes
        sb.append("Nodes:\n");
        for (EnergyNode node : nodes.values()) {
            sb.append("  ").append(node.getId()).append(" (").append(node.getNodeType()).append(")\n");
        }

        // Print edges
        sb.append("Edges:\n");
        for (String sourceId : outgoingEdges.keySet()) {
            for (GraphEdge edge : outgoingEdges.get(sourceId)) {
                sb.append("  ").append(sourceId).append(" -> ").append(edge.getTarget().getId())
                        .append(" [capacity=").append(edge.getCapacity())
                        .append(", flow=").append(edge.getFlow())
                        .append(", weight=").append(edge.getWeight()).append("]\n");
            }
        }

        return sb.toString();
    }
}