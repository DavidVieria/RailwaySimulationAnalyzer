
# US26 - Algorithm Efficiency Analysis (Worst-Case)

## Explanation of Terms

| Symbol | Meaning                                         |
|--------|-------------------------------------------------|
| n      | Number of vertices (stations) in the graph      |
| m      | Number of edges (connections) in the graph      |
| k      | Number of selected stations (subset)            |

## Algorithms Analyzed from US13

---

### 1. `isReachable()` / `dfs()`

**Description:**  
Checks if there is a path between two stations for a specific train type using a depth-first search.

**Pseudocode:**
```
function DFS(current, target, visited, trainType):
    if current == target:
        return true
    mark current as visited
    for each neighbor in graph[current]:
        if not visited[neighbor] and (trainType != "electric" OR neighbor.electrified):
            if DFS(neighbor, target, visited, trainType):
                return true
    return false
```

**Worst-case Time Complexity:**  
O(n + m)
---

### 2. `isGraphConnectedUsingTransitiveClosure()`

**Description:**  
Uses Warshall's algorithm to compute transitive closure and determine full connectivity.

**Pseudocode:**
```
function warshall(matrix, n):
    for k = 0 to n-1:
        for i = 0 to n-1:
            for j = 0 to n-1:
                matrix[i][j] = matrix[i][j] OR (matrix[i][k] AND matrix[k][j])
```

**Worst-case Time Complexity:**  
O(n³)

---

### 3. `checkReachabilityBetween()`

**Description:**  
Checks if all pairs of a subset of stations are reachable for a given train type.

**Pseudocode:**
```
function checkReachabilityBetween(stations, trainType):
    for i = 0 to k:
        for j = 0 to k:
            if i != j:
                if not isReachable(station[i], station[j], trainType):
                    return false
    return true
```

**Worst-case Time Complexity:**  
O(k² × (n + m))

---

### 4. `getReasonForUnreachability()`

**Description:**  
Provides a diagnostic message explaining why a path between two stations is not reachable for a given train type. It performs two searches: one that checks for topological connectivity (ignoring train type constraints), and one that respects the train type.

**Pseudocode:**
```
function getReason(from, to, trainType):
    if from not in stationIds:
        return "Source station does not exist."
    if to not in stationIds:
        return "Destination station does not exist."
    if not DFS_IgnoreTrainType(from, to):
        return "No topological connection."
    return "No path accessible with train type."
```

**Worst-case Time Complexity:**  
O(n + m)  
(*Performs up to two DFS traversals.*)

---
## Notes

- Graphic visualization routines are excluded from this analysis as per project requirements.
- The pseudocode reflects only logical and primitive operations used in the algorithms.
