package pt.ipp.isep.dei.domain.MDISC;

import java.io.*;
import java.util.*;

public class RailwayDataLoader {

    private final Map<String, Integer> nameToId = new HashMap<>();
    private final Map<Integer, String> idToName = new HashMap<>();
    private final Map<Integer, Station> stations = new HashMap<>();
    private final Map<Integer, List<Line>> graph = new HashMap<>();

    private int nextId = 1;


    /**
     * Represents a station with ID, name, and type.
     */
    public static class Station {
        public final int id;
        public final String name;
        public final String type;

        /**
         * Creates a Station.
         * @param id Unique station ID.
         * @param name Station name.
         * @param type Station type ("Depot", "Station", "Terminal" or "Unknown").
         */
        public Station(int id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }

    /**
     * Represents a railway line (edge) between two stations.
     */
    public static class Line {
        int from, to, length;
        boolean electrified;

        /**
         * Creates a railway line.
         * @param from Origin station ID.
         * @param to Destination station ID.
         * @param length Line length in km.
         * @param electrified True if the line is electrified.
         */
        public Line(int from, int to, int length, boolean electrified) {
            this.from = from;
            this.to = to;
            this.length = length;
            this.electrified = electrified;
        }
    }


    /**h
     * Loads stations from a CSV file.
     * Each station receives a unique ID and type based on the name prefix.
     * @param filename Path to the stations CSV file.
     * @throws IOException If the file cannot be read.
     */
    public void loadStations(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] names = line.split(";");
                for (String name : names) {
                    name = name.trim();
                    if (!nameToId.containsKey(name)) {
                        String typePrefix = name.substring(0, 1);
                        String type = switch (typePrefix) {
                            case "D" -> "Depot";
                            case "S" -> "Station";
                            case "T" -> "Terminal";
                            default -> "Unknown";
                        };
                        int id = nextId++;
                        Station s = new Station(id, name, type);
                        stations.put(id, s);
                        nameToId.put(name, id);
                        idToName.put(id, name);
                        graph.putIfAbsent(id, new ArrayList<>());
                    }
                }
            }
        }
    }


    /**
     * Loads railway lines from a CSV file and builds the undirected graph.
     * Each line is added bidirectionally.
     * @param filename Path to the lines CSV file.
     * @throws IOException If the file cannot be read.
     */
    public void loadLines(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length != 4) continue;

                String fromName = parts[0].trim();
                String toName = parts[1].trim();
                boolean electrified = parts[2].trim().equals("1");
                int distance = Integer.parseInt(parts[3].trim());

                int fromId = getOrCreateStation(fromName);
                int toId = getOrCreateStation(toName);

                Line l = new Line(fromId, toId, distance, electrified);
                graph.get(fromId).add(l);
                graph.get(toId).add(new Line(toId, fromId, distance, electrified));
            }
        }
    }


    /**
     * Gets or creates the ID of a station based on its name.
     * @param name Station name.
     * @return The station ID.
     */
    private int getOrCreateStation(String name) {
        if (nameToId.containsKey(name)) {
            return nameToId.get(name);
        }

        String typePrefix = name.substring(0, 1);
        String type = switch (typePrefix) {
            case "D" -> "Depot";
            case "S" -> "Station";
            case "T" -> "Terminal";
            default -> "Unknown";
        };

        int id = nextId++;
        Station s = new Station(id, name, type);
        stations.put(id, s);
        nameToId.put(name, id);
        idToName.put(id, name);
        graph.putIfAbsent(id, new ArrayList<>());
        return id;
    }


    /**
     * Prints all loaded railway lines to the console.
     * Each line includes the stations, length, and electrification status.
     */
    public void printAllLines() {
        Set<String> shown = new HashSet<>();
        for (Map.Entry<Integer, List<RailwayDataLoader.Line>> entry : graph.entrySet()) {
            int fromId = entry.getKey();
            for (RailwayDataLoader.Line l : entry.getValue()) {
                String key = Math.min(fromId, l.to) + "-" + Math.max(fromId, l.to);
                if (shown.contains(key)) continue;
                shown.add(key);

                String fromName = getNameById(l.from);
                String toName = getNameById(l.to);
                String type = l.electrified ? "Electrified" : "Not electrified";
                System.out.printf("- %s -> %s | %dkm | %s%n", fromName, toName, l.length, type);
            }
        }
    }


    // Getters
    public Map<String, Integer> getNameToId() { return nameToId; }
    public Map<Integer, String> getIdToName() { return idToName; }
    public Map<Integer, Station> getStations() { return stations; }
    public Map<Integer, List<Line>> getGraph() { return graph; }

    /**
     * Returns a map of all loaded stations and their types.
     * @return Map from station name to type.
     */
    public Map<String, String> getAllStations() {
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : nameToId.entrySet()) {
            RailwayDataLoader.Station s = stations.get(entry.getValue());
            result.put(entry.getKey(), s.type);
        }
        return result;
    }

    /**
     * Returns the name of a station given its ID.
     * @param id Station ID.
     * @return Station name, or "ID_x" if not found.
     */
    private String getNameById(int id) {
        return idToName.getOrDefault(id, "ID_" + id);
    }
}

