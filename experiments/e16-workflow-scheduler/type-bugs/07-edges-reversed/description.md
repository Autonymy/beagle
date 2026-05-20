# Bug 07: Dependency graph edges reversed (logic error)

**File:** graph.bgl, line 38
**Category:** Logic error
**Checker catches:** No (both maps have same types)

**Change:** Swap adjacency and reverse-adj in the DependencyGraph constructor:
`(->DependencyGraph adjacency reverse-adj task-ids)` →
`(->DependencyGraph reverse-adj adjacency task-ids)`

**Effect:** Dependencies are backwards. If A depends on B, the graph thinks
B depends on A. Topological order is reversed. Tasks start before their
dependencies complete.
