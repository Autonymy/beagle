from __future__ import annotations

from dataclasses import dataclass, field

from .errors import CycleError
from .types import Task


@dataclass
class DependencyGraph:
    adjacency: dict[str, list[str]] = field(default_factory=dict)
    reverse: dict[str, list[str]] = field(default_factory=dict)
    nodes: set[str] = field(default_factory=set)
    task_priority: dict[str, int] = field(default_factory=dict)


def build_dependency_graph(tasks: list[Task]) -> DependencyGraph:
    graph = DependencyGraph()
    for task in tasks:
        graph.nodes.add(task.id)
        graph.task_priority[task.id] = task.priority
        if task.id not in graph.adjacency:
            graph.adjacency[task.id] = []
        if task.id not in graph.reverse:
            graph.reverse[task.id] = []
        for dep_id in task.depends_on:
            if dep_id not in graph.adjacency:
                graph.adjacency[dep_id] = []
            if dep_id not in graph.reverse:
                graph.reverse[dep_id] = []
            graph.adjacency[dep_id].append(task.id)
            graph.reverse[task.id].append(dep_id)
            graph.nodes.add(dep_id)
    return graph


def detect_cycles(graph: DependencyGraph) -> list[list[str]]:
    WHITE = 0
    GRAY = 1
    BLACK = 2

    color: dict[str, int] = {node: WHITE for node in graph.nodes}
    path: list[str] = []
    cycles: list[list[str]] = []

    def _dfs(node: str) -> None:
        color[node] = GRAY
        path.append(node)
        for neighbor in sorted(graph.adjacency.get(node, [])):
            if color[neighbor] == GRAY:
                cycle_start = path.index(neighbor)
                cycle = path[cycle_start:] + [neighbor]
                cycles.append(cycle)
            elif color[neighbor] == WHITE:
                _dfs(neighbor)
        path.pop()
        color[node] = BLACK

    for node in sorted(graph.nodes):
        if color[node] == WHITE:
            _dfs(node)

    return _deduplicate_cycles(cycles)


def _deduplicate_cycles(cycles: list[list[str]]) -> list[list[str]]:
    seen: set[tuple[str, ...]] = set()
    unique: list[list[str]] = []
    for cycle in cycles:
        raw = cycle[:-1]
        if not raw:
            continue
        min_idx = 0
        for i in range(1, len(raw)):
            if raw[i] < raw[min_idx]:
                min_idx = i
        canonical = tuple(raw[min_idx:] + raw[:min_idx])
        if canonical not in seen:
            seen.add(canonical)
            unique.append(cycle)
    unique.sort(key=lambda c: (len(c), c))
    return unique


def topological_order(graph: DependencyGraph) -> list[str]:
    cycles = detect_cycles(graph)
    if cycles:
        raise CycleError(cycles=cycles)

    in_degree: dict[str, int] = {node: 0 for node in graph.nodes}
    for node in graph.nodes:
        for neighbor in graph.adjacency.get(node, []):
            in_degree[neighbor] = in_degree.get(neighbor, 0) + 1

    queue: list[str] = sorted(
        [n for n in graph.nodes if in_degree[n] == 0],
        key=lambda n: (graph.task_priority.get(n, 0), n),
    )

    result: list[str] = []
    while queue:
        node = queue.pop(0)
        result.append(node)
        successors: list[str] = []
        for neighbor in graph.adjacency.get(node, []):
            in_degree[neighbor] -= 1
            if in_degree[neighbor] == 0:
                successors.append(neighbor)
        successors.sort(key=lambda n: (graph.task_priority.get(n, 0), n))
        for s in successors:
            _insert_sorted(queue, s, graph.task_priority)

    return result


def _insert_sorted(
    queue: list[str],
    item: str,
    priorities: dict[str, int],
) -> None:
    key = (priorities.get(item, 0), item)
    lo = 0
    hi = len(queue)
    while lo < hi:
        mid = (lo + hi) // 2
        mid_key = (priorities.get(queue[mid], 0), queue[mid])
        if mid_key <= key:
            lo = mid + 1
        else:
            hi = mid
    queue.insert(lo, item)
