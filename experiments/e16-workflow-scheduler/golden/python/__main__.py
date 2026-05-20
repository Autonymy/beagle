from __future__ import annotations

import json
import sys
from typing import Any

from .scheduler import schedule
from .types import (
    Assignment,
    CapabilityMismatch,
    DeadlineExceeded,
    DeadlineMissed,
    DependencyCycle,
    DependencyOrder,
    NoCapableWorker,
    OverlapViolation,
    Resource,
    ResourceExceeded,
    ResourceOverCapacity,
    RetryPolicy,
    ScheduleError,
    ScheduleFailure,
    ScheduleOk,
    Task,
    Violation,
    Window,
    WindowConflict,
    Worker,
    WorkerUnavailable,
)
from .validator import validate_assignments


def _parse_retry_policy(data: dict[str, Any]) -> RetryPolicy:
    return RetryPolicy(
        max_attempts=int(data.get("max_attempts", 0)),
        backoff=int(data.get("backoff", 0)),
    )


def _parse_window(data: dict[str, Any]) -> Window:
    return Window(
        start=int(data["start"]),
        end=int(data["end"]),
    )


def _parse_task(data: dict[str, Any]) -> Task:
    retry_raw = data.get("retry_policy")
    retry = _parse_retry_policy(retry_raw) if retry_raw else RetryPolicy()
    return Task(
        id=str(data["id"]),
        name=str(data.get("name", "")),
        duration=int(data.get("duration", 0)),
        priority=int(data.get("priority", 1)),
        deadline=int(data.get("deadline", 0)),
        capabilities=list(data.get("capabilities", [])),
        resources=list(data.get("resources", [])),
        depends_on=list(data.get("depends_on", [])),
        retry_policy=retry,
    )


def _parse_worker(data: dict[str, Any]) -> Worker:
    unavailable_raw: list[dict[str, Any]] = data.get("unavailable", [])
    return Worker(
        id=str(data["id"]),
        name=str(data.get("name", "")),
        capabilities=list(data.get("capabilities", [])),
        unavailable=[_parse_window(w) for w in unavailable_raw],
    )


def _parse_resource(data: dict[str, Any]) -> Resource:
    return Resource(
        id=str(data["id"]),
        name=str(data.get("name", "")),
        capacity=int(data.get("capacity", 1)),
    )


def _parse_input(
    data: dict[str, Any],
) -> tuple[list[Task], list[Worker], list[Resource]]:
    tasks = [_parse_task(t) for t in data.get("tasks", [])]
    workers = [_parse_worker(w) for w in data.get("workers", [])]
    resources = [_parse_resource(r) for r in data.get("resources", [])]
    return tasks, workers, resources


def _window_to_dict(w: Window) -> dict[str, Any]:
    return {"start": w.start, "end": w.end}


def _assignment_to_dict(a: Assignment) -> dict[str, Any]:
    return {
        "task_id": a.task_id,
        "worker_id": a.worker_id,
        "start_time": a.start_time,
        "end_time": a.end_time,
        "attempt": a.attempt,
    }


def _failure_reason_to_dict(reason: object) -> dict[str, Any]:
    if isinstance(reason, DependencyCycle):
        return {"type": "DependencyCycle", "cycle": reason.cycle}
    if isinstance(reason, NoCapableWorker):
        return {
            "type": "NoCapableWorker",
            "required": reason.required,
            "available": reason.available,
        }
    if isinstance(reason, ResourceOverCapacity):
        return {
            "type": "ResourceOverCapacity",
            "resource_id": reason.resource_id,
            "capacity": reason.capacity,
            "demanded": reason.demanded,
        }
    if isinstance(reason, DeadlineMissed):
        return {
            "type": "DeadlineMissed",
            "earliest_start": reason.earliest_start,
            "duration": reason.duration,
            "deadline": reason.deadline,
        }
    if isinstance(reason, WorkerUnavailable):
        return {
            "type": "WorkerUnavailable",
            "worker_id": reason.worker_id,
            "windows": [_window_to_dict(w) for w in reason.windows],
        }
    return {"type": "Unknown"}


def _failure_to_dict(f: ScheduleFailure) -> dict[str, Any]:
    return {
        "task_id": f.task_id,
        "reason": _failure_reason_to_dict(f.reason),
    }


def _violation_kind_to_dict(kind: object) -> dict[str, Any]:
    if isinstance(kind, CapabilityMismatch):
        return {"type": "CapabilityMismatch", "missing": kind.missing}
    if isinstance(kind, WindowConflict):
        return {
            "type": "WindowConflict",
            "window": _window_to_dict(kind.window),
        }
    if isinstance(kind, ResourceExceeded):
        return {
            "type": "ResourceExceeded",
            "resource_id": kind.resource_id,
            "at_time": kind.at_time,
            "count": kind.count,
            "capacity": kind.capacity,
        }
    if isinstance(kind, DependencyOrder):
        return {
            "type": "DependencyOrder",
            "dependency_id": kind.dependency_id,
            "dep_end": kind.dep_end,
            "task_start": kind.task_start,
        }
    if isinstance(kind, DeadlineExceeded):
        return {
            "type": "DeadlineExceeded",
            "deadline": kind.deadline,
            "end_time": kind.end_time,
        }
    if isinstance(kind, OverlapViolation):
        return {
            "type": "OverlapViolation",
            "other_task_id": kind.other_task_id,
        }
    return {"type": "Unknown"}


def _violation_to_dict(v: Violation) -> dict[str, Any]:
    return {
        "assignment": _assignment_to_dict(v.assignment),
        "kind": _violation_kind_to_dict(v.kind),
    }


def _result_to_dict(
    result: ScheduleOk | ScheduleError,
    validations: list[Violation] | None = None,
) -> dict[str, Any]:
    output: dict[str, Any] = {}
    if isinstance(result, ScheduleOk):
        output["status"] = "ok"
        output["assignments"] = [
            _assignment_to_dict(a) for a in result.assignments
        ]
        if validations is not None:
            output["violations"] = [
                _violation_to_dict(v) for v in validations
            ]
    elif isinstance(result, ScheduleError):
        output["status"] = "error"
        output["failures"] = [_failure_to_dict(f) for f in result.failures]
    return output


def main() -> None:
    raw = sys.stdin.read()
    data: dict[str, Any] = json.loads(raw)

    tasks, workers, resources = _parse_input(data)
    result = schedule(tasks, workers, resources)

    validations: list[Violation] | None = None
    if isinstance(result, ScheduleOk) and data.get("validate", False):
        validations = validate_assignments(
            result.assignments, tasks, workers, resources
        )

    output = _result_to_dict(result, validations)
    json.dump(output, sys.stdout, indent=2)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
