package pt.ipp.isep.dei.domain.MDISC;

import java.util.*;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;


/**
 * US13 - Railway Network Connectivity and Accessibility Tools.
 * This class provides utilities for analyzing railway network connectivity,
 * checking accessibility between stations based on train types, and
 * visualizing the railway network. It also includes tools for determining
 * network-wide properties such as Eulerian trails and full connectivity.
 **/
public class US13 {

    public Map<String, Integer> nameToId;
    private final Map<Integer, String> idToName;
    private final Map<Integer, RailwayDataLoader.Station> stations;
    public Map<Integer, List<RailwayDataLoader.Line>> graph;


    public US13(RailwayDataLoader dataLoader) {
        this.nameToId = dataLoader.getNameToId();
        this.idToName = dataLoader.getIdToName();
        this.stations = dataLoader.getStations();
        this.graph = dataLoader.getGraph();

        for (Map.Entry<Integer, List<RailwayDataLoader.Line>> entry : dataLoader.getGraph().entrySet()) {
            List<RailwayDataLoader.Line> lines = new ArrayList<>();
            for (RailwayDataLoader.Line l : entry.getValue()) {
                lines.add(new RailwayDataLoader.Line(l.from, l.to, l.length, l.electrified));
            }
            graph.put(entry.getKey(), lines);
        }
    }


    /**
     * Checks if there is a path between two stations for a given train type.
     * This method determines whether a specific train type can travel from one station to another
     * based on network connectivity and line electrification status.
     *
     * @param from Name of the origin station.
     * @param to Name of the destination station.
     * @param trainType Type of train ("steam", "diesel", "electric").
     * @return true if reachable, false otherwise.
     */
    public boolean isReachable(String from, String to, String trainType) {
        Integer fromId = nameToId.get(from);
        Integer toId = nameToId.get(to);
        if (fromId == null || toId == null) return false;

        Set<Integer> visited = new HashSet<>();
        return dfs(fromId, toId, visited, trainType.toLowerCase());
    }


    /**
     * Depth-first search to check reachability between two stations for a given train type.
     * This helper method implements a recursive DFS algorithm that respects train type constraints,
     * particularly for electric trains that can only travel on electrified lines.
     *
     * @param current Current station ID.
     * @param target Target station ID.
     * @param visited Set of visited station IDs.
     * @param trainType Type of train ("steam", "diesel", "electric").
     * @return true if target is reachable, false otherwise.
     */
    private boolean dfs(int current, int target, Set<Integer> visited, String trainType) {
        if (current == target) return true;
        visited.add(current);

        for (RailwayDataLoader.Line l : graph.get(current)) {
            if (!visited.contains(l.to)) {
                if ("electric".equals(trainType) && !l.electrified) continue;
                if (dfs(l.to, target, visited, trainType)) return true;
            }
        }
        return false;
    }


    /**
     * Checks if a station is of a given type.
     * Validates whether a specified station matches the requested type classification.
     *
     * @param name Station name.
     * @param desiredType Desired type ("Depot", "Station", "Terminal").
     * @return true if the station matches the type, false otherwise.
     */
    public boolean isStationOfType(String name, String desiredType) {
        Integer id = nameToId.get(name);
        if (id == null) return false;
        RailwayDataLoader.Station s = stations.get(id);
        return s != null && s.type.equalsIgnoreCase(desiredType);
    }

    /**
     * Visualizes the current railway network using GraphStream.
     * Creates an interactive visual representation of the railway network, displaying
     * stations as nodes and railway lines as edges with appropriate styling based on
     * electrification status. The visualization opens in a separate window.
     */
    public void visualizeWithGraphStream() {
        System.setProperty("org.graphstream.ui", "swing");
        Graph gsGraph = new SingleGraph("Railway Network");

        // Set the stylesheet to use a 'base' class for nodes
        gsGraph.setAttribute("ui.stylesheet",
                "graph { padding: 31px; fill-color: #f8f8ff; }" +
                        // Use a class for base node styles
                        "node.base { size: 18px, 20px; fill-color: #DB6F0F; stroke-mode: plain; stroke-color: #222; shape: rounded-box; text-alignment: under; text-size: 13px; text-style: bold; text-offset: 0, 18; text-color: #222; }" +
                        "edge { size: 3px; fill-color: #C0C0C0; text-size: 13px; text-style: italic; text-background-mode: plain; text-background-color: #fffbe7; text-offset: 0, 8; }" +
                        "edge.electrified { fill-color: #009ac1; size: 4px; }"
        );

        // Add all nodes and assign the 'base' class
        for (Map.Entry<Integer, RailwayDataLoader.Station> entry : stations.entrySet()) {
            RailwayDataLoader.Station s = entry.getValue();
            Node node = gsGraph.addNode(String.valueOf(s.id));
            node.setAttribute("ui.label", s.name + " (" + s.type + ")");
            node.setAttribute("ui.class", "base");
        }

        // Prepare the map of unique edges
        Map<String, RailwayDataLoader.Line> edgeMap = new HashMap<>();
        for (Map.Entry<Integer, List<RailwayDataLoader.Line>> entry : graph.entrySet()) {
            for (RailwayDataLoader.Line l : entry.getValue()) {
                String edgeId = Math.min(l.from, l.to) + "-" + Math.max(l.from, l.to);
                RailwayDataLoader.Line existing = edgeMap.get(edgeId);
                if (existing == null || (!existing.electrified && l.electrified)) {
                    edgeMap.put(edgeId, l);
                }
            }
        }

        // Add all edges with their styles
        for (Map.Entry<String, RailwayDataLoader.Line> entry : edgeMap.entrySet()) {
            RailwayDataLoader.Line l = entry.getValue();
            Edge edge = gsGraph.addEdge(entry.getKey(), String.valueOf(l.from), String.valueOf(l.to));
            edge.setAttribute("ui.label", l.length + "km");
            if (l.electrified) {
                edge.setAttribute("ui.class", "electrified");
            }
        }

        // Display the graph
        gsGraph.display();
        gsGraph.addAttribute("ui.quality");
        gsGraph.addAttribute("ui.antialias");
        gsGraph.addAttribute("layout.force", 1.0);
        gsGraph.addAttribute("layout.quality", 5);
    }

    /**
     * Visualizes the railway network with a highlighted path.
     * This method creates an interactive visual representation of the railway network,
     * highlighting a specified path in red.
     *
     * @param pathToHighlight List of station names representing the path to highlight.
     */
    public void visualizeWithGraphStream(List<String> pathToHighlight) {
        System.setProperty("org.graphstream.ui", "swing");
        Graph gsGraph = new SingleGraph("Railway Network");

        gsGraph.setAttribute("ui.stylesheet",
                "graph { padding: 31px; fill-color: #f8f8ff; }" +
                        // Base style for nodes, now as a class
                        "node.base { size: 18px, 20px; fill-color: #DB6F0F; stroke-mode: plain; stroke-color: #222; shape: rounded-box; text-alignment: under; text-size: 13px; text-style: bold; text-offset: 0, 18; text-color: #222; }" +
                        // Base edge styles
                        "edge { size: 3px; fill-color: #C0C0C0; text-size: 13px; text-style: italic; text-background-mode: plain; text-background-color: #fffbe7; text-offset: 0, 8; }" +
                        "edge.electrified { fill-color: #009ac1; size: 3px; }" +
                        // Highlighted edge style
                        "edge.highlighted { fill-color: #4cc37c" +
                        "; size: 4px; }" +
                        // Complete style definition for highlighted nodes
                        "node.highlighted { size: 18px, 21px; fill-color: #cc4e38; stroke-mode: plain; shape: rounded-box; text-alignment: under; text-size: 13px; text-style: bold; text-offset: 0, 18; text-color: #222; }"
        );

        for (Map.Entry<Integer, RailwayDataLoader.Station> entry : stations.entrySet()) {
            RailwayDataLoader.Station s = entry.getValue();
            Node node = gsGraph.addNode(String.valueOf(s.id));
            node.setAttribute("ui.label", s.name + " (" + s.type + ")");
            node.setAttribute("ui.class", "base");
        }

        Map<String, RailwayDataLoader.Line> edgeMap = new HashMap<>();
        for (Map.Entry<Integer, List<RailwayDataLoader.Line>> entry : graph.entrySet()) {
            for (RailwayDataLoader.Line l : entry.getValue()) {
                String edgeId = Math.min(l.from, l.to) + "-" + Math.max(l.from, l.to);
                RailwayDataLoader.Line existing = edgeMap.get(edgeId);
                if (existing == null || (!existing.electrified && l.electrified)) {
                    edgeMap.put(edgeId, l);
                }
            }
        }

        Set<String> highlightedEdgeIds = new HashSet<>();
        for (int i = 0; i < pathToHighlight.size() - 1; i++) {
            int fromId = nameToId.get(pathToHighlight.get(i));
            int toId = nameToId.get(pathToHighlight.get(i + 1));
            highlightedEdgeIds.add(Math.min(fromId, toId) + "-" + Math.max(fromId, toId));
        }

        for (Map.Entry<String, RailwayDataLoader.Line> entry : edgeMap.entrySet()) {
            String edgeId = entry.getKey();
            RailwayDataLoader.Line l = entry.getValue();
            Edge edge = gsGraph.addEdge(edgeId, String.valueOf(l.from), String.valueOf(l.to));
            edge.setAttribute("ui.label", l.length + "km");

            if (highlightedEdgeIds.contains(edgeId)) {
                edge.setAttribute("ui.class", "highlighted");
            } else if (l.electrified) {
                edge.setAttribute("ui.class", "electrified");
            }
        }

        for (String stationName : pathToHighlight) {
            if (nameToId.containsKey(stationName)) {
                Node node = gsGraph.getNode(String.valueOf(nameToId.get(stationName)));
                if (node != null) {
                    node.setAttribute("ui.class", "highlighted");
                }
            }
        }

        gsGraph.display();
        gsGraph.addAttribute("ui.quality");
        gsGraph.addAttribute("ui.antialias");
        gsGraph.addAttribute("layout.force", 1.0);
        gsGraph.addAttribute("layout.quality", 5);
    }


    /**
     * Provides a reason for unreachability between two stations for a given train type.
     * This diagnostic method determines whether stations are unreachable due to topological
     * disconnection or train type constraints, providing appropriate feedback.
     *
     * @param from Name of the origin station (e.g., "S_Lisboa").
     * @param to Name of the destination station (e.g., "S_Porto").
     * @param trainType Type of train.
     * @return String describing the reason for unreachability.
     */
    public String getReasonForUnreachability(String from, String to, String trainType) {
        Integer fromId = nameToId.get(from);
        Integer toId = nameToId.get(to);

        if (fromId == null) return "Source station does not exist.";
        if (toId == null) return "Destination station does not exist.";

        Set<Integer> visited = new HashSet<>();
        boolean reachableIgnoringTrainType = dfsIgnoreTrainType(fromId, toId, visited);
        if (!reachableIgnoringTrainType) {
            return "There is no topological connection between the stations.";
        }

        return "There is no path accessible with train type \"" + trainType + "\".";
    }


    /**
     * Depth-first search ignoring train type constraints.
     * This helper method implements a recursive DFS algorithm that ignores electrification
     * constraints to determine pure topological connectivity between stations.
     *
     * @param current Current station ID.
     * @param target Target station ID.
     * @param visited Set of visited station IDs.
     * @return true if target is reachable, false otherwise.
     */
    private boolean dfsIgnoreTrainType(int current, int target, Set<Integer> visited) {
        if (current == target) return true;
        visited.add(current);

        for (RailwayDataLoader.Line l : graph.get(current)) {
            if (!visited.contains(l.to)) {
                if (dfsIgnoreTrainType(l.to, target, visited)) return true;
            }
        }
        return false;
    }

    /**
     * Checks if the network is fully connected using the transitive closure (Warshall's algorithm).
     * This method determines whether all stations in the network are mutually reachable from
     * each other by implementing Warshall's algorithm to compute the transitive closure.
     *
     * @return true if all stations are mutually reachable, false otherwise.
     */
    public boolean isGraphConnectedUsingTransitiveClosure() {
        int n = stations.size();
        if (n == 0) return true;

        Map<Integer, Integer> idToIndex = new HashMap<>();
        Map<Integer, Integer> indexToId = new HashMap<>();
        int index = 0;
        for (int id : stations.keySet()) {
            idToIndex.put(id, index);
            indexToId.put(index, id);
            index++;
        }

        boolean[][] reach = new boolean[n][n];

        // Fill in the adjacency matrix
        for (int fromId : graph.keySet()) {
            for (RailwayDataLoader.Line l : graph.get(fromId)) {
                int i = idToIndex.get(fromId);
                int j = idToIndex.get(l.to);
                reach[i][j] = true;
            }
        }

        // Warshall's Algorithm - with transitive closure
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    reach[i][j] = reach[i][j] || (reach[i][k] && reach[k][j]);
                }
            }
        }

        // Check if all pairs (except the diagonal) are reachable
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && !reach[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Prints the transitive closure matrix of the network.
     * Computes and displays a matrix showing the reachability between all station pairs,
     * which helps in analyzing network connectivity. The matrix is displayed with station
     * names as row and column headers for better readability.
     *
     * @return true if the network is fully connected, false otherwise.
     */
    public boolean printTransitiveClosureMatrix() {
        int n = stations.size();
        if (n == 0) {
            System.out.println("No stations loaded.");
            return true;
        }

        Map<Integer, Integer> idToIndex = new HashMap<>();
        Map<Integer, Integer> indexToId = new HashMap<>();
        int index = 0;
        for (int id : stations.keySet()) {
            idToIndex.put(id, index);
            indexToId.put(index, id);
            index++;
        }
        boolean[][] reach = new boolean[n][n];

        for (int fromId : graph.keySet()) {
            for (RailwayDataLoader.Line l : graph.get(fromId)) {
                int i = idToIndex.get(fromId);
                int j = idToIndex.get(l.to);
                reach[i][j] = true;
            }
        }

        // Warshall's Algorithm
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    reach[i][j] = reach[i][j] || (reach[i][k] && reach[k][j]);
                }
            }
        }

        System.out.println("\n--- Transitive Closure Matrix ---");

        // Get names in index order
        List<String> names = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            names.add(idToName.getOrDefault(indexToId.get(i), "ID_" + indexToId.get(i)));
        }

        System.out.printf("%-20s", "");
        for (String name : names) {
            System.out.printf("%-20s", name);
        }
        System.out.println();

        for (int i = 0; i < n; i++) {
            System.out.printf("%-20s", names.get(i));
            for (int j = 0; j < n; j++) {
                System.out.printf("%-20s", reach[i][j] ? "1" : "0");
            }
            System.out.println();
        }

        // Connectivity check
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && !reach[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Checks reachability between all pairs of a set of stations for a given train type.
     * This method tests if a specified set of stations forms a fully connected subgraph
     * for the given train type, reporting any connectivity failures.
     *
     * @param stations Set of station names (e.g., "S_Lisboa").
     * @param trainType Type of train.
     * @return true if all pairs are mutually reachable, false otherwise.
     */
    public boolean checkReachabilityBetween(Set<String> stations, String trainType) {
        List<String> stationList = new ArrayList<>(stations);
        for (int i = 0; i < stationList.size(); i++) {
            for (int j = 0; j < stationList.size(); j++) {
                if (i != j && !isReachable(stationList.get(i), stationList.get(j), trainType)) {
                    System.out.printf("❌ Failure: %s -> %s is not reachable with train type \"%s\".%n",
                            stationList.get(i), stationList.get(j), trainType);
                    return false;
                }
            }
        }
        return true;
    }
}

