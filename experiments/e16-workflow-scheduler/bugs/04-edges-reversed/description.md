# Bug 04: Dependency graph edges reversed

**Category:** Logic error

**Description:**
In `build_dependency_graph`, the direction of dependency edges is swapped.
Instead of recording "task A depends on B" as an edge B -> A (so B is
processed before A in topological sort), the bug records it as A -> B.
This means topological sort sees the edges backwards: it thinks B depends
on A, so it schedules A first -- exactly the wrong order.

**Mutation:**
- Python `graph.py`: swap `graph.adjacency[dep_id].append(task.id)` with
  `graph.adjacency[task.id].append(dep_id)`, and correspondingly swap the
  `graph.reverse` line. (2 lines changed)
- Beagle `graph.bgl`: swap the `adjacency` and `reverse-adj` arguments in
  the `->DependencyGraph` constructor call. (1 line changed)

**Expected oracle impact:**
Topological ordering is inverted with respect to dependencies. Tasks that
depend on others run before their dependencies, violating the dependency
constraint. The oracle should flag `DependencyOrder` violations in every
schedule that has non-trivial dependencies. Cycle detection may also be
affected since the adjacency direction determines which cycles are found.
