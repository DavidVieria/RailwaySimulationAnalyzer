package pt.ipp.isep.dei.controller;

import pt.ipp.isep.dei.domain.MDISC.RailwayDataLoader;
import pt.ipp.isep.dei.domain.MDISC.US13;
import pt.ipp.isep.dei.domain.MDISC.US14;
import pt.ipp.isep.dei.domain.MDISC.US27_Dijkstra;

import java.io.IOException;
import java.util.*;

public class MDISCController {

    private final RailwayDataLoader dataLoader = new RailwayDataLoader();
    private US13 graphTool;
    private US14 maintenancePlanner;
    private US27_Dijkstra dijkstra;


    public void loadStations(String path) throws IOException {
        dataLoader.loadStations(path);
    }

    public void loadLines(String path) throws IOException {
        dataLoader.loadLines(path);

        graphTool = new US13(dataLoader);
        maintenancePlanner = new US14(dataLoader);
        dijkstra = new US27_Dijkstra(dataLoader);

    }

    public Map<String, String> getAllStations() {
        return dataLoader.getAllStations();
    }

    public void printAllLines() {
        dataLoader.printAllLines();
    }

    public boolean isStationOfType(String name, String type) {
        return graphTool.isStationOfType(name, type);
    }

    public boolean isReachable(String from, String to, String trainType) {
        return graphTool.isReachable(from, to, trainType);
    }

    public String getReasonForUnreachability(String from, String to, String trainType) {
        return graphTool.getReasonForUnreachability(from, to, trainType);
    }

    public boolean checkReachabilityBetween(Set<String> stations, String trainType) {
        return graphTool.checkReachabilityBetween(stations, trainType);
    }

    public boolean isGraphConnectedUsingTransitiveClosure() {
        return graphTool.isGraphConnectedUsingTransitiveClosure();
    }

    public boolean printTransitiveClosureMatrix() {
        return graphTool.printTransitiveClosureMatrix();
    }

    public void visualizeWithGraphStream() {
        graphTool.visualizeWithGraphStream();
    }

    public void visualizeWithGraphStream(List<String> pathToHighlight) {
        graphTool.visualizeWithGraphStream(pathToHighlight);
    }

    public List<String> getPotentialStartStations(boolean onlyElectrified) {
        if (maintenancePlanner == null) {
            System.err.println("⚠️ Warning: The data network is not loaded correctly.");
            return Collections.emptyList();
        }
        return maintenancePlanner.getPotentialStartStations(onlyElectrified);
    }

    public List<String> findMaintenanceRoute(String startStation) {
        if (maintenancePlanner == null) {
            System.err.println("⚠️ Warning: The data network is not loaded correctly.");
            return Collections.emptyList();
        }
        return maintenancePlanner.findMaintenanceRoute(startStation);
    }

    /**
     * Calculates the shortest route through an ordered list of station names.
     *
     * @param stationNames The ordered list of stations to visit.
     * @return A PathResult containing the full path and total distance.
     */
    public US27_Dijkstra.PathResult findShortestRouteThroughOrderedStations(List<String> stationNames) {
        if (stationNames == null || stationNames.size() < 2) {
            System.err.println("Error: At least two stations are required to find a route.");
            return new US27_Dijkstra.PathResult();
        }

        List<String> totalPath = new ArrayList<>();
        double totalDistance = 0;
        boolean firstSegment = true;

        for (int i = 0; i < stationNames.size() - 1; i++) {
            String start = stationNames.get(i);
            String end = stationNames.get(i + 1);

            US27_Dijkstra.PathResult segmentResult = dijkstra.findShortestPath(start, end);

            if (!segmentResult.isPossible) {
                System.err.printf("Error: No path found from '%s' to '%s'.%n", start, end);
                return new US27_Dijkstra.PathResult(); // The whole route is impossible
            }

            totalDistance += segmentResult.distance;

            if (firstSegment) {
                totalPath.addAll(segmentResult.path);
                firstSegment = false;
            } else {
                // Add the rest of the path, but skip the first element to avoid duplication
                totalPath.addAll(segmentResult.path.subList(1, segmentResult.path.size()));
            }
        }

        return new US27_Dijkstra.PathResult(totalPath, totalDistance);
    }
}