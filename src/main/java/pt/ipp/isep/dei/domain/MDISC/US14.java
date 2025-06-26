package pt.ipp.isep.dei.domain.MDISC;

import java.util.*;

/**
 * US14 - Maintenance Route Finder (Eulerian Path/Circuit).
 * This class provides algorithms to find a maintenance route in a railway network,
 * specifically by searching for an Eulerian path or circuit using Fleury's algorithm.
 * It builds a relevant subgraph (all lines or only electrified lines), checks Eulerian conditions,
 * and constructs a valid route if possible. It also identifies possible starting stations
 * and validates the feasibility of maintenance routes for the current network scenario.
 */
public class US14 {

    private final RailwayDataLoader networkData;

    private Map<Integer, List<EulerEdge>> relevantAdj;
    private Map<Integer, Integer> degrees;
    private Set<Integer> relevantNodes;

    // Inner/auxiliar classes:

    /**
     * Represents an edge in the graph for Eulerian path/circuit calculations.
     * Each edge has a destination node, an electrification status, a unique ID, and a usage flag.
     */
    private static class EulerEdge {
        final int to;          // Destination station ID
        final boolean electrified;      // Whether the line is electrified
        final String edgeId;        // Unique ID for the bidirectional edge (smallerId-largerId)
        boolean used;        // Flag to track if the edge has been traversed

        /**
         * Constructs a new EulerEdge.
         *
         * @param from The starting node of the edge.
         * @param to The ending node of the edge.
         * @param electrified True if the edge is electrified, false otherwise.
         */
        EulerEdge(int from, int to, boolean electrified) {
            this.to = to;
            this.electrified = electrified;
            this.edgeId = from < to ? from + "-" + to : to + "-" + from;
            this.used = false;
        }
    }

    /**
     * Stores information about the Eulerian properties of a graph.
     * Indicates whether an Eulerian path or circuit exists, and provides details about odd-degree nodes.
     */
    private static class EulerianInfo {
        boolean hasEulerianPathOrCircuit;
        boolean hasCircuit;
        boolean hasPath;
        int oddDegreeCount;
        List<Integer> oddDegreeNodes;

        /**
         * Constructs a new EulerianInfo object.
         *
         * @param valid True if an Eulerian path or circuit exists, false otherwise.
         * @param circuit True if an Eulerian circuit exists.
         * @param path True if an Eulerian path exists.
         * @param oddCount The count of nodes with odd degrees.
         * @param oddNodes A list of nodes with odd degrees.
         */
        EulerianInfo(boolean valid, boolean circuit, boolean path, int oddCount, List<Integer> oddNodes) {
            this.hasEulerianPathOrCircuit = valid;
            this.hasCircuit = circuit;
            this.hasPath = path;
            this.oddDegreeCount = oddCount;
            this.oddDegreeNodes = oddNodes;
        }
    }

    // Constructor:

    /**
     * Constructs a new US14 object with the given RailwayDataLoader.
     *
     * @param networkData The data loader for railway network information.
     */
    public US14(RailwayDataLoader networkData) {
        if (networkData == null) {
            throw new IllegalArgumentException("⚠️ Error: Rail network data not found.");
        }
        this.networkData = networkData;
    }

    // Methods:

    /**
     * Builds the relevant subgraph (either all lines or only electrified lines) and calculates node degrees.
     *
     * @param onlyElectrified True to consider only electrified lines, false for all lines.
     */
    private void buildRelevantGraphAndDegrees(boolean onlyElectrified) {
        relevantAdj = new HashMap<>();
        degrees = new HashMap<>();
        relevantNodes = new HashSet<>();

        if (networkData.getGraph() == null) {
            System.err.println("⚠️ Error: Railway network data not loaded correctly.");
            return;  // Handle case where graph wasn't loaded
        }

        // Iterate through the original graph to build the relevant subgraph
        for (Map.Entry<Integer, List<RailwayDataLoader.Line>> entry : networkData.getGraph().entrySet()) {
            int from = entry.getKey();
            List<RailwayDataLoader.Line> originalLines = entry.getValue();
            if (originalLines == null) continue;

            for (RailwayDataLoader.Line line : originalLines) {
                int to = line.to;

                // Check if the line should be included based on the 'onlyElectrified' flag
                if (!onlyElectrified || line.electrified) {
                    // Only adds nodes that participate in at least one relevant line
                    relevantNodes.add(from);
                    relevantNodes.add(to);

                    // Ensure nodes exist in the adjacency list map
                    relevantAdj.putIfAbsent(from, new ArrayList<>());
                    relevantAdj.putIfAbsent(to, new ArrayList<>());

                    // Only add one edge per pair (bidirectional)
                    String edgeId = from < to ? from + "-" + to : to + "-" + from;
                    if (!containsEdge(relevantAdj.get(from), edgeId)) {
                        relevantAdj.get(from).add(new EulerEdge(from, to, line.electrified));
                        relevantAdj.get(to).add(new EulerEdge(to, from, line.electrified));
                        degrees.put(from, degrees.getOrDefault(from, 0) + 1);
                        degrees.put(to, degrees.getOrDefault(to, 0) + 1);
                    }
                }
            }
        }
    }

    /**
     * Checks if a list of EulerEdges already contains an edge with a given ID.
     *
     * @param edges The list of EulerEdges to check.
     * @param edgeId The unique ID of the edge to look for.
     * @return True if the edge is found, false otherwise.
     */
    private boolean containsEdge(List<EulerEdge> edges, String edgeId) {
        for (EulerEdge e : edges) {
            if (e.edgeId.equals(edgeId)) return true;
        }
        return false;
    }

    /**
     * Analyzes the Eulerian conditions of the relevant subgraph.
     * Determines if an Eulerian path or circuit exists based on node degrees.
     *
     * @return An EulerianInfo object containing the analysis results.
     */
    private EulerianInfo analyzeEulerianConditions() {
        int oddDegreeCount = 0;
        List<Integer> oddNodes = new ArrayList<>();

        for (int node : relevantNodes) {
            if (degrees.getOrDefault(node, 0) % 2 != 0) {
                oddDegreeCount++;
                oddNodes.add(node);
            }
        }

        boolean hasCircuit = (oddDegreeCount == 0);
        boolean hasPath = (oddDegreeCount == 2);
        boolean valid = (hasCircuit || hasPath);

        return new EulerianInfo(valid, hasCircuit, hasPath, oddDegreeCount, oddNodes);
    }

    /**
     * Checks if the relevant subgraph is connected.
     *
     * @param startNodeId An arbitrary starting node ID to begin the connectivity check.
     * @return True if the relevant subgraph is connected, false otherwise.
     */
    private boolean isRelevantSubgraphConnected(int startNodeId) {
        if (relevantNodes.isEmpty()) {
            System.err.println("⚠️ Warning: The data is empty. Cannot check connectivity.");
            return true; // An empty graph is trivially connected
        }

        Set<Integer> visited = new HashSet<>();
        Stack<Integer> stack = new Stack<>();

        // Count nodes that actually have relevant edges (degree > 0)
        int nodesWithEdgesCount = 0;
        int actualStartNode = -1; // Find a valid start node with edges

        for (int node : relevantNodes) {
            List<EulerEdge> edges = relevantAdj.get(node);
            if (edges != null && !edges.isEmpty()) {
                nodesWithEdgesCount++;

                // Use the provided startNodeId if it has edges, otherwise find the first one that does
                if (actualStartNode == -1 || node == startNodeId) {  // Prioritize startNodeId if valid
                    actualStartNode = node;
                }
            }
        }

        // If no nodes have any edges, it's connected (trivially)
        if (nodesWithEdgesCount == 0) return true;

        // If the designated start node has no edges, but others do, the graph cannot be connected from it.
        // However, the check should start from *any* node with edges.
        if (actualStartNode == -1) return false;

        // Start DFS from the actual start node with edges
        stack.push(actualStartNode);
        visited.add(actualStartNode);
        int visitedNodesWithEdgesCount = 0;
        if(relevantAdj.get(actualStartNode) != null && !relevantAdj.get(actualStartNode).isEmpty()){
            visitedNodesWithEdgesCount = 1; // Count the start node if it has edges
        }

        while (!stack.isEmpty()) {
            int u = stack.pop();

            List<EulerEdge> neighbors = relevantAdj.get(u);
            if (neighbors != null) {
                for (EulerEdge edge : neighbors) {
                    int v = edge.to;
                    // Check if v is a relevant node (it should be if added correctly)
                    // and if it hasn't been visited yet.
                    if (relevantNodes.contains(v) && !visited.contains(v)) {
                        visited.add(v);
                        stack.push(v);
                        // Increment count if this newly visited node has edges
                        if (relevantAdj.get(v) != null && !relevantAdj.get(v).isEmpty()) {
                            visitedNodesWithEdgesCount++;
                        }
                    }
                }
            }
        }

        // The subgraph is connected if the number of visited nodes *that have edges*
        // equals the total number of nodes *that have edges*.
        return visitedNodesWithEdgesCount == nodesWithEdgesCount;
    }


    /**
     * Returns a list of potential start stations for a maintenance route.
     * The stations are determined based on Eulerian path/circuit conditions
     * and whether only electrified lines are considered.
     *
     * @param onlyElectrified True to consider only electrified lines, false for all lines.
     * @return A sorted list of station names that can be starting points for a maintenance route.
     */
    public List<String> getPotentialStartStations(boolean onlyElectrified) {
        // Build subgraph and compute degrees for relevant lines
        buildRelevantGraphAndDegrees(onlyElectrified);
        List<String> validStarts = new ArrayList<>();

        // Handle trivial case - no relevant lines at all
        if (relevantNodes.isEmpty()) {
            System.err.println("⚠️ Warning: No relevant lines in the network. Cannot find maintenance route.");
            return validStarts;
        }

        // Pick a node with degree > 0 to check subgraph connectivity
        int checkStartNode = -1;
        for (int node : relevantNodes) {
            if (degrees.getOrDefault(node, 0) > 0) {
                checkStartNode = node;
                break;
            }
        }

        // If all nodes are isolated or the subgraph is disconnected
        if (checkStartNode == -1 || !isRelevantSubgraphConnected(checkStartNode)) {
            System.out.println("⚠️ Warning: It is not possible to find a maintenance route in the current network scenario because there is a station \nor a group of stations without " +
                    (onlyElectrified ? "electrified " : "") +
                    "lines connecting to the rest of the network (the relevant subgraph is disconnected), or there are no relevant lines.\n");
            return validStarts;
        }

        // Analyze Eulerian feasibility and get possible start nodes
        EulerianInfo validateInfo = analyzeEulerianConditions();
        if (!validateInfo.hasEulerianPathOrCircuit) {
            System.out.println("⚠️ Warning: Cannot find a maintenance route that passes through all lines exactly once.");
            System.out.println("For this to be possible, all relevant vertices must have an even degree (circuit), or exactly two must have an odd degree (path).");
            return validStarts;
        }

        Map<Integer, String> idToNameMap = networkData.getIdToName();

        String name;
        if (validateInfo.hasCircuit) {
            // Eulerian circuit: any node with degree > 0 is valid
            for (int node : relevantNodes) {
                name = idToNameMap.get(node);
                if (degrees.getOrDefault(node, 0) > 0) {
                    validStarts.add(name);
                }
            }
        } else if (validateInfo.hasPath) {
            // Eulerian path: start at one of the odd-degree nodes
            for (int node : validateInfo.oddDegreeNodes) {
                name = idToNameMap.get(node);
                validStarts.add(name);
            }
        }

        // Sort for consistent and user-friendly output
        Collections.sort(validStarts);
        return validStarts;
    }

    /**
     * Finds a maintenance route (Eulerian path/circuit) starting from a given station.
     * This implementation uses a modified Fleury's algorithm to ensure each bidirectional
     * line is traversed exactly once.
     *
     * @param startStationName The name of the starting station for the maintenance route.
     * @return A list of station names representing the maintenance route, or an empty list if no such route exists.
     */
    public List<String> findMaintenanceRoute(String startStationName) {
        Integer startNode = networkData.getNameToId().get(startStationName);

        if (startNode == null) return Collections.emptyList();

        if (relevantAdj == null || degrees == null) return Collections.emptyList();

        // Deep copy of the graph for manipulation
        Map<Integer, List<EulerEdge>> adjCopy = new HashMap<>();
        Map<String, Boolean> usedEdges = new HashMap<>(); // edgeId -> used

        for (Map.Entry<Integer, List<EulerEdge>> entry : relevantAdj.entrySet()) {
            List<EulerEdge> edges = new ArrayList<>();
            for (EulerEdge e : entry.getValue()) {
                // Ensure correct 'from' node for EulerEdge construction when copying
                edges.add(new EulerEdge(entry.getKey(), e.to, e.electrified));
            }
            adjCopy.put(entry.getKey(), edges);
        }

        List<Integer> path = new ArrayList<>();
        fleuryDFS(startNode, adjCopy, usedEdges, path);

        // Check if all edges were used
        int totalEdges = 0;
        for (List<EulerEdge> edges : adjCopy.values()) totalEdges += edges.size();
        // Each bidirectional edge appears twice (from-to and to-from)
        // So, if all edges were used, the remaining size of each list should be 0.
        // We ensure totalEdges is 0 to verify all original edges were processed.
        if (totalEdges > 0) return Collections.emptyList();


        // Convert IDs to names
        Map<Integer, String> idToName = networkData.getIdToName();
        List<String> result = new ArrayList<>();
        for (int id : path) result.add(idToName.get(id));

        return result;
    }

    /**
     * Recursive implementation of Fleury's algorithm.
     * This function finds an Eulerian path/circuit in the graph, ensuring each edge is used only once.
     *
     * @param current The current node.
     * @param adj The adjacency list representation of the graph (mutable copy).
     * @param usedEdges A map to track used edges by their unique ID.
     * @param path The list to build the Eulerian path/circuit.
     */
    private void fleuryDFS(int current, Map<Integer, List<EulerEdge>> adj, Map<String, Boolean> usedEdges, List<Integer> path) {
        List<EulerEdge> edges = adj.get(current);
        while (edges != null && !edges.isEmpty()) {
            EulerEdge chosen = null;
            // Choose an edge that is not a bridge if possible
            for (int i = 0; i < edges.size(); i++) {
                EulerEdge e = edges.get(i);
                if (!usedEdges.getOrDefault(e.edgeId, false)) {
                    if (edges.size() == 1 || !isEdgeBridge(current, e.to, adj, usedEdges, e.edgeId)) {
                        chosen = e;
                        break;
                    }
                }
            }
            if (chosen == null) break; // No more available edges

            // Mark the edge as used
            usedEdges.put(chosen.edgeId, true);

            // Remove the edge from both sides
            removeEdge(current, chosen.to, chosen.edgeId, adj);
            removeEdge(chosen.to, current, chosen.edgeId, adj);

            fleuryDFS(chosen.to, adj, usedEdges, path);
        }
        path.add(0, current);
    }

    /**
     * Removes a specific edge from the adjacency list.
     *
     * @param from The starting node of the edge.
     * @param to The ending node of the edge.
     * @param edgeId The unique ID of the edge to remove.
     * @param adj The adjacency list.
     */
    private void removeEdge(int from, int to, String edgeId, Map<Integer, List<EulerEdge>> adj) {
        List<EulerEdge> list = adj.get(from);
        if (list != null) {
            for (Iterator<EulerEdge> it = list.iterator(); it.hasNext(); ) {
                EulerEdge e = it.next();
                if (e.to == to && e.edgeId.equals(edgeId)) {
                    it.remove();
                    break;
                }
            }
        }
    }

    /**
     * Checks if the edge (u, v) is a bridge in the current graph state (considering already used edges).
     * A bridge is an edge whose removal increases the number of connected components.
     *
     * @param u The starting node of the edge.
     * @param v The ending node of the edge.
     * @param adj The adjacency list of the graph (mutable copy).
     * @param usedEdges A map to track used edges by their unique ID.
     * @param edgeId The unique ID of the edge being checked.
     * @return True if the edge (u, v) is a bridge, false otherwise.
     */
    private boolean isEdgeBridge(int u, int v, Map<Integer, List<EulerEdge>> adj, Map<String, Boolean> usedEdges, String edgeId) {
        int before = dfsCount(u, new HashSet<>(), adj, usedEdges);
        // Temporarily remove the edge u-v and v-u
        removeEdge(u, v, edgeId, adj);
        removeEdge(v, u, edgeId, adj);

        int after = dfsCount(u, new HashSet<>(), adj, usedEdges);
        // Restore the edge
        adj.get(u).add(new EulerEdge(u, v, true)); // Electrified status does not matter for bridge check
        adj.get(v).add(new EulerEdge(v, u, true)); // Electrified status does not matter for bridge check
        return after < before;
    }

    /**
     * Counts the number of reachable vertices from a given starting node using DFS,
     * ignoring edges that have already been used.
     *
     * @param current The starting node for the DFS.
     * @param visited A set to keep track of visited nodes during the DFS.
     * @param adj The adjacency list of the graph.
     * @param usedEdges A map to track used edges by their unique ID.
     * @return The count of reachable vertices.
     */
    private int dfsCount(int current, Set<Integer> visited, Map<Integer, List<EulerEdge>> adj, Map<String, Boolean> usedEdges) {
        visited.add(current);
        int count = 1;
        List<EulerEdge> edges = adj.get(current);
        if (edges != null) {
            for (EulerEdge e : edges) {
                if (!usedEdges.getOrDefault(e.edgeId, false) && !visited.contains(e.to)) {
                    count += dfsCount(e.to, visited, adj, usedEdges);
                }
            }
        }
        return count;
    }
}
