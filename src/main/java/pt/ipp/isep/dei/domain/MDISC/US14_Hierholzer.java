package pt.ipp.isep.dei.domain.MDISC;

import java.util.*;

/**
 * US14_Hierholzer - Maintenance Route Finder (Eulerian Path/Circuit).
 * This class provides algorithms to find a maintenance route in a railway network,
 * specifically by searching for an Eulerian path or circuit using Hierholzer's algorithm.
 * It builds a relevant subgraph (all lines or only electrified lines), checks Eulerian conditions,
 * and constructs a valid route if possible. It also identifies possible starting stations
 * and validates the feasibility of maintenance routes for the current network scenario.
 */
public class US14_Hierholzer {

    private final RailwayDataLoader networkData;

    private Map<Integer, List<EulerEdge>> relevantAdj;
    private Map<Integer, Integer> degrees;
    private Set<Integer> relevantNodes;

    // Inner/auxiliar classes:

    /**
     * Inner class to represent an edge specifically for the Eulerian path algorithms.
     * It includes a 'used' flag essential for Hierholzer's algorithm.
     * We use this separate class to avoid modifying the original US13.Line objects.
     */
    private static class EulerEdge {
        final int to;          // Destination station ID
        final boolean electrified;      // Whether the line is electrified
        boolean used;        // Flag to track if the edge has been traversed

        EulerEdge(int to, boolean electrified) {
            this.to = to;
            this.electrified = electrified;
            this.used = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EulerEdge edge = (EulerEdge) o;
            // Consider edges equal if they connect to the same node and have the same electrification
            return to == edge.to && electrified == edge.electrified;
        }

        @Override
        public int hashCode() {
            return Objects.hash(to, electrified);
        }
    }


    /**
     * Utility class for storing Eulerian path/circuit analysis results.
     */
    private static class EulerianInfo {
        boolean hasEulerianPathOrCircuit;
        boolean hasCircuit;
        boolean hasPath;
        int oddDegreeCount;
        List<Integer> oddDegreeNodes;

        /**
         * Constructs a new EulerianInfo object.
         * @param valid True if an Eulerian path or circuit exists, false otherwise.
         * @param circuit True if an Eulerian circuit exists.
         * @param path True if an Eulerian path exists.
         * @param oddCount The count of nodes with odd degrees.
         * @param oddNodes A list of nodes with odd degrees.
         */
        public EulerianInfo(boolean valid, boolean circuit, boolean path, int oddCount, List<Integer> oddNodes) {
            this.hasEulerianPathOrCircuit = valid;
            this.hasCircuit = circuit;
            this.hasPath = path;
            this.oddDegreeCount = oddCount;
            this.oddDegreeNodes = oddNodes;
        }
    }

    // Constructor:

    /**
     * Constructs a US14 instance for maintenance route analysis.
     *
     * @param networkData The loaded railway network data.
     * @throws IllegalArgumentException if networkData is null.
     */
    public US14_Hierholzer(RailwayDataLoader networkData) {
        if (networkData == null) {
            throw new IllegalArgumentException("⚠️ Error: Rail network data not found.");
        }
        this.networkData = networkData;
    }

    // Methods:

    /**
     * Builds the adjacency list representation for the relevant subgraph (all or electrified)
     * using EulerEdge objects (for tracking usage) and calculates node degrees for this subgraph.
     *
     * @param onlyElectrified If true, only electrified lines are included.
     */
    private void buildRelevantGraphAndDegrees(boolean onlyElectrified) {
        relevantAdj = new HashMap<>();
        degrees = new HashMap<>();
        relevantNodes = new HashSet<>();

        if (networkData.getGraph() == null) {
            System.err.println("⚠️ Error: The railway network data is not loaded correctly.");
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
                    relevantAdj.putIfAbsent(to, new ArrayList<>()); // Important for Hierholzer's

                    // Add the EulerEdge representation for Hierholzer's algorithm
                    // We add edges in both directions to represent the undirected graph for traversal
                    relevantAdj.get(from).add(new EulerEdge(to, line.electrified));

                    // Increment degrees for both nodes involved in the edge
                    degrees.put(from, degrees.getOrDefault(from, 0) + 1);
                }
            }
        }
    }


    /**
     * Analyzes the relevant subgraph to determine if an Eulerian path or circuit is possible,
     * and identifies odd-degree nodes.
     *
     * @return EulerianInfo with feasibility and odd-degree node information.
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
     * Validates if the selected starting station is valid for an Eulerian path/circuit,
     * given the current subgraph and degree conditions.
     *
     * @param startNodeId      The candidate starting station ID.
     * @param startStation     The name of the starting station.
     * @param onlyElectrified  Whether only electrified lines are considered.
     * @return true if the start is valid, false otherwise.
     */
    private boolean validateStartStationForEulerianRoute(int startNodeId, String startStation, boolean onlyElectrified) {
        EulerianInfo validateInfo = analyzeEulerianConditions();

        // If no Eulerian path or circuit is possible at all
        if (!validateInfo.hasEulerianPathOrCircuit) {
            System.err.println("⚠️ Warning - For the current network scenario, it is not possible to find a maintenance route that passes through all the lines exactly once.");
            return false;
        }

        int startNodeDegree = degrees.getOrDefault(startNodeId, 0);

        // If a path is possible but the selected start node has even degree (invalid)
        if (validateInfo.hasPath && startNodeDegree % 2 == 0) {
            System.err.println("⚠️ Warning - For the current network scenario, a maintenance route is possible, " +
                    "but it must start at one station with an odd number of relevant lines.");
            return false;
        }

        // If a circuit is possible but the selected station is isolated
        if (validateInfo.hasCircuit && startNodeDegree == 0 && !relevantNodes.isEmpty()) {
            System.err.println("⚠️ Warning: Although an Eulerian circuit may exist in the current network, the selected start station '" +
                    startStation + "' is not connected to any " + (onlyElectrified ? "electrified " : "") + "railway lines. Please select a different station from the available options for the maintenance route.");
            return false;
        }

        return true;
    }

    /**
     * Returns a list of possible starting station names for a maintenance route (Eulerian path/circuit),
     * or an empty list if not possible in the current scenario.
     *
     * @param onlyElectrified If true, considers only electrified lines.
     * @return List of valid starting station names.
     */
    public List<String> getPotentialStartStations(boolean onlyElectrified) {
        // Build subgraph and compute degrees for relevant lines
        buildRelevantGraphAndDegrees(onlyElectrified);
        List<String> validStarts = new ArrayList<>();

        // Handle trivial case - no relevant lines at all
        if (relevantNodes.isEmpty()) {
            System.err.println("⚠️ Warning: No relevant lines found in the network. No maintenance route possible.");
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
            System.err.println("⚠️ Warning: It is not possible to find a maintenance route in the current network scenario because there is a station or a group of stations without " +
                    (onlyElectrified ? "electrified " : "") +
                    "lines connecting to the rest of the network (the relevant subgraph is disconnected), or there are no relevant lines.\n");
            return validStarts;
        }

        // Analyze Eulerian feasibility and get possible start nodes
        EulerianInfo validateInfo = analyzeEulerianConditions();
        if (!validateInfo.hasEulerianPathOrCircuit) {
            System.out.println("⚠️ Warning: In this scenario, it is not possible to find a maintenance route that passes through all the lines exactly once.");
            System.out.println("To be able to find a maintenance route, all relevant stations must have an even number of lines (circuit), or exactly two stations must have an odd number of lines (path).");
            return validStarts;
        }

        Map<Integer, String> idToNameMap = getIdToNameMap();

        String name;
        if (validateInfo.hasCircuit) {
            // Eulerian circuit: any node with degree > 0 is valid
            for (int node : relevantNodes) {
                name = idToNameMap.get(node);
                if (degrees.getOrDefault(node, 0) > 0 && validateStartStationForEulerianRoute(node, name, onlyElectrified)) {
                    validStarts.add(idToNameMap.get(node));
                }
            }
        } else if (validateInfo.hasPath) {
            // Eulerian path: start at one of the odd-degree nodes
            for (int node : validateInfo.oddDegreeNodes) {
                name = idToNameMap.get(node);
                if (validateStartStationForEulerianRoute(node, name, onlyElectrified)) {
                    validStarts.add(idToNameMap.get(node));
                }
            }
        }

        // Sort for consistent and user-friendly output
        Collections.sort(validStarts);
        return validStarts;
    }


    /**
     * Finds a maintenance route (Eulerian path/circuit) starting from a specific station.
     * Returns the route as a list of station names, or null if not possible.
     *
     * @param startStation The name of the starting station.
     * @return List of station names representing the route, or null if not possible.
     */
    public List<String> findMaintenanceRoute(String startStation) {

        int startNodeId = networkData.getNameToId().get(startStation);

        // Find the Path using Hierholzer's Algorithm
        List<Integer> pathIds = findEulerianPathHierholzer(relevantAdj, startNodeId);

        if (pathIds == null) {
            System.err.println("❌ Warning: Failed to compute the Eulerian path/circuit for maintenance using Hierholzer's algorithm.");
            return null;
        }


        // Convert Path IDs to Station Names
        List<String> pathNames = new LinkedList<>(); // Use LinkedList for efficient addFirst/addLast if needed
        Map<Integer, String> idToNameMap = getIdToNameMap(); // Helper to get reverse mapping

        for (int id : pathIds) {
            String name = idToNameMap.get(id);
            if (name != null) {
                pathNames.add(name);
            } else {
                System.err.println("⚠️ Warning: Station ID " + id + " found in path but has no corresponding name." +
                        " Please verify the data integrity.");
            }
        }

        // Final Verification:
        // Check if all relevant edges were actually used by comparing path length to edge count.
        int expectedEdges = 0;
        for(int degree : degrees.values()){
            expectedEdges += degree;
        }
        expectedEdges /= 2; // Each edge counted twice in degree sum

        // Path length is the number of stations - 1
        if(pathNames.size() - 1 != expectedEdges) {
            System.err.println("⚠️ Warning: The generated path length (" + (pathNames.size() - 1) +
                    " edges) does not match the expected number of relevant edges (" + expectedEdges +
                    "). There might be an issue.");
        }

        return pathNames;
    }


    /**
     * Checks if the relevant subgraph is connected (all nodes with degree > 0 are reachable).
     *
     * @param startNodeId Node ID to start DFS from.
     * @return true if the subgraph is connected, false otherwise.
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
                if(actualStartNode == -1 || node == startNodeId){ // Prioritize startNodeId if valid
                    actualStartNode = node;
                }
            }
        }

        // If no nodes have any edges, it's connected (trivially)
        if (nodesWithEdgesCount == 0) {
            return true;
        }

        // If the designated start node has no edges, but others do, the graph cannot be connected from it.
        // However, the check should start from *any* node with edges.
        if(actualStartNode == -1) {
            return false;
        }


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
                        if(relevantAdj.get(v) != null && !relevantAdj.get(v).isEmpty()){
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
     * Implements Hierholzer's algorithm to find an Eulerian path or circuit.
     * Assumes the input graph (adj) meets the conditions (connectivity and degrees)
     * checked previously. Modifies the 'used' flag in the EulerEdge objects within adj.
     *
     * @param adj         The adjacency list of the relevant subgraph (using EulerEdge).
     * The 'used' flags in this map WILL BE MODIFIED.
     * @param startNodeId The ID of the node to start the path from.
     * @return A List of station IDs representing the Eulerian path/circuit, or null if an error occurs.
     */
    private List<Integer> findEulerianPathHierholzer(Map<Integer, List<EulerEdge>> adj, int startNodeId) {
        // --- Create a Deep Copy of Adjacency List to Modify ---
        // This is crucial to avoid modifying the original list passed if it's reused,
        // and to ensure 'used' flags are reset for each call if needed elsewhere.
        // However, the current structure modifies the list passed in. If reuse is needed,
        // a deep copy mechanism for EulerEdge lists would be required here.
        // For simplicity now, we assume 'adj' can be modified directly for this run.

        // Reset 'used' flags if this method could be called multiple times on the same adj map instance
        for(List<EulerEdge> edges : adj.values()){
            for(EulerEdge edge : edges){
                edge.used = false;
            }
        }


        LinkedList<Integer> path = new LinkedList<>(); // Stores the final path
        Stack<Integer> currentPathStack = new Stack<>(); // Stack for tracking the current traversal

        currentPathStack.push(startNodeId); // Start with the designated start node

        while (!currentPathStack.isEmpty()) {
            int u = currentPathStack.peek(); // Get the current node

            List<EulerEdge> neighbors = adj.get(u);
            EulerEdge nextEdge = null;

            // Find the first unused edge outgoing from node u
            if (neighbors != null) {
                for (EulerEdge edge : neighbors) {
                    if (!edge.used) {
                        nextEdge = edge;
                        break;
                    }
                }
            }

            if (nextEdge != null) {
                // Mark this edge u -> v as used
                nextEdge.used = true;

                // Mark the reverse edge v -> u as used too!
                // Find the corresponding edge in the adjacency list of v
                int v = nextEdge.to;
                List<EulerEdge> reverseNeighbors = adj.get(v);
                boolean foundReverse = false;
                if (reverseNeighbors != null) {
                    for (EulerEdge reverseEdge : reverseNeighbors) {
                        // Match destination AND check if it's already used (it shouldn't be if logic is correct)
                        if (reverseEdge.to == u && !reverseEdge.used) {
                            // Optional stricter check: ensure electrification matches if parallel lines possible
                            if (reverseEdge.electrified == nextEdge.electrified) {
                                reverseEdge.used = true;
                                foundReverse = true;
                                break; // Assume only one matching reverse edge per forward edge
                            }
                        }
                    }
                }
                if (!foundReverse && adj.containsKey(v)) { // Added adj.containsKey(v) check
                    System.err.println("❌ Error in Hierholzer: Could not find or mark reverse edge for " + u +
                            " -> " + v + ". Graph might be inconsistent.");
                }


                // Push the neighbor (v) onto the stack to continue the path
                currentPathStack.push(v);
            } else {
                path.addFirst(currentPathStack.pop());
            }
        }

        return path;
    }

    /**
     * Retrieves the mapping from node IDs to station names.
     * @return A map where keys are node IDs and values are station names.
     */
    private Map<Integer, String> getIdToNameMap() {
        return networkData.getIdToName();
    }
}
