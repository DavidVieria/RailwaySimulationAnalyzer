package pt.ipp.isep.dei.domain.MDISC;

import java.util.*;

/**
 * Implements Dijkstra's algorithm to find the shortest path between two stations.
 * This functionality is required for US27.
 *
 */
public class US27_Dijkstra {

    private final Map<Integer, List<RailwayDataLoader.Line>> graph;
    private final Map<String, Integer> nameToId;
    private final Map<Integer, String> idToName;

    /**
     * Helper class to store a node and its distance for the Priority Queue.
     */
    private static class NodeDistance implements Comparable<NodeDistance> {
        int nodeId;
        double distance;

        NodeDistance(int nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    /**
     * Represents the result of the shortest path calculation.
     */
    public static class PathResult {
        public final List<String> path;
        public final double distance;
        public final boolean isPossible;        // Indicates if a path was found

        public PathResult(List<String> path, double distance) {
            this.path = path;
            this.distance = distance;
            this.isPossible = true;
        }

        public PathResult() {
            this.path = Collections.emptyList();
            this.distance = -1;
            this.isPossible = false;
        }
    }

    public US27_Dijkstra(RailwayDataLoader dataLoader) {
        this.graph = dataLoader.getGraph();
        this.nameToId = dataLoader.getNameToId();
        this.idToName = dataLoader.getIdToName();
    }

    /**
     * Finds the shortest path between a start and end station using Dijkstra's algorithm.
     *
     * @param startStationName The name of the starting station.
     * @param endStationName The name of the ending station.
     *
     * @return A PathResult object containing the path and distance.
     */
    public PathResult findShortestPath(String startStationName, String endStationName) {
        Integer startId = nameToId.get(startStationName);
        Integer endId = nameToId.get(endStationName);

        if (startId == null || endId == null) {
            System.err.println("⚠️ Error: One or both station names are invalid.");
            return new PathResult(); // Return impossible result
        }

        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Integer> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>();

        if (graph.isEmpty()) {
            System.err.println("⚠️ Error: Railway network data not loaded correctly.");
            return new PathResult();
        }

        // Initialization
        for (Integer stationId : graph.keySet()) {
            distances.put(stationId, Double.POSITIVE_INFINITY);
        }
        distances.put(startId, 0.0);
        pq.add(new NodeDistance(startId, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            int u = current.nodeId;

            // If we've found a shorter path already, skip
            if (current.distance > distances.get(u)) {
                continue;
            }

            // If we reached the destination, we can stop
            if (u == endId) {
                break;
            }

            // Explore neighbors
            if (graph.get(u) == null) continue;
            for (RailwayDataLoader.Line edge : graph.get(u)) {
                int v = edge.to;
                double weight = edge.length;
                double distanceThroughU = distances.get(u) + weight;

                if (distanceThroughU < distances.get(v)) {
                    distances.put(v, distanceThroughU);
                    predecessors.put(v, u);
                    pq.add(new NodeDistance(v, distanceThroughU));
                }
            }
        }

        // Reconstruct path if possible
        if (distances.get(endId) == Double.POSITIVE_INFINITY) {
            return new PathResult(); // No path found
        }

        LinkedList<String> path = new LinkedList<>();
        Integer currentId = endId;
        while (currentId != null) {
            path.addFirst(idToName.get(currentId));
            currentId = predecessors.get(currentId);
        }

        // Ensure the path starts with the start station
        if (path.isEmpty() || !path.getFirst().equals(startStationName)) {
            return new PathResult(); // Should not happen if a path is found
        }

        return new PathResult(path, distances.get(endId));
    }
}