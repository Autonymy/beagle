from __future__ import annotations

from .types import Assignment, Resource, Task, Worker


def capable_workers(task: Task, workers: list[Worker]) -> list[Worker]:
    required = set(task.capabilities)
    result: list[Worker] = []
    for worker in workers:
        if required.issubset(set(worker.capabilities)):
            result.append(worker)
    result.sort(key=lambda w: w.id)
    return result


def available_workers(
    workers: list[Worker], start: int, end: int
) -> list[Worker]:
    result: list[Worker] = []
    for worker in workers:
        if _is_available(worker, start, end):
            result.append(worker)
    result.sort(key=lambda w: w.id)
    return result


def _is_available(worker: Worker, start: int, end: int) -> bool:
    for window in worker.unavailable:
        if _intervals_overlap(start, end, window.start, window.end):
            return False
    return True


def _intervals_overlap(
    start1: int, end1: int, start2: int, end2: int
) -> bool:
    return start1 < end2 and start2 < end1


def check_resource_capacity(
    resource: Resource,
    existing: list[Assignment],
    start: int,
    end: int,
) -> bool:
    count = 0
    for assignment in existing:
        if _intervals_overlap(
            assignment.start_time, assignment.end_time, start, end
        ):
            count += 1
    return count < resource.capacity


def find_eligible_workers(
    task: Task,
    workers: list[Worker],
    resources: list[Resource],
    existing: list[Assignment],
    start: int,
) -> list[Worker]:
    end = start + task.duration
    cap = capable_workers(task, workers)
    avail = available_workers(cap, start, end)

    resource_map: dict[str, Resource] = {r.id: r for r in resources}
    for res_id in task.resources:
        resource = resource_map.get(res_id)
        if resource is not None:
            if not check_resource_capacity(resource, existing, start, end):
                return []

    # Filter out workers who already have an overlapping assignment
    avail = _filter_worker_conflicts(avail, existing, start, end)

    avail.sort(key=lambda w: w.id)
    return avail


def _filter_worker_conflicts(
    workers: list[Worker],
    existing: list[Assignment],
    start: int,
    end: int,
) -> list[Worker]:
    result: list[Worker] = []
    for worker in workers:
        has_conflict = False
        for assignment in existing:
            if assignment.worker_id == worker.id:
                if _intervals_overlap(
                    assignment.start_time, assignment.end_time, start, end
                ):
                    has_conflict = True
                    break
        if not has_conflict:
            result.append(worker)
    return result
