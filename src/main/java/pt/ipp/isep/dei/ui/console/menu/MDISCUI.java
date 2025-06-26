package pt.ipp.isep.dei.ui.console.menu;

import pt.ipp.isep.dei.controller.MDISCController;
import pt.ipp.isep.dei.domain.MDISC.US27_Dijkstra.PathResult;
import pt.ipp.isep.dei.ui.console.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Console UI for MDISC related graph analysis tools.
 * Integrates US13 (Reachability) and US14 (Maintenance Route) functionalities,
 * along with other graph inspection options.
 */
public class MDISCUI implements Runnable {

    private MDISCController controller = new MDISCController();
    private static final String INPUT_DIR_PATH = "./inputfiles/";
    private static boolean isGraphWindowOpen = false;

    /**
     * Inner class to hold pairs of scenario files (lines and stations).
     */
    private static class ScenarioFiles {
        String scenarioName;
        File linesFile;
        File stationsFile;

        ScenarioFiles(String name, File lines, File stations) {
            this.scenarioName = name;
            this.linesFile = lines;
            this.stationsFile = stations;
        }

        @Override
        public String toString() {
            return String.format("%s (Lines: %s, Stations: %s)",
                    scenarioName, linesFile.getName(), stationsFile.getName());
        }
    }


    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        if (!loadNetworkData(scanner)) {
            System.out.println("\nReturning to Admin menu...");
            return;
        }

        int option;
        do {
            displayMenu();
            option = getUserOption(scanner);
            handleOption(option, scanner);
        } while (option != 0);
    }

    private boolean loadNetworkData(Scanner scanner) {
        File inputDir = new File(INPUT_DIR_PATH);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            System.err.println("Critical Error: Input directory not found or invalid: " + INPUT_DIR_PATH);
            return false;
        }

        Map<String, ScenarioFiles> scenarioMap = listValidScenarios(inputDir);
        if (scenarioMap.isEmpty()) {
            System.out.println("No valid scenarios found. Please add a new one manually.");
        }

        int userChoice = promptScenarioSelection(scanner, scenarioMap);

        File stationsFile;
        File linesFile;
        String scenarioName;

        if (userChoice == 0) {
            return false;
        } else if (userChoice == scenarioMap.size() + 1) {
            ScenarioFiles manual = promptManualFileInput(scanner);
            stationsFile = manual.stationsFile;
            linesFile = manual.linesFile;
            scenarioName = manual.scenarioName;
        } else {
            ScenarioFiles selected = new ArrayList<>(scenarioMap.values()).get(userChoice - 1);
            stationsFile = selected.stationsFile;
            linesFile = selected.linesFile;
            scenarioName = selected.scenarioName;
        }

        System.out.printf("Loading scenario '%s' from:%n - %s%n - %s%n",
                scenarioName, stationsFile.getPath(), linesFile.getPath());

        try {
            controller.loadStations(stationsFile.getPath());
            controller.loadLines(linesFile.getPath());
            System.out.println("Network data successfully loaded.");
            return true;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Critical Error: Failed to load or parse scenario: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected Critical Error while loading scenario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private Map<String, ScenarioFiles> listValidScenarios(File inputDir) {
        Map<String, File> linesFiles = new HashMap<>();
        Map<String, File> stationsFiles = new HashMap<>();
        for (File file : Objects.requireNonNull(inputDir.listFiles())) {
            String name = file.getName();
            if (name.endsWith("_lines.csv")) {
                linesFiles.put(name.replace("_lines.csv", ""), file);
            } else if (name.endsWith("_stations.csv")) {
                stationsFiles.put(name.replace("_stations.csv", ""), file);
            }
        }

        Map<String, ScenarioFiles> validScenarios = new TreeMap<>();
        for (String prefix : linesFiles.keySet()) {
            if (stationsFiles.containsKey(prefix)) {
                validScenarios.put(prefix, new ScenarioFiles(prefix, linesFiles.get(prefix), stationsFiles.get(prefix)));
            }
        }
        return validScenarios;
    }

    private int promptScenarioSelection(Scanner scanner, Map<String, ScenarioFiles> scenarios) {
        List<ScenarioFiles> scenarioList = new ArrayList<>(scenarios.values());

        System.out.println("\n--- Choose Scenario to Load ------");
        for (int i = 0; i < scenarioList.size(); i++) {
            System.out.printf(" %d. %s%n", i + 1, scenarioList.get(i).scenarioName);
        }
        int manualOption = scenarioList.size() + 1;
        System.out.printf(" %d. add a new scenario%n", manualOption);

        int choice = -1;
        while (choice < 0 || choice > manualOption) {
            System.out.printf("Choose an option (1-%d) or 0 to exit: ", manualOption);
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice < 0 || choice > manualOption) {
                    System.out.println("  Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a number.");
            }
        }
        return choice;
    }

    private ScenarioFiles promptManualFileInput(Scanner scanner) {
        System.out.println("\n--- Manual Scenario Upload ---");
        System.out.println("Please provide the files for the new scenario.\n");

        System.out.println("Choose an input method:");
        System.out.println("  1. File name (must be stored inside the input folder 'inputfiles')");
        System.out.println("  2. Full file path");

        int mode;

        while (true) {
            System.out.print("Option (1 or 2): ");
            String input = scanner.nextLine().trim();
            if (input.equals("1") || input.equals("2")) {
                mode = Integer.parseInt(input);
                break;
            }
            System.out.println("Invalid option. Please enter 1 or 2.");
        }

        File stationsFile, linesFile;

        while (true) {
            System.out.print("\nEnter stations file name/path: ");
            String stationsInput = scanner.nextLine().trim();
            stationsFile = (mode == 1) ? new File(INPUT_DIR_PATH, stationsInput) : new File(stationsInput);

            if (!stationsFile.exists() || !stationsFile.isFile()) {
                System.out.println("  Error: File not found. Try again.");
                continue;
            }
            if (!stationsFile.getName().endsWith(".csv")) {
                System.out.println("  Error: File must end with '.csv'.");
                continue;
            }
            break;
        }

        while (true) {
            System.out.print("Enter lines file name/path: ");
            String linesInput = scanner.nextLine().trim();
            linesFile = (mode == 1) ? new File(INPUT_DIR_PATH, linesInput) : new File(linesInput);

            if (!linesFile.exists() || !linesFile.isFile()) {
                System.out.println("  Error: File not found. Try again.");
                continue;
            }
            if (!linesFile.getName().endsWith(".csv")) {
                System.out.println("  Error: File must end with '.csv'.");
                continue;
            }
            break;
        }

        String prefix = stationsFile.getName().replace("_stations.csv", "");
        if (!linesFile.getName().startsWith(prefix)) {
            System.out.println("Warning: Files may not belong to the same scenario.");
        }

        return new ScenarioFiles(prefix, linesFile, stationsFile);
    }



    private void displayMenu() {
        System.out.println("\n\n ==== MDISC MENU ====");
        System.out.println(" 1. List loaded stations");
        System.out.println(" 2. List loaded lines");
        System.out.println(" 3. Check connection between stations (US13)");
        System.out.println(" 4. Automatic connectivity checks (US13)");
        System.out.println(" 5. Print Transitive Closure Matrix (US13)");
        System.out.println(" 6. Find maintenance route (US14)");
        System.out.println(" 7. Find Shortest Route between two Stations (US27)");
        System.out.println(" 8. Visualize Complete Network (GraphStream)");
        System.out.println(" 9. Check Network Connectivity (Warshall)");
        System.out.println(" 0. Back");
        System.out.print(" Choose an option: ");
    }

    private int getUserOption(Scanner scanner) {
        try {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) {
                return -1;
            }
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid option. Please enter a number.");
            return -1;
        }
    }

    private void handleOption(int option, Scanner scanner) {
        switch (option) {
            case 1: handleListStations(); break;
            case 2: handleListLines(); break;
            case 3: handleUS13Reachability(scanner); break;
            case 4: handleBatchConnectivityChecks(); break;
            case 5: handlePrintTransitiveClosure(); break;
            case 6: handleUS14MaintenanceRoute(scanner); break;
            case 7: handleUS27ShortestRoute(); break;
            case 8: handleVisualizeGraph(); break;
            case 9: handleCheckConnectivityWarshall(); break;
            case 0:
                System.out.println("Returning to the previous menu...");
                controller = new MDISCController();
                isGraphWindowOpen = false;
                break;
            case -1:
                break;
            default: System.out.println("Invalid option.");
        }
    }

    private void handleListStations() {
        System.out.println("\n--- Loaded Stations ---");
        Map<String, String> stations = controller.getAllStations();
        if (stations == null || stations.isEmpty()) {
            System.out.println("No stations loaded or failed to retrieve station data.");
        } else {
            stations.forEach((name, type) ->
                    System.out.printf("- %s (%s)%n", name, type)
            );
        }
    }

    private void handleListLines() {
        System.out.println("\n--- Loaded Railway Lines ---");
        try {
            controller.printAllLines();
        } catch (Exception e) {
            System.err.println("Error while printing lines: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUS13Reachability(Scanner scanner) {
        System.out.println("\n--- US13: Check Reachability Between Stations ---");

        String stationType = readMandatoryInput(scanner, "Station type (Depot/Station/Terminal): ");
        String origin = readMandatoryInput(scanner, "Origin station (e.g., S_Ardgay): ");
        String destination = readMandatoryInput(scanner, "Destination station (e.g., S_Wick): ");
        String trainType = readMandatoryInput(scanner, "Train type (steam/diesel/electric): ").toLowerCase();

        boolean originValid = controller.isStationOfType(origin, stationType);
        boolean destinationValid = controller.isStationOfType(destination, stationType);

        if (!originValid || !destinationValid) {
            printStationTypeError(origin, destination, stationType, originValid, destinationValid);
        } else {
            boolean reachable = controller.isReachable(origin, destination, trainType);
            if (reachable) {
                System.out.println("✅ Result: Connection possible.");
                if (!isGraphWindowOpen) {
                    isGraphWindowOpen = true;
                    handleVisualizeGraph();
                }
            } else {
                String reason = controller.getReasonForUnreachability(origin, destination, trainType);
                System.out.println("❌ Result: Connection not possible.");
                System.out.println("   Reason: " + reason);
            }
        }
    }

    private void handleBatchConnectivityChecks() {
        System.out.println("\n--- Automatic Connectivity Checks (US13) ---");
        Map<String, String> stationMap = controller.getAllStations();
        if (stationMap == null || stationMap.isEmpty()) {
            System.out.println("No stations loaded to check.");
            return;
        }
        Set<String> allStations = stationMap.keySet();

        System.out.println("\n▶ Check 1: Diesel train between all stations...");
        try {
            boolean diesel = controller.checkReachabilityBetween(allStations, "diesel");
            System.out.println(diesel ? "  ✅ All stations are reachable using a diesel train."
                    : "  ❌ Some stations are not reachable using a diesel train.");
        } catch (Exception e) {
            System.err.println("  Error during diesel check: " + e.getMessage());
        }

        System.out.println("\n▶ Check 2: Electric train between Stations and Terminals...");
        Set<String> stationOrTerminal = stationMap.entrySet().stream()
                .filter(e -> "Station".equalsIgnoreCase(e.getValue()) || "Terminal".equalsIgnoreCase(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (stationOrTerminal.isEmpty()) {
            System.out.println("  ℹ️ No stations of type 'Station' or 'Terminal' found.");
        } else {
            try {
                boolean elecST = controller.checkReachabilityBetween(stationOrTerminal, "electric");
                System.out.println(elecST ? "  ✅ Stations and Terminals are fully connected via electric train."
                        : "  ❌ Not all Stations and Terminals are connected via electric train.");
            } catch (Exception e) {
                System.err.println("  Error during electric check (Station/Terminal): " + e.getMessage());
            }
        }

        System.out.println("\n▶ Check 3: Electric train between Terminals only...");
        Set<String> onlyTerminals = stationMap.entrySet().stream()
                .filter(e -> "Terminal".equalsIgnoreCase(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (onlyTerminals.isEmpty()) {
            System.out.println("  ℹ️ No stations of type 'Terminal' found.");
        } else {
            try {
                boolean elecT = controller.checkReachabilityBetween(onlyTerminals, "electric");
                System.out.println(elecT ? "  ✅ All Terminals are connected via electric train."
                        : "  ❌ Not all Terminals are connected via electric train.");
            } catch (Exception e) {
                System.err.println("  Error during electric check (Terminals): " + e.getMessage());
            }
        }
    }

    private void handlePrintTransitiveClosure() {
        System.out.println("\n--- Transitive Closure Matrix ---");
        try {
            boolean isConnected = controller.printTransitiveClosureMatrix();
            if (isConnected) {
                System.out.println("\n✅ The matrix confirms the graph is fully connected.");
            } else {
                System.out.println("\n❌ The matrix shows the graph is NOT fully connected.");
            }
        } catch (Exception e) {
            System.err.println("Error while printing the transitive closure matrix: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUS14MaintenanceRoute(Scanner scanner) {
        System.out.println("\n--- US14: Find Maintenance Route ---");

        boolean onlyElectrified = askYesNo(scanner, "▶ Do you want to consider only electrified lines for the maintenance route? (y/n) ");

        List<String> possibleStarts = controller.getPotentialStartStations(onlyElectrified);

        if (possibleStarts == null || possibleStarts.isEmpty()) {
            handleVisualizeGraph();
            return;
        }

        System.out.println("\n▶ Possible starting stations for the route:");
        for (int i = 0; i < possibleStarts.size(); i++) {
            System.out.printf(" %d. %s%n", i + 1, formatStationName(possibleStarts.get(i)));
        }

        int choice = -1;
        while (choice < 1 || choice > possibleStarts.size()) {
            System.out.printf(" Choose the starting station (1-%d): ", possibleStarts.size());
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice < 1 || choice > possibleStarts.size()) {
                    System.out.println("   Error: Invalid choice.");
                }
            } catch (NumberFormatException e) {
                System.out.println("   Error: Invalid input. Please enter a number.");
                choice = -1;
            }
        }
        String chosenStartStation = possibleStarts.get(choice - 1);
        System.out.println("\n Chosen: " + formatStationName(chosenStartStation));

        List<String> route = controller.findMaintenanceRoute(chosenStartStation);

        if (route != null && !route.isEmpty()) {
            System.out.println("\n✅ Maintenance route found:");

            StringBuilder formattedRoute = new StringBuilder();
            for (int i = 0; i < route.size(); i++) {
                formattedRoute.append(formatStationName(route.get(i)));

                if (i < route.size() - 1) {
                    formattedRoute.append(" -> ");
                }
            }

            System.out.println("   🚉  " + formattedRoute.toString());
        } else {
            System.out.println("\n❌ Unable to find a complete maintenance route starting from " + formatStationName(chosenStartStation) + ".");
            System.out.println("   (Check previous error/warning messages.)");
        }

        if (!isGraphWindowOpen) {
            isGraphWindowOpen = true;
            handleVisualizeGraph();
        }
    }

    // US27
    private void handleUS27ShortestRoute() {
        System.out.println("\n--- US27: Find Shortest Route through an Ordered List of Stations ---");

        Scanner scanner = new Scanner(System.in);
        System.out.println("How do you want to provide the list of stations the train must pass through?");
        System.out.println("  1. Enter stations manually");
        System.out.println("  2. Read stations from CSV file");
        int mode = -1;
        while (mode != 1 && mode != 2) {
            System.out.print("Option (1 or 2): ");
            try {
                mode = Integer.parseInt(scanner.nextLine().trim());
                if (mode != 1 && mode != 2) {
                    System.out.println("Invalid option. Please enter 1 or 2.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter 1 or 2.");
            }
        }

        List<String> selectedStations = new ArrayList<>();
        if (mode == 1) {
            // Manual mode (original logic)
            List<String> allStations = new ArrayList<>(controller.getAllStations().keySet());
            if (allStations.isEmpty()) {
                System.out.println("No stations loaded. Cannot find a route.");
                return;
            }
            Collections.sort(allStations); // Sort for better usability
            int choice;
            do {
                System.out.println("\n--- Build Your Route ---");
                if (selectedStations.isEmpty()) {
                    System.out.println("Current route: [empty]");
                } else {
                    System.out.println("Current route: " + String.join(" -> ", selectedStations));
                }
                System.out.println("\nSelect the next station to add (select 0 when done):");
                choice = Utils.showAndSelectIndex(allStations, "Available Stations:");
                if (choice >= 0 && choice < allStations.size()) {
                    String selected = allStations.get(choice);
                    selectedStations.add(selected);
                    System.out.printf("Added '%s' to the route.%n", selected);
                } else if (choice != -1) { // -1 is the cancel/done option
                    System.out.println("Invalid selection.");
                }
            } while (choice != -1);
        } else {
            // CSV mode
            System.out.println("\nChoose an input method:");
            System.out.println("  1. File name (must be stored inside the input folder 'inputfiles')");
            System.out.println("  2. Full file path");
            int fileMode = -1;
            while (fileMode != 1 && fileMode != 2) {
                System.out.print("Option (1 or 2): ");
                try {
                    fileMode = Integer.parseInt(scanner.nextLine().trim());
                    if (fileMode != 1 && fileMode != 2) {
                        System.out.println("Invalid option. Please enter 1 or 2.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter 1 or 2.");
                }
            }
            String fileName;
            if (fileMode == 1) {
                System.out.print("Enter the CSV file name (must be stored inside the input folder 'inputfiles'): ");
                fileName = scanner.nextLine().trim();
                fileName = INPUT_DIR_PATH + fileName;
            } else {
                System.out.print("Enter the full path to the CSV file: ");
                fileName = scanner.nextLine().trim();
            }
            File file = new File(fileName);
            if (!file.exists() || !file.isFile()) {
                System.out.println("File not found: " + file.getPath());
                return;
            }
            try (Scanner fileScanner = new Scanner(file)) {
                if (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine();
                    String[] stations = line.split(";");
                    for (String s : stations) {
                        String station = s.trim();
                        if (!station.isEmpty()) {
                            selectedStations.add(station);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading file: " + e.getMessage());
                return;
            }
            if (selectedStations.isEmpty()) {
                System.out.println("No stations found in the file.");
                return;
            }
            // Validate stations
            Set<String> allStations = controller.getAllStations().keySet();
            List<String> invalid = selectedStations.stream().filter(s -> !allStations.contains(s)).collect(Collectors.toList());
            if (!invalid.isEmpty()) {
                System.out.println("The following stations do not exist in the network: " + String.join(", ", invalid));
                return;
            }
            System.out.println("Stations loaded from file: " + String.join(" -> ", selectedStations));
        }

        // Check if a valid route can be formed
        if (selectedStations.size() < 2) {
            System.out.println("\nRoute requires at least two stations. Operation cancelled.");
            return;
        }

        System.out.println("\nCalculating shortest path for route: " + String.join(" -> ", selectedStations));
        // Call the controller with the validated list of stations
        PathResult result = controller.findShortestRouteThroughOrderedStations(selectedStations);

        if (result.isPossible) {
            System.out.println("\n✅ Shortest Route Found!");
            System.out.printf("   Total Distance: %.2f km%n", result.distance);
            System.out.println("   Path: " + String.join(" -> ", result.path));

            // AC01: Visualization would be triggered here
            handleVisualizeGraph(result.path);

        } else {
            // This error will now only appear for actual connectivity issues
            System.out.println("\n❌ Could not find the requested route. Please check network connectivity between the selected stations.");
        }
    }

    private String formatStationName(String name) {
        String prefix = name.substring(0, 1);
        String type = switch (prefix) {
            case "S" -> "Station";
            case "D" -> "Depot";
            case "T" -> "Terminal";
            default -> "Unknown";
        };

        return name + " (" + type + ")";
    }

    private void handleVisualizeGraph() {
        System.out.println("\n--- Visualize Network (GraphStream) ---");
        try {
            Map<String, String> stations = controller.getAllStations();

            if (stations == null || stations.isEmpty()) {
                System.out.println("ℹ️ No stations loaded to visualize.");
                return;
            }
            System.out.println("Attempting to open visualization window...");
            controller.visualizeWithGraphStream();
            System.out.println("Visualization window opened (it may be in the background).");
        } catch (Exception e) {
            System.err.println("Error visualizing the graph with GraphStream: " + e.getMessage());
            System.err.println("Check if the GraphStream library is properly configured and accessible.");
        }
    }

    // Highlighted path version for US27/AC01
    private void handleVisualizeGraph(List<String> pathToHighlight) {
        System.out.println("\n--- Visualize Network (GraphStream) ---");
        try {
            Map<String, String> stations = controller.getAllStations();

            if (stations == null || stations.isEmpty()) {
                System.out.println("ℹ️ No stations loaded to visualize.");
                return;
            }
            System.out.println("Attempting to open visualization window...");
            controller.visualizeWithGraphStream(pathToHighlight);
            System.out.println("Visualization window opened (it may be in the background).");
        } catch (Exception e) {
            System.err.println("Error visualizing the graph with GraphStream: " + e.getMessage());
            System.err.println("Check if the GraphStream library is properly configured and accessible.");
        }
    }

    private void handleCheckConnectivityWarshall() {
        System.out.println("\n--- Check Network Connectivity (Warshall) ---");
        try {
            boolean isConnected = controller.isGraphConnectedUsingTransitiveClosure();
            if (isConnected) {
                System.out.println("✅ The railway network is connected (verified by transitive closure).");
            } else {
                System.out.println("❌ The railway network is NOT connected (verified by transitive closure).");
            }
        } catch (Exception e) {
            System.err.println("Error checking connectivity with Warshall: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printStationTypeError(String origin, String destination, String stationType, boolean originValid, boolean destinationValid) {
        if (!originValid) {
            System.out.printf("   Error: The origin station \"%s\" is not of type \"%s\".%n", origin, stationType);
        }
        if (!destinationValid) {
            System.out.printf("   Error: The destination station \"%s\" is not of type \"%s\".%n", destination, stationType);
        }
    }

    private String readMandatoryInput(Scanner scanner, String prompt) {
        String input;
        do {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("   Error: This field is required. Please try again.");
            }
        } while (input.isEmpty());
        return input;
    }

    private boolean askYesNo(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = scanner.nextLine().trim().toLowerCase();
            if ("y".equals(answer)) {
                return true;
            } else if ("n".equals(answer)) {
                return false;
            } else {
                System.out.println("   Error: Invalid response. Please enter 'y' or 'n'.");
            }
        }
    }
}
