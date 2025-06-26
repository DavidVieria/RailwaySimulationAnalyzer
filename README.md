# Railway Simulation Analyzer

<br>

```
⚠️ This repository was created as part of the Curricular Unit of "Matemática Discreta (MDISC)" in the 2nd semester of the Bachelor’s Degree in Informatics Engineering at ISEP, and was therefore developed as a group project.
```

Railway Simulation Analyzer is a Java application developed to simulate railway network scenarios and provide detailed analysis tools. The project was created to support university teaching of algorithms and data structures, enabling the practical application of theoretical concepts in real-world contexts.

## Features
- Simulation of multiple railway scenarios from customizable input files (CSV).
- Generation and visualization of graphs (DOT format).
- Tools for path analysis, connections, and properties of railway graphs.
- Automated test coverage and detailed code documentation.
- Development organized by sprints, with task distribution and algorithm documentation.

## Project Structure

```
RailwaySimulationAnalyzer/
├── docs/                # Project documentation, algorithms, and management
│   ├── TeamMembersAndTasks.md
│   ├── algorithms_documentation/ (US13.md, US14.md, ...)
│   ├── management/ (sprint spreadsheets)
│   └── us26/, us28/ (algorithm analyses)
├── inputfiles/          # CSV files for railway scenarios
├── outputfiles/         # Generated outputs (e.g., grafo.dot)
├── src/                 # Main source code and tests
│   ├── main/java/pt/    # Java implementation
│   └── test/java/pt/    # Automated tests
├── target/              # Build outputs, reports, and generated documentation
├── pom.xml              # Maven configuration
└── README.md            # This file
```

## Documentation and Organization
- [Task Distribution and Team](docs/TeamMembersAndTasks.md)
- [Algorithm Documentation](docs/algorithms_documentation/)
- [Sprint Management](docs/management/)
- [Algorithm Analyse - US26](docs/us26/)
- [Algorithm Analyse - US28](docs/us28/)

## How to Run the Project

### Prerequisites
- Java 11 or higher (preferably Java 23)
- Maven 3.6+

### Running Unit Tests
```sh
mvn clean test
```

### Generating Javadoc for Source Code
```sh
mvn javadoc:javadoc
```

### Generating Javadoc for Tests
```sh
mvn javadoc:test-javadoc
```

### Generating Jacoco Coverage Report
```sh
mvn test jacoco:report
```

### Checking Coverage Limits
```sh
mvn test jacoco:check
```

### Generating the Jar Package
```sh
mvn package
```

### Running the Generated Jar
```sh
java -jar target/RailwaySimulationAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---
