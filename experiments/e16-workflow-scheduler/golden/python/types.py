from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Union


@dataclass(frozen=True)
class RetryPolicy:
    max_attempts: int = 0
    backoff: int = 0


@dataclass(frozen=True)
class Task:
    id: str
    name: str
    duration: int
    priority: int
    deadline: int = 0
    capabilities: list[str] = field(default_factory=list)
    resources: list[str] = field(default_factory=list)
    depends_on: list[str] = field(default_factory=list)
    retry_policy: RetryPolicy = field(default_factory=RetryPolicy)


@dataclass(frozen=True)
class Window:
    start: int
    end: int


@dataclass(frozen=True)
class Worker:
    id: str
    name: str
    capabilities: list[str] = field(default_factory=list)
    unavailable: list[Window] = field(default_factory=list)


@dataclass(frozen=True)
class Resource:
    id: str
    name: str
    capacity: int = 1


@dataclass(frozen=True)
class Dependency:
    from_task: str
    to_task: str


@dataclass(frozen=True)
class Assignment:
    task_id: str
    worker_id: str
    start_time: int
    end_time: int
    attempt: int = 1


# --- Failure reasons ---

@dataclass(frozen=True)
class DependencyCycle:
    cycle: list[str] = field(default_factory=list)


@dataclass(frozen=True)
class NoCapableWorker:
    required: list[str] = field(default_factory=list)
    available: list[list[str]] = field(default_factory=list)


@dataclass(frozen=True)
class ResourceOverCapacity:
    resource_id: str = ""
    capacity: int = 0
    demanded: int = 0


@dataclass(frozen=True)
class DeadlineMissed:
    earliest_start: int = 0
    duration: int = 0
    deadline: int = 0


@dataclass(frozen=True)
class WorkerUnavailable:
    worker_id: str = ""
    windows: list[Window] = field(default_factory=list)


FailureReason = Union[
    DependencyCycle,
    NoCapableWorker,
    ResourceOverCapacity,
    DeadlineMissed,
    WorkerUnavailable,
]


@dataclass(frozen=True)
class ScheduleFailure:
    task_id: str
    reason: FailureReason


@dataclass(frozen=True)
class ScheduleOk:
    assignments: list[Assignment] = field(default_factory=list)


@dataclass(frozen=True)
class ScheduleError:
    failures: list[ScheduleFailure] = field(default_factory=list)


ScheduleResult = Union[ScheduleOk, ScheduleError]


# --- Violation kinds ---

@dataclass(frozen=True)
class CapabilityMismatch:
    missing: list[str] = field(default_factory=list)


@dataclass(frozen=True)
class WindowConflict:
    window: Window = field(default_factory=lambda: Window(0, 0))


@dataclass(frozen=True)
class ResourceExceeded:
    resource_id: str = ""
    at_time: int = 0
    count: int = 0
    capacity: int = 0


@dataclass(frozen=True)
class DependencyOrder:
    dependency_id: str = ""
    dep_end: int = 0
    task_start: int = 0


@dataclass(frozen=True)
class DeadlineExceeded:
    deadline: int = 0
    end_time: int = 0


@dataclass(frozen=True)
class OverlapViolation:
    other_task_id: str = ""


ViolationKind = Union[
    CapabilityMismatch,
    WindowConflict,
    ResourceExceeded,
    DependencyOrder,
    DeadlineExceeded,
    OverlapViolation,
]


@dataclass(frozen=True)
class Violation:
    assignment: Assignment
    kind: ViolationKind
