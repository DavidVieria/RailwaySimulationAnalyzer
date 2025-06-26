# US28 - Algorithm Efficiency Analysis (Worst Case)

## Introduction to Algorithm Complexity Analysis

The complexity of an algorithm refers to the amount of computational resources (time and space) required for its execution, depending on the algorithm itself and the characteristics of its input. The goal of complexity analysis is to predict the performance of an algorithm, allowing the selection of the most efficient solution for a given problem.

In this document, the complexity analysis focuses mainly on execution time (time complexity). Execution time is analyzed in terms of the number of primitive operations (reading an element, assignments, comparisons, arithmetic operations, method calls) required for the execution of the algorithm as a function of the input size. For this analysis, we use Big O notation to perform an asymptotic analysis, which describes the behavior of the algorithm as the input size tends to infinity, disregarding constants and lower-order terms.

## Explanation of Terms

| Symbol | Meaning                                      |
|--------|----------------------------------------------|
| $n$    | Number of vertices (stations) in the graph   |
| $m$    | Number of edges (connections) in the graph   |
| $k$    | Number of stations in the ordered stop list  |

---

## Algorithms Analyzed from US27

### 1. Dijkstra's Algorithm (`US27_Dijkstra.findShortestPath()`)

**Description:**
Implements Dijkstra's algorithm to find the shortest path between two stations. Returns a `PathResult` object containing the path, distance, and a boolean `isPossible` indicating if a path exists.

**Pseudocode (aligned with code):**

```
function Dijkstra(graph, startStationName, endStationName):
    startId = nameToId[startStationName]
    endId = nameToId[endStationName]
    if startId is null or endId is null:
        return PathResult(impossible)
    // Initialization
    distances = empty map // Stores the shortest known distance to each node
    predecessors = empty map // Stores the predecessor node in the shortest path
    for each node_id in graph:
        distances[node_id] = infinity
        predecessors[node_id] = null
    distances[startId] = 0

    priority_queue = priority queue (min-heap)
    priority_queue.add(startId, 0) // Add source with distance 0

    // Processing
    while priority_queue is not empty:
        (u_id, u_distance) = priority_queue.removeMin() // Extract node with smallest distance

        // If the extracted distance is greater than the known one, skip (already processed)
        if u_distance > distances[u_id]:
            continue

        // If the target is reached, we can stop
        if u_id == endId:
            break

        // For each edge (u_id, v_id) from u_id:
        for each edge in graph[u_id]:
            v_id = edge.target_id
            edge_weight = edge.length
            distance_through_u = distances[u_id] + edge_weight

            // Edge relaxation
            if distance_through_u < distances[v_id]:
                distances[v_id] = distance_through_u
                predecessors[v_id] = u_id
                priority_queue.add(v_id, distances[v_id])

    if distances[endId] == infinity:
        return PathResult(impossible)

    // Path reconstruction
    path = empty list
    current_id = endId
    while current_id is not null:
        path.addToStart(id_to_name[current_id])
        current_id = predecessors[current_id]

    return PathResult(path, distances[endId], true)
```

**Worst-case Time Complexity:**
The complexity of Dijkstra's algorithm, when implemented with a priority queue based on a binary heap (as is the case with `java.util.PriorityQueue` in Java), is:
$$O((n + m) \log n)$$
Where:
- $n$ is the number of vertices (stations).
- $m$ is the number of edges (railway connections).
- $\log n$ is due to the insert (`add`) and extract-min (`removeMin`) operations in the priority queue, which take logarithmic time relative to the number of elements in the queue.

#### Detailed line-by-line analysis of the pseudocode

| Line(s) | Code Snippet                                 | Worst-case Complexity                       | Justification                                                                                                                                                                                                                                                                                                                 |
|---------|----------------------------------------------|---------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1-5     | Initialization of distances and predecessors | $O(n)$                                      | A loop iterates over all $n$ nodes in the graph to initialize their distances to infinity and predecessors to null.                                                                                                                                                                                                           |
| 6-7     | `priority_queue.add(sourceId, 0)`            | $O(\log n)$                                 | Adding an element to a priority queue (min-heap) has logarithmic complexity relative to the number of elements in the queue, which in the worst case can be $n$.                                                                                                                                                              |
| 8       | `while priority_queue is not empty:`         | $O((n + m) \log n)$                         | This is the main loop that dominates the algorithm's complexity. It runs while there are nodes to process in the priority queue. The total complexity is the sum of the complexities of the operations inside the loop.                                                                                                       |
| 9       | `priority_queue.removeMin()`                 | $O(\log n)$                                 | Removing (extracting) the element with the smallest priority from a priority queue has logarithmic complexity. This operation is performed at most $n$ times (once for each vertex).                                                                                                                                          |
| 10-12   | `if u_distance > distances[u_id]: continue`  | $O(1)$                                      | Comparison and control flow operations, which are constant.                                                                                                                                                                                                                                                                   |
| 13-15   | `if u_id == endId: break`                    | $O(1)$                                      | Comparison and control flow operations, which are constant.                                                                                                                                                                                                                                                                   |
| 16      | `for each edge in graph[u_id]:`              | $O(\text{deg}(u))$ (local) / $O(m)$ (total) | The inner loop iterates over the neighbors of node `u_id`. The per-iteration complexity of this loop depends on the degree of node `u`. However, the sum of the executions of this loop for all nodes throughout the algorithm's execution is proportional to the total number of edges $m$, since each edge is visited once. |
| 17-19   | Calculation of `distance_through_u`          | $O(1)$                                      | Data access and arithmetic operations, which are constant.                                                                                                                                                                                                                                                                    |
| 20-23   | Relaxation and `priority_queue.add(...)`     | $O(\log n)$                                 | Comparison, assignment, and insertion/update operations in the priority queue. Inserting/updating an element in the priority queue has logarithmic complexity. In total, these operations are performed $O(m)$ times.                                                                                                         |
| 24-28   | Path reconstruction                          | $O(n)$                                      | Path reconstruction is done by traversing the predecessors from the target node to the source node. In the worst case, the path can have up to $n$ distinct nodes.                                                                                                                                                            |
| 29      | `return path, distances[endId]`              | $O(1)$                                      | Return operation, which is constant.                                                                                                                                                                                                                                                                                          |

---

### 2. Shortest Path Through Ordered List of Stations (`MDISCController.findShortestRouteThroughOrderedStations()`)

**Description:**
Given an ordered list of station names, computes the shortest path for each consecutive pair using Dijkstra. If any segment is impossible, returns an impossible result. Otherwise, concatenates the paths, avoiding duplication.

**Pseudocode:**

```
function findShortestRouteThroughOrderedStations(stationNames):
    if stationNames is null or length < 2:
        return PathResult(impossible)
    totalPath = []
    totalDistance = 0
    firstSegment = true
    for i = 0 to length(stationNames) - 2:
        start = stationNames[i]
        end = stationNames[i+1]
        segmentResult = Dijkstra(graph, start, end)
        if not segmentResult.isPossible:
            return PathResult(impossible)
        totalDistance += segmentResult.distance
        if firstSegment:
            totalPath.addAll(segmentResult.path)
            firstSegment = false
        else:
            totalPath.addAll(segmentResult.path[1:])
    return PathResult(totalPath, totalDistance, true)
```

**Worst-case Time Complexity:**
This algorithm consists of multiple calls to Dijkstra's algorithm. If the `ordered_station_list` contains $k$ stations, then there will be $k-1$ segments for which Dijkstra will be executed (and each execution is independent of the previous, except for the continuity of the stations).

The complexity of each call to Dijkstra is $O((n + m) \log n)$.
Since the main loop runs $k-1$ times, and the dominant operation inside the loop is the call to Dijkstra, the total worst-case complexity is:

$$O(k \cdot (n + m) \log n)$$

Where:
- $k$ is the number of stations in the ordered stop list.
- $n$ is the number of vertices (stations) in the graph.
- $m$ is the number of edges (connections).

#### Updated line-by-line analysis for the ordered path procedure

| Line(s) | Code Snippet                          | Worst-case Complexity | Justification     |
|---------|---------------------------------------|-----------------------|-------------------|
| 1-2     | Null/length check                     | $O(1)$                | Input validation. |
| 3-5     | Initialization                        | $O(1)$                | Variable setup.   |
| 6       | for i = 0 to length(stationNames) - 2 | $O(k)$                | Main loop.        |
| 7-8     | Get start/end                         | $O(1)$                | List access.      |
| 9       | segmentResult = Dijkstra(...)         | $O((n + m) \log n)$   | Dijkstra call.    |
| 10-11   | If not possible, return               | $O(1)$                | Early exit.       |
| 12      | totalDistance += ...                  | $O(1)$                | Arithmetic.       |
| 13-17   | Path concatenation                    | $O(n)$ (local)        | As before.        |
| 18      | Return result                         | $O(1)$                | Return.           |

---

### Note on Graphical Visualization

**According to Acceptance Criterion AC02 of US28, all procedures related to graphical visualization (i.e., drawing the route on the screen) are explicitly excluded from this complexity analysis.** The analysis focuses purely on the efficiency of the underlying algorithms for route calculation.

---

## Conclusion

The algorithms implemented for US27 (Dijkstra for the shortest path and its orchestration for paths with ordered stops) have been thoroughly analyzed regarding their worst-case time complexity. Their implementation, which makes use of efficient data structures such as the priority queue (`java.util.PriorityQueue`), ensures that performance aligns with the theoretical complexities presented. These algorithms meet the requirements of US28 by providing a solid basis for evaluating their computational efficiency.
