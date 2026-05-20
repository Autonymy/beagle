from __future__ import annotations

from .errors import CycleError
from .graph import build_dependency_graph, detect_cycles, topological_order
from .matcher import capable_workers, find_eligible_workers
from .types import (
    Assignment,
    DeadlineMissed,
    DependencyCycle,
    NoCapableWorker,
    Resource,
    ScheduleError,
    ScheduleFailure,
    ScheduleOk,
    ScheduleResult,
    Task,
    Worker,
    WorkerUnavailable,
)

MAX_SLIDE_WINDOW = 1440


def schedule(
    tasks: list[Task],
    workers: list[Worker],
    resources: list[Resource],
) -> ScheduleResult:
    if not tasks:
        return ScheduleOk(assignments=[])

    graph = build_dependency_graph(tasks)
    cycles = detect_cycles(graph)
    if cycles:
        task_ids_in_cycles = _task_ids_in_cycles(cycles)
        failures: list[ScheduleFailure] = []
        for cycle in cycles:
            raw = cycle[:-1]
            for task_id in raw:
                already = any(f.task_id == task_id for f in failures)
                if not already:
                    failures.append(
                        ScheduleFailure(
                            task_id=task_id,
                            reason=DependencyCycle(cycle=cycle),
                        )
                    )
        tasks_not_in_cycles = [
            t for t in tasks if t.id not in task_ids_in_cycles
        ]
        remaining_graph = build_dependency_graph(tasks_not_in_cycles)
        _schedule_remaining(
            tasks_not_in_cycles,
            remaining_graph,
            workers,
            resources,
            failures,
        )
        failures.sort(key=lambda f: f.task_id)
        return ScheduleError(failures=failures)

    try:
        order = topological_order(graph)
    except CycleError as e:
        failures = []
        for cycle in e.cycles:
            raw = cycle[:-1]
            for task_id in raw:
                already = any(f.task_id == task_id for f in failures)
                if not already:
                    failures.append(
                        ScheduleFailure(
                            task_id=task_id,
                            reason=DependencyCycle(cycle=cycle),
                        )
                    )
        failures.sort(key=lambda f: f.task_id)
        return ScheduleError(failures=failures)

    task_map: dict[str, Task] = {t.id: t for t in tasks}
    assignments: list[Assignment] = []
    assignment_map: dict[str, Assignment] = {}
    failures = []

    for task_id in order:
        task = task_map.get(task_id)
        if task is None:
            continue

        result = _schedule_task(
            task, workers, resources, assignments, assignment_map, graph
        )
        if isinstance(result, Assignment):
            assignments.append(result)
            assignment_map[task_id] = result
        elif isinstance(result, ScheduleFailure):
            failures.append(result)

    if failures:
        failures.sort(key=lambda f: f.task_id)
        return ScheduleError(failures=failures)

    assignments.sort(key=lambda a: (a.start_time, a.task_id))
    return ScheduleOk(assignments=assignments)


def _task_ids_in_cycles(cycles: list[list[str]]) -> set[str]:
    ids: set[str] = set()
    for cycle in cycles:
        for task_id in cycle:
            ids.add(task_id)
    return ids


def _schedule_remaining(
    tasks: list[Task],
    graph: object,
    workers: list[Worker],
    resources: list[Resource],
    failures: list[ScheduleFailure],
) -> None:
    pass


def _schedule_task(
    task: Task,
    workers: list[Worker],
    resources: list[Resource],
    existing: list[Assignment],
    assignment_map: dict[str, Assignment],
    graph: object,
) -> Assignment | ScheduleFailure:
    from .graph import DependencyGraph

    assert isinstance(graph, DependencyGraph)

    earliest_start = 0
    for dep_id in task.depends_on:
        dep_assignment = assignment_map.get(dep_id)
        if dep_assignment is not None:
            earliest_start = max(earliest_start, dep_assignment.end_time)

    max_attempts = max(1, task.retry_policy.max_attempts)
    backoff = task.retry_policy.backoff

    attempt_start = earliest_start

    for attempt in range(1, max_attempts + 1):
        result = _try_schedule_at(
            task, workers, resources, existing, attempt_start, attempt
        )
        if isinstance(result, Assignment):
            return result

        if attempt < max_attempts:
            attempt_start = attempt_start + backoff

    return _build_failure(task, workers, attempt_start)


def _try_schedule_at(
    task: Task,
    workers: list[Worker],
    resources: list[Resource],
    existing: list[Assignment],
    earliest_start: int,
    attempt: int,
) -> Assignment | ScheduleFailure | None:
    deadline = task.deadline if task.deadline > 0 else earliest_start + MAX_SLIDE_WINDOW
    max_start = deadline - task.duration if task.duration > 0 else deadline

    start = earliest_start
    while start <= max_start:
        eligible = find_eligible_workers(
            task, workers, resources, existing, start
        )
        if eligible:
            worker = eligible[0]
            return Assignment(
                task_id=task.id,
                worker_id=worker.id,
                start_time=start,
                end_time=start + task.duration,
                attempt=attempt,
            )
        start += 1

    return None


def _build_failure(
    task: Task,
    workers: list[Worker],
    last_attempted_start: int,
) -> ScheduleFailure:
    cap = capable_workers(task, workers)

    if not cap:
        return ScheduleFailure(
            task_id=task.id,
            reason=NoCapableWorker(
                required=sorted(task.capabilities),
                available=[sorted(w.capabilities) for w in sorted(workers, key=lambda w: w.id)],
            ),
        )

    if task.deadline > 0:
        return ScheduleFailure(
            task_id=task.id,
            reason=DeadlineMissed(
                earliest_start=last_attempted_start,
                duration=task.duration,
                deadline=task.deadline,
            ),
        )

    return ScheduleFailure(
        task_id=task.id,
        reason=WorkerUnavailable(
            worker_id=cap[0].id,
            windows=list(cap[0].unavailable),
        ),
    )
