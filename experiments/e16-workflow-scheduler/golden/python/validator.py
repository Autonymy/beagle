from __future__ import annotations

from .types import (
    Assignment,
    CapabilityMismatch,
    DeadlineExceeded,
    DependencyOrder,
    OverlapViolation,
    Resource,
    ResourceExceeded,
    Task,
    Violation,
    Window,
    WindowConflict,
    Worker,
)


def validate_assignments(
    assignments: list[Assignment],
    tasks: list[Task],
    workers: list[Worker],
    resources: list[Resource],
) -> list[Violation]:
    violations: list[Violation] = []

    task_map: dict[str, Task] = {t.id: t for t in tasks}
    worker_map: dict[str, Worker] = {w.id: w for w in workers}
    resource_map: dict[str, Resource] = {r.id: r for r in resources}
    assignment_map: dict[str, Assignment] = {a.task_id: a for a in assignments}

    for assignment in assignments:
        task = task_map.get(assignment.task_id)
        worker = worker_map.get(assignment.worker_id)

        if task is None or worker is None:
            continue

        _check_capabilities(assignment, task, worker, violations)
        _check_windows(assignment, worker, violations)
        _check_deadline(assignment, task, violations)
        _check_dependencies(assignment, task, assignment_map, violations)
        _check_overlaps(assignment, assignments, violations)

    _check_resources(assignments, task_map, resource_map, violations)

    violations.sort(
        key=lambda v: (v.assignment.start_time, v.assignment.task_id)
    )
    return violations


def _check_capabilities(
    assignment: Assignment,
    task: Task,
    worker: Worker,
    violations: list[Violation],
) -> None:
    worker_caps = set(worker.capabilities)
    missing = [c for c in sorted(task.capabilities) if c not in worker_caps]
    if missing:
        violations.append(
            Violation(
                assignment=assignment,
                kind=CapabilityMismatch(missing=missing),
            )
        )


def _check_windows(
    assignment: Assignment,
    worker: Worker,
    violations: list[Violation],
) -> None:
    for window in worker.unavailable:
        if _intervals_overlap(
            assignment.start_time,
            assignment.end_time,
            window.start,
            window.end,
        ):
            violations.append(
                Violation(
                    assignment=assignment,
                    kind=WindowConflict(window=window),
                )
            )


def _check_deadline(
    assignment: Assignment,
    task: Task,
    violations: list[Violation],
) -> None:
    if task.deadline > 0 and assignment.end_time > task.deadline:
        violations.append(
            Violation(
                assignment=assignment,
                kind=DeadlineExceeded(
                    deadline=task.deadline,
                    end_time=assignment.end_time,
                ),
            )
        )


def _check_dependencies(
    assignment: Assignment,
    task: Task,
    assignment_map: dict[str, Assignment],
    violations: list[Violation],
) -> None:
    for dep_id in task.depends_on:
        dep_assignment = assignment_map.get(dep_id)
        if dep_assignment is not None:
            if dep_assignment.end_time > assignment.start_time:
                violations.append(
                    Violation(
                        assignment=assignment,
                        kind=DependencyOrder(
                            dependency_id=dep_id,
                            dep_end=dep_assignment.end_time,
                            task_start=assignment.start_time,
                        ),
                    )
                )


def _check_overlaps(
    assignment: Assignment,
    all_assignments: list[Assignment],
    violations: list[Violation],
) -> None:
    for other in all_assignments:
        if other.task_id == assignment.task_id:
            continue
        if other.worker_id != assignment.worker_id:
            continue
        if _intervals_overlap(
            assignment.start_time,
            assignment.end_time,
            other.start_time,
            other.end_time,
        ):
            violations.append(
                Violation(
                    assignment=assignment,
                    kind=OverlapViolation(other_task_id=other.task_id),
                )
            )


def _check_resources(
    assignments: list[Assignment],
    task_map: dict[str, Task],
    resource_map: dict[str, Resource],
    violations: list[Violation],
) -> None:
    resource_assignments: dict[str, list[Assignment]] = {}
    for assignment in assignments:
        task = task_map.get(assignment.task_id)
        if task is None:
            continue
        for res_id in task.resources:
            if res_id not in resource_assignments:
                resource_assignments[res_id] = []
            resource_assignments[res_id].append(assignment)

    for res_id, res_assignments in sorted(resource_assignments.items()):
        resource = resource_map.get(res_id)
        if resource is None:
            continue

        time_points: set[int] = set()
        for a in res_assignments:
            time_points.add(a.start_time)
            time_points.add(a.end_time)

        for t in sorted(time_points):
            count = 0
            for a in res_assignments:
                if a.start_time <= t < a.end_time:
                    count += 1
            if count > resource.capacity:
                for a in res_assignments:
                    if a.start_time <= t < a.end_time:
                        already_reported = any(
                            v.assignment.task_id == a.task_id
                            and isinstance(v.kind, ResourceExceeded)
                            and v.kind.resource_id == res_id
                            for v in violations
                        )
                        if not already_reported:
                            violations.append(
                                Violation(
                                    assignment=a,
                                    kind=ResourceExceeded(
                                        resource_id=res_id,
                                        at_time=t,
                                        count=count,
                                        capacity=resource.capacity,
                                    ),
                                )
                            )


def _intervals_overlap(
    start1: int, end1: int, start2: int, end2: int
) -> bool:
    return start1 < end2 and start2 < end1
