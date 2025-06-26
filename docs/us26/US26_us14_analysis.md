# US26 - Algorithm Efficiency Analysis for US14 and US14_Hierholzer (Worst Case)

## Explanation of Terms

| Symbol | Meaning                                       |
|--------|-----------------------------------------------|
| n      | Number of vertices (stations) in the graph    |
| m      | Number of edges (connections) in the graph    |
| k      | Number of selected stations (subset)          |

---

## Algorithms Analyzed from US14 and US14_Hierholzer

### 1. Building the Relevant Subgraph (`buildRelevantGraphAndDegrees()`)

**Description:**  
Traverses the original graph to construct a subgraph including either all lines or only the electrified ones, and computes the degree of each node.

**Pseudocode:**
```
for each station 'from' in the graph:
    for each line (from, to):
        if the line is relevant (according to 'onlyElectrified'):
            add 'from' and 'to' to the subgraph
            update degrees of 'from' and 'to'
```

**Worst-case Time Complexity:**  
O(n + m)


---

### 2. Eulerian Condition Check (`analyzeEulerianConditions()`)

**Description:**  
Counts how many nodes have odd degrees to determine whether an Eulerian trail or circuit exists.

**Pseudocode:**
```
oddDegree = 0
for each node:
    if degree(node) % 2 ≠ 0:
        oddDegree += 1
```

**Worst-case Time Complexity:**  
O(n)

---

### 3. Connectivity Check (`isRelevantSubgraphConnected()`)

**Description:**  
Performs a DFS to verify whether the relevant subgraph is connected.

**Pseudocode:**
```
function DFS(node):
    mark as visited
    for each unvisited neighbor:
        DFS(neighbor)
```

**Worst-case Time Complexity:**  
O(n + m)

---

### 4. Fleury’s Algorithm (`US14.findMaintenanceRoute()` → `fleuryDFS()`)

**Description:**  
Recursively builds an Eulerian path or circuit, selecting non-bridge edges. Internally calls `isEdgeBridge()` which executes `dfsCount()` (a DFS to check connectivity).

**Pseudocode:**
```
function fleury(u):
    for each unused edge (u, v):
        if it’s not a bridge or is the only edge:
            mark as used
            remove edge (u,v)
            fleury(v)
```

**Worst-case Time Complexity:**  
O(m²)

**Note:** Each call to `isEdgeBridge()` performs a full DFS (`dfsCount`), leading to O(m) per edge. Since this may happen for each of the m edges, total cost can reach O(m²).

---

### 5. Hierholzer’s Algorithm (`US14_Hierholzer.findMaintenanceRoute()` → `findEulerianPathHierholzer()`)

**Description:**  
Efficiently constructs the Eulerian path or circuit using a stack. It does not check for bridges and visits each edge only once.

**Pseudocode:**
```
stack.push(start)
while stack not empty:
    u = stack.top()
    if u has unused neighbor:
        mark as used
        stack.push(v)
    else:
        path.append(stack.pop())
```

**Worst-case Time Complexity:**  
O(m)

---

## Conclusion

- Hierholzer’s algorithm is significantly more efficient than Fleury’s for large graphs.
- All algorithms comply with the requirements of US26 and were implemented using efficient structures and techniques described in the MDISC guide.
