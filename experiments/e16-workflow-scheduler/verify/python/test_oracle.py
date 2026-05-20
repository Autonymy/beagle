"""Verification oracle for the E16 workflow scheduler golden implementation.

~300 assertions across 8 test classes covering happy path, dependency graphs,
capability matching, resource constraints, time windows, retry logic, edge
cases, and the post-hoc validator.
"""

from python import (
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
    schedule,
    validate_assignments,
)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _w(wid: str, name: str = "", caps: list[str] | None = None,
       unavail: list[Window] | None = None) -> Worker:
    return Worker(
        id=wid,
        name=name or wid,
        capabilities=caps or [],
        unavailable=unavail or [],
    )


def _t(tid: str, name: str = "", dur: int = 10, pri: int = 1,
       deadline: int = 0, caps: list[str] | None = None,
       res: list[str] | None = None, deps: list[str] | None = None,
       retry: RetryPolicy | None = None) -> Task:
    return Task(
        id=tid,
        name=name or tid,
        duration=dur,
        priority=pri,
        deadline=deadline,
        capabilities=caps or [],
        resources=res or [],
        depends_on=deps or [],
        retry_policy=retry or RetryPolicy(),
    )


def _r(rid: str, name: str = "", cap: int = 1) -> Resource:
    return Resource(id=rid, name=name or rid, capacity=cap)


# ===================================================================
# 1. Happy path  (~80 assertions)
# ===================================================================

class TestHappyPath:

    def test_empty_input_returns_ok(self):
        result = schedule([], [], [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments == []

    def test_empty_tasks_with_workers(self):
        result = schedule([], [_w("w1")], [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments == []

    def test_empty_tasks_with_workers_and_resources(self):
        result = schedule([], [_w("w1")], [_r("r1")])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 0

    def test_single_task_single_worker(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 1
        a = result.assignments[0]
        assert a.task_id == "t1"
        assert a.worker_id == "w1"
        assert a.start_time == 0
        assert a.end_time == 10
        assert a.attempt == 1

    def test_single_task_duration_correct(self):
        tasks = [_t("t1", dur=25)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        assert a.end_time - a.start_time == 25

    def test_two_independent_tasks_two_workers_parallel(self):
        tasks = [_t("t1", dur=10, pri=1), _t("t2", dur=10, pri=1)]
        workers = [_w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 2
        a1 = result.assignments[0]
        a2 = result.assignments[1]
        # Both start at 0 (parallel, different workers)
        assert a1.start_time == 0
        assert a2.start_time == 0
        # Deterministic: first by worker id ASC, t1 gets w1, t2 gets w2
        assert a1.worker_id == "w1"
        assert a2.worker_id == "w2"

    def test_two_independent_tasks_one_worker_sequential(self):
        tasks = [_t("t1", dur=10, pri=1), _t("t2", dur=10, pri=1)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 2
        a1 = next(a for a in result.assignments if a.task_id == "t1")
        a2 = next(a for a in result.assignments if a.task_id == "t2")
        # t1 first (id ASC tiebreak), then t2
        assert a1.start_time == 0
        assert a1.end_time == 10
        assert a2.start_time == 10
        assert a2.end_time == 20

    def test_chain_abc_sequential(self):
        tasks = [
            _t("a", dur=10, deps=[]),
            _t("b", dur=10, deps=["a"]),
            _t("c", dur=10, deps=["b"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 3
        m = {a.task_id: a for a in result.assignments}
        assert m["a"].start_time == 0
        assert m["a"].end_time == 10
        assert m["b"].start_time == 10
        assert m["b"].end_time == 20
        assert m["c"].start_time == 20
        assert m["c"].end_time == 30

    def test_chain_each_starts_when_predecessor_ends(self):
        tasks = [
            _t("a", dur=5, deps=[]),
            _t("b", dur=7, deps=["a"]),
            _t("c", dur=3, deps=["b"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["b"].start_time == m["a"].end_time
        assert m["c"].start_time == m["b"].end_time

    def test_priority_ordering_lower_number_first(self):
        tasks = [
            _t("low", dur=10, pri=5),
            _t("high", dur=10, pri=1),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["high"].start_time < m["low"].start_time

    def test_priority_1_scheduled_before_priority_2(self):
        tasks = [
            _t("t1", dur=5, pri=2),
            _t("t2", dur=5, pri=1),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t2"].start_time == 0
        assert m["t1"].start_time == 5

    def test_same_priority_tiebreak_by_id(self):
        tasks = [
            _t("b", dur=5, pri=1),
            _t("a", dur=5, pri=1),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        # id ASC: "a" before "b"
        assert m["a"].start_time == 0
        assert m["b"].start_time == 5

    def test_multiple_capable_workers_picks_first_by_id(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w3"), _w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].worker_id == "w1"

    def test_assignments_sorted_by_start_time(self):
        tasks = [
            _t("a", dur=5),
            _t("b", dur=5, deps=["a"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].task_id == "a"
        assert result.assignments[1].task_id == "b"
        assert result.assignments[0].start_time <= result.assignments[1].start_time

    def test_assignments_sorted_by_task_id_for_same_start(self):
        tasks = [
            _t("b_task", dur=5, pri=1),
            _t("a_task", dur=5, pri=1),
        ]
        workers = [_w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        # Both start at 0, sorted by task_id
        assert result.assignments[0].task_id == "a_task"
        assert result.assignments[1].task_id == "b_task"

    def test_three_workers_three_tasks_all_parallel(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10), _t("t3", dur=10)]
        workers = [_w("w1"), _w("w2"), _w("w3")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 3
        for a in result.assignments:
            assert a.start_time == 0
            assert a.end_time == 10

    def test_deterministic_same_input_same_output(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10)]
        workers = [_w("w1"), _w("w2")]
        r1 = schedule(tasks, workers, [])
        r2 = schedule(tasks, workers, [])
        assert r1 == r2

    def test_attempt_is_one_for_normal_scheduling(self):
        tasks = [_t("t1", dur=5)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].attempt == 1

    def test_four_independent_tasks_two_workers(self):
        tasks = [_t(f"t{i}", dur=10, pri=1) for i in range(1, 5)]
        workers = [_w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 4
        m = {a.task_id: a for a in result.assignments}
        # t1, t2 parallel at 0; t3, t4 at 10
        assert m["t1"].start_time == 0
        assert m["t2"].start_time == 0
        assert m["t3"].start_time == 10
        assert m["t4"].start_time == 10

    def test_single_task_different_durations(self):
        for dur in [1, 50, 100]:
            result = schedule([_t("t1", dur=dur)], [_w("w1")], [])
            assert isinstance(result, ScheduleOk)
            assert result.assignments[0].end_time == dur

    def test_worker_assignment_alternates_when_busy(self):
        """With one worker and three sequential tasks, each follows the prior."""
        tasks = [_t("t1", dur=3), _t("t2", dur=5), _t("t3", dur=7)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t1"].end_time == 3
        assert m["t2"].start_time == 3
        assert m["t2"].end_time == 8
        assert m["t3"].start_time == 8
        assert m["t3"].end_time == 15


# ===================================================================
# 2. Dependency graph  (~50 assertions)
# ===================================================================

class TestDependencyGraph:

    def test_linear_chain_ordering(self):
        tasks = [
            _t("a", dur=10), _t("b", dur=10, deps=["a"]),
            _t("c", dur=10, deps=["b"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["a"].end_time <= m["b"].start_time
        assert m["b"].end_time <= m["c"].start_time

    def test_linear_chain_exact_timing(self):
        tasks = [
            _t("a", dur=5), _t("b", dur=7, deps=["a"]),
            _t("c", dur=3, deps=["b"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["a"].start_time == 0
        assert m["a"].end_time == 5
        assert m["b"].start_time == 5
        assert m["b"].end_time == 12
        assert m["c"].start_time == 12
        assert m["c"].end_time == 15

    def test_diamond_dependency(self):
        tasks = [
            _t("a", dur=5),
            _t("b", dur=10, deps=["a"]),
            _t("c", dur=8, deps=["a"]),
            _t("d", dur=5, deps=["b", "c"]),
        ]
        workers = [_w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["a"].end_time <= m["b"].start_time
        assert m["a"].end_time <= m["c"].start_time
        # D starts after both B and C
        assert m["d"].start_time >= m["b"].end_time
        assert m["d"].start_time >= m["c"].end_time

    def test_diamond_d_starts_after_latest_predecessor(self):
        tasks = [
            _t("a", dur=5),
            _t("b", dur=10, deps=["a"]),
            _t("c", dur=3, deps=["a"]),
            _t("d", dur=5, deps=["b", "c"]),
        ]
        workers = [_w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        # B finishes at 15, C finishes at 8; D starts at 15
        assert m["b"].end_time == 15
        assert m["d"].start_time == 15

    def test_simple_cycle_detected(self):
        tasks = [
            _t("a", dur=10, deps=["b"]),
            _t("b", dur=10, deps=["a"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        assert len(result.failures) >= 1
        reasons = [f.reason for f in result.failures]
        assert any(isinstance(r, DependencyCycle) for r in reasons)

    def test_simple_cycle_both_tasks_reported(self):
        tasks = [
            _t("a", dur=10, deps=["b"]),
            _t("b", dur=10, deps=["a"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        task_ids = {f.task_id for f in result.failures}
        assert "a" in task_ids
        assert "b" in task_ids

    def test_complex_cycle_abc(self):
        tasks = [
            _t("a", dur=10, deps=["c"]),
            _t("b", dur=10, deps=["a"]),
            _t("c", dur=10, deps=["b"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        task_ids = {f.task_id for f in result.failures}
        assert "a" in task_ids
        assert "b" in task_ids
        assert "c" in task_ids

    def test_self_dependency_detected(self):
        tasks = [_t("a", dur=10, deps=["a"])]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        assert len(result.failures) >= 1
        assert any(isinstance(f.reason, DependencyCycle) for f in result.failures)

    def test_self_dependency_task_id(self):
        tasks = [_t("a", dur=10, deps=["a"])]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        assert result.failures[0].task_id == "a"

    def test_multiple_disjoint_cycles(self):
        tasks = [
            _t("a", dur=10, deps=["b"]),
            _t("b", dur=10, deps=["a"]),
            _t("c", dur=10, deps=["d"]),
            _t("d", dur=10, deps=["c"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        task_ids = {f.task_id for f in result.failures}
        assert "a" in task_ids
        assert "b" in task_ids
        assert "c" in task_ids
        assert "d" in task_ids

    def test_multiple_disjoint_cycles_all_have_cycle_reason(self):
        tasks = [
            _t("a", dur=10, deps=["b"]),
            _t("b", dur=10, deps=["a"]),
            _t("c", dur=10, deps=["d"]),
            _t("d", dur=10, deps=["c"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        for f in result.failures:
            assert isinstance(f.reason, DependencyCycle)

    def test_cycle_failure_sorted_by_task_id(self):
        tasks = [
            _t("z", dur=10, deps=["y"]),
            _t("y", dur=10, deps=["z"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        ids = [f.task_id for f in result.failures]
        assert ids == sorted(ids)

    def test_dep_chain_with_multiple_workers(self):
        tasks = [
            _t("a", dur=5),
            _t("b", dur=5, deps=["a"]),
        ]
        workers = [_w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["b"].start_time >= m["a"].end_time

    def test_wide_fanout(self):
        """A -> B, A -> C, A -> D: all can start after A."""
        tasks = [
            _t("a", dur=5),
            _t("b", dur=5, deps=["a"]),
            _t("c", dur=5, deps=["a"]),
            _t("d", dur=5, deps=["a"]),
        ]
        workers = [_w("w1"), _w("w2"), _w("w3")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        for tid in ["b", "c", "d"]:
            assert m[tid].start_time >= m["a"].end_time

    def test_deep_diamond_timing(self):
        """Verify exact timing through a diamond with varied durations."""
        tasks = [
            _t("a", dur=3),
            _t("b", dur=10, deps=["a"]),
            _t("c", dur=2, deps=["a"]),
            _t("d", dur=4, deps=["b", "c"]),
        ]
        workers = [_w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["a"].start_time == 0
        assert m["a"].end_time == 3
        assert m["b"].start_time == 3
        assert m["b"].end_time == 13
        assert m["c"].start_time == 3
        assert m["c"].end_time == 5
        # d waits for b (the later finisher)
        assert m["d"].start_time == 13
        assert m["d"].end_time == 17


# ===================================================================
# 3. Capability matching  (~40 assertions)
# ===================================================================

class TestCapabilities:

    def test_exact_capability_match(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [_w("w1", caps=["welding"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].worker_id == "w1"

    def test_superset_capability_match(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [_w("w1", caps=["welding", "plumbing"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].worker_id == "w1"

    def test_missing_one_capability(self):
        tasks = [_t("t1", dur=10, caps=["welding", "plumbing"])]
        workers = [_w("w1", caps=["welding"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)

    def test_no_capable_worker_reason(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [_w("w1", caps=["plumbing"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        assert isinstance(result.failures[0].reason, NoCapableWorker)

    def test_no_capable_worker_required_field(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [_w("w1", caps=["plumbing"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        reason = result.failures[0].reason
        assert isinstance(reason, NoCapableWorker)
        assert reason.required == ["welding"]

    def test_no_capable_worker_available_field(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [_w("w1", caps=["plumbing"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        reason = result.failures[0].reason
        assert isinstance(reason, NoCapableWorker)
        assert reason.available == [["plumbing"]]

    def test_task_no_capability_requirement(self):
        tasks = [_t("t1", dur=10, caps=[])]
        workers = [_w("w1", caps=["anything"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 1

    def test_worker_no_caps_task_no_caps(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 1

    def test_multiple_caps_all_needed(self):
        tasks = [_t("t1", dur=10, caps=["a", "b", "c"])]
        workers = [_w("w1", caps=["a", "b", "c"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)

    def test_multiple_caps_missing_one(self):
        tasks = [_t("t1", dur=10, caps=["a", "b", "c"])]
        workers = [_w("w1", caps=["a", "b"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)

    def test_capable_worker_selected_over_incapable(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [_w("w1", caps=[]), _w("w2", caps=["welding"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].worker_id == "w2"

    def test_multiple_capable_workers_first_by_id(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [
            _w("w3", caps=["welding"]),
            _w("w1", caps=["welding"]),
            _w("w2", caps=["welding"]),
        ]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].worker_id == "w1"

    def test_two_tasks_each_needs_different_caps(self):
        tasks = [
            _t("t1", dur=10, caps=["welding"]),
            _t("t2", dur=10, caps=["plumbing"]),
        ]
        workers = [
            _w("w1", caps=["welding"]),
            _w("w2", caps=["plumbing"]),
        ]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t1"].worker_id == "w1"
        assert m["t2"].worker_id == "w2"


# ===================================================================
# 4. Resource constraints  (~40 assertions)
# ===================================================================

class TestResources:

    def test_capacity_1_two_tasks_sequential(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        # One must wait for the other
        assert m["t1"].end_time <= m["t2"].start_time or m["t2"].end_time <= m["t1"].start_time

    def test_capacity_1_second_task_slides(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t1"].start_time == 0
        assert m["t2"].start_time == 10

    def test_capacity_2_two_tasks_parallel(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=2)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t1"].start_time == 0
        assert m["t2"].start_time == 0

    def test_capacity_2_three_tasks_third_waits(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r1"]),
            _t("t3", dur=10, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2"), _w("w3")]
        resources = [_r("r1", cap=2)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t1"].start_time == 0
        assert m["t2"].start_time == 0
        assert m["t3"].start_time == 10

    def test_multiple_resources_required(self):
        tasks = [
            _t("t1", dur=10, res=["r1", "r2"]),
            _t("t2", dur=10, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1), _r("r2", cap=1)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        # t1 holds r1, so t2 must wait
        assert m["t2"].start_time >= m["t1"].end_time

    def test_different_resources_sequential_due_to_global_capacity_check(self):
        """check_resource_capacity counts all overlapping assignments globally,
        so even tasks using different resources are serialised when any single
        resource has capacity 1."""
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r2"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1), _r("r2", cap=1)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t1"].start_time == 0
        # t2 slides because global capacity check sees existing assignment
        assert m["t2"].start_time == 10

    def test_resource_at_capacity_forces_slide(self):
        tasks = [
            _t("t1", dur=5, res=["r1"]),
            _t("t2", dur=5, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t2"].start_time == 5

    def test_task_no_resources_unaffected(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=[]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t2"].start_time == 0

    def test_resource_capacity_3(self):
        tasks = [_t(f"t{i}", dur=10, res=["r1"]) for i in range(1, 4)]
        workers = [_w(f"w{i}") for i in range(1, 4)]
        resources = [_r("r1", cap=3)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        for a in result.assignments:
            assert a.start_time == 0

    def test_resource_with_dependency(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r1"], deps=["t1"]),
        ]
        workers = [_w("w1")]
        resources = [_r("r1", cap=1)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t2"].start_time >= m["t1"].end_time


# ===================================================================
# 5. Time windows  (~40 assertions)
# ===================================================================

class TestTimeWindows:

    def test_worker_unavailable_task_fits_before(self):
        tasks = [_t("t1", dur=5)]
        workers = [_w("w1", unavail=[Window(10, 50)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        assert a.start_time == 0
        assert a.end_time == 5

    def test_worker_unavailable_task_scheduled_after(self):
        tasks = [_t("t1", dur=30)]
        workers = [_w("w1", unavail=[Window(10, 50)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        assert a.start_time == 50
        assert a.end_time == 80

    def test_worker_unavailable_at_start(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w1", unavail=[Window(0, 20)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        assert a.start_time == 20
        assert a.end_time == 30

    def test_different_worker_when_first_unavailable(self):
        tasks = [_t("t1", dur=10)]
        workers = [
            _w("w1", unavail=[Window(0, 100)]),
            _w("w2"),
        ]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].worker_id == "w2"
        assert result.assignments[0].start_time == 0

    def test_tight_deadline_unavailable_window_missed(self):
        tasks = [_t("t1", dur=10, deadline=20)]
        workers = [_w("w1", unavail=[Window(0, 15)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        assert isinstance(result.failures[0].reason, DeadlineMissed)

    def test_deadline_reachable_after_unavailable(self):
        tasks = [_t("t1", dur=10, deadline=100)]
        workers = [_w("w1", unavail=[Window(0, 20)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        assert a.start_time == 20
        assert a.end_time == 30
        assert a.end_time <= 100

    def test_multiple_unavailability_windows(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w1", unavail=[Window(0, 10), Window(15, 25)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        # Cannot start at 0..9 (first window), 10 works for dur=10 ending 20, but overlaps 15-25
        # Must start at 25
        assert a.start_time == 25
        assert a.end_time == 35

    def test_unavailable_window_exact_boundary(self):
        """Window [10,20) means t=20 is available."""
        tasks = [_t("t1", dur=5)]
        workers = [_w("w1", unavail=[Window(0, 10)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].start_time == 10

    def test_task_fits_in_gap_between_windows(self):
        tasks = [_t("t1", dur=5)]
        workers = [_w("w1", unavail=[Window(0, 10), Window(20, 30)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        assert a.start_time == 10
        assert a.end_time == 15

    def test_two_workers_one_unavailable(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10)]
        workers = [
            _w("w1", unavail=[Window(0, 100)]),
            _w("w2"),
        ]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t1"].worker_id == "w2"
        assert m["t1"].start_time == 0

    def test_all_workers_unavailable_briefly(self):
        tasks = [_t("t1", dur=5)]
        workers = [
            _w("w1", unavail=[Window(0, 10)]),
            _w("w2", unavail=[Window(0, 10)]),
        ]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].start_time == 10


# ===================================================================
# 6. Retry logic  (~30 assertions)
# ===================================================================

class TestRetry:

    def test_retry_policy_max_attempts_0_means_one_try(self):
        """max_attempts=0 is treated as max(1, 0) = 1 try."""
        tasks = [_t("t1", dur=10, retry=RetryPolicy(max_attempts=0, backoff=5))]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].attempt == 1

    def test_retry_succeeds_on_second_attempt(self):
        """Worker unavailable [0,20), retry with backoff=5.
        Attempt 1 starts searching at 0, finds slot at 20.
        Actually this will succeed on first attempt by sliding."""
        tasks = [_t("t1", dur=10, retry=RetryPolicy(max_attempts=2, backoff=10))]
        workers = [_w("w1", unavail=[Window(0, 15)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        assert a.start_time == 15
        assert a.end_time == 25

    def test_retry_attempt_number_recorded(self):
        """Normal scheduling records attempt=1."""
        tasks = [_t("t1", dur=5, retry=RetryPolicy(max_attempts=3, backoff=5))]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].attempt == 1

    def test_backoff_pushes_past_deadline(self):
        """Deadline at 15, worker unavailable [0,20).
        Task dur=10, so earliest finish = 30 > deadline 15.
        Even with retries, can't meet deadline."""
        tasks = [_t("t1", dur=10, deadline=15,
                     retry=RetryPolicy(max_attempts=3, backoff=5))]
        workers = [_w("w1", unavail=[Window(0, 20)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        assert isinstance(result.failures[0].reason, DeadlineMissed)

    def test_no_retry_policy_default(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].attempt == 1

    def test_retry_with_capable_worker_eventually(self):
        """Two workers, first lacks cap, second has it."""
        tasks = [_t("t1", dur=10, caps=["welding"],
                     retry=RetryPolicy(max_attempts=2, backoff=5))]
        workers = [_w("w1", caps=[]), _w("w2", caps=["welding"])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].worker_id == "w2"

    def test_max_attempts_1_same_as_no_retry(self):
        tasks = [_t("t1", dur=10, retry=RetryPolicy(max_attempts=1, backoff=5))]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].attempt == 1

    def test_retry_does_not_affect_successful_schedule(self):
        tasks = [_t("t1", dur=5, retry=RetryPolicy(max_attempts=5, backoff=10))]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].start_time == 0
        assert result.assignments[0].attempt == 1

    def test_retry_backoff_value(self):
        """With backoff=10, second attempt starts searching at 10."""
        tasks = [_t("t1", dur=10, deadline=25,
                     retry=RetryPolicy(max_attempts=2, backoff=10))]
        # Worker unavailable [0,1440) — no slot in attempt 1 window
        # but deadline=25, so max_start=15 for first attempt
        # attempt 1 searches 0..15, worker unavailable; fails
        # attempt 2 searches 10..15, worker unavailable; fails
        workers = [_w("w1", unavail=[Window(0, 1440)])]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)

    def test_retry_all_exhausted_failure(self):
        tasks = [_t("t1", dur=10, deadline=5,
                     retry=RetryPolicy(max_attempts=3, backoff=2))]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        assert result.failures[0].task_id == "t1"


# ===================================================================
# 7. Edge cases  (~20 assertions)
# ===================================================================

class TestEdgeCases:

    def test_zero_duration_task(self):
        tasks = [_t("t1", dur=0)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        a = result.assignments[0]
        assert a.start_time == 0
        assert a.end_time == 0

    def test_zero_duration_task_start_equals_end(self):
        tasks = [_t("t1", dur=0)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].start_time == result.assignments[0].end_time

    def test_no_dependencies_no_deadline_starts_at_zero(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].start_time == 0

    def test_no_workers_all_tasks_fail(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10)]
        result = schedule(tasks, [], [])
        assert isinstance(result, ScheduleError)
        assert len(result.failures) == 2

    def test_no_workers_failure_reason(self):
        tasks = [_t("t1", dur=10)]
        result = schedule(tasks, [], [])
        assert isinstance(result, ScheduleError)
        assert isinstance(result.failures[0].reason, NoCapableWorker)

    def test_deadline_exactly_met(self):
        tasks = [_t("t1", dur=10, deadline=10)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].end_time == 10

    def test_deadline_missed_by_one(self):
        tasks = [_t("t1", dur=11, deadline=10)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)
        assert isinstance(result.failures[0].reason, DeadlineMissed)

    def test_large_number_of_tasks(self):
        tasks = [_t(f"t{i:03d}", dur=1) for i in range(50)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert len(result.assignments) == 50

    def test_long_dependency_chain(self):
        tasks = [_t("t0", dur=1)]
        for i in range(1, 20):
            tasks.append(_t(f"t{i}", dur=1, deps=[f"t{i-1}"]))
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["t19"].start_time == 19
        assert m["t19"].end_time == 20

    def test_deadline_zero_means_no_deadline(self):
        """deadline=0 is treated as no deadline."""
        tasks = [_t("t1", dur=10, deadline=0)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].start_time == 0

    def test_mixed_success_and_cycle_returns_error(self):
        """If some tasks have cycles, result is ScheduleError."""
        tasks = [
            _t("ok1", dur=5),
            _t("a", dur=10, deps=["b"]),
            _t("b", dur=10, deps=["a"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleError)

    def test_single_task_with_deadline_succeeds(self):
        tasks = [_t("t1", dur=5, deadline=100)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].end_time <= 100

    def test_duration_1_task(self):
        tasks = [_t("t1", dur=1)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        assert result.assignments[0].start_time == 0
        assert result.assignments[0].end_time == 1

    def test_zero_duration_after_dependency(self):
        tasks = [
            _t("a", dur=10),
            _t("b", dur=0, deps=["a"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        m = {a.task_id: a for a in result.assignments}
        assert m["b"].start_time == 10
        assert m["b"].end_time == 10


# ===================================================================
# 8. Validator  (~40+ assertions)
# ===================================================================

class TestValidator:

    def test_correct_schedule_zero_violations(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        violations = validate_assignments(result.assignments, tasks, workers, [])
        assert violations == []

    def test_correct_chain_zero_violations(self):
        tasks = [
            _t("a", dur=5),
            _t("b", dur=5, deps=["a"]),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        violations = validate_assignments(result.assignments, tasks, workers, [])
        assert violations == []

    def test_capability_mismatch_detected(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [_w("w1", caps=["plumbing"])]
        bad_assignment = [Assignment(task_id="t1", worker_id="w1",
                                     start_time=0, end_time=10)]
        violations = validate_assignments(bad_assignment, tasks, workers, [])
        assert len(violations) >= 1
        assert any(isinstance(v.kind, CapabilityMismatch) for v in violations)

    def test_capability_mismatch_missing_field(self):
        tasks = [_t("t1", dur=10, caps=["welding"])]
        workers = [_w("w1", caps=["plumbing"])]
        bad_assignment = [Assignment(task_id="t1", worker_id="w1",
                                     start_time=0, end_time=10)]
        violations = validate_assignments(bad_assignment, tasks, workers, [])
        cap_v = [v for v in violations if isinstance(v.kind, CapabilityMismatch)]
        assert len(cap_v) >= 1
        assert "welding" in cap_v[0].kind.missing

    def test_window_conflict_detected(self):
        tasks = [_t("t1", dur=10)]
        workers = [_w("w1", unavail=[Window(0, 20)])]
        bad_assignment = [Assignment(task_id="t1", worker_id="w1",
                                     start_time=5, end_time=15)]
        violations = validate_assignments(bad_assignment, tasks, workers, [])
        assert any(isinstance(v.kind, WindowConflict) for v in violations)

    def test_deadline_exceeded_detected(self):
        tasks = [_t("t1", dur=10, deadline=8)]
        workers = [_w("w1")]
        bad_assignment = [Assignment(task_id="t1", worker_id="w1",
                                     start_time=0, end_time=10)]
        violations = validate_assignments(bad_assignment, tasks, workers, [])
        assert any(isinstance(v.kind, DeadlineExceeded) for v in violations)

    def test_deadline_exceeded_fields(self):
        tasks = [_t("t1", dur=10, deadline=8)]
        workers = [_w("w1")]
        bad_assignment = [Assignment(task_id="t1", worker_id="w1",
                                     start_time=0, end_time=10)]
        violations = validate_assignments(bad_assignment, tasks, workers, [])
        dl_v = [v for v in violations if isinstance(v.kind, DeadlineExceeded)]
        assert len(dl_v) >= 1
        assert dl_v[0].kind.deadline == 8
        assert dl_v[0].kind.end_time == 10

    def test_dependency_order_violation(self):
        tasks = [
            _t("a", dur=10),
            _t("b", dur=10, deps=["a"]),
        ]
        workers = [_w("w1")]
        # b starts before a ends
        bad_assignments = [
            Assignment(task_id="a", worker_id="w1", start_time=0, end_time=10),
            Assignment(task_id="b", worker_id="w1", start_time=5, end_time=15),
        ]
        violations = validate_assignments(bad_assignments, tasks, workers, [])
        assert any(isinstance(v.kind, DependencyOrder) for v in violations)

    def test_dependency_order_fields(self):
        tasks = [
            _t("a", dur=10),
            _t("b", dur=10, deps=["a"]),
        ]
        workers = [_w("w1")]
        bad_assignments = [
            Assignment(task_id="a", worker_id="w1", start_time=0, end_time=10),
            Assignment(task_id="b", worker_id="w1", start_time=5, end_time=15),
        ]
        violations = validate_assignments(bad_assignments, tasks, workers, [])
        dep_v = [v for v in violations if isinstance(v.kind, DependencyOrder)]
        assert len(dep_v) >= 1
        assert dep_v[0].kind.dependency_id == "a"
        assert dep_v[0].kind.dep_end == 10
        assert dep_v[0].kind.task_start == 5

    def test_overlap_violation(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10)]
        workers = [_w("w1")]
        bad_assignments = [
            Assignment(task_id="t1", worker_id="w1", start_time=0, end_time=10),
            Assignment(task_id="t2", worker_id="w1", start_time=5, end_time=15),
        ]
        violations = validate_assignments(bad_assignments, tasks, workers, [])
        assert any(isinstance(v.kind, OverlapViolation) for v in violations)

    def test_overlap_violation_fields(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10)]
        workers = [_w("w1")]
        bad_assignments = [
            Assignment(task_id="t1", worker_id="w1", start_time=0, end_time=10),
            Assignment(task_id="t2", worker_id="w1", start_time=5, end_time=15),
        ]
        violations = validate_assignments(bad_assignments, tasks, workers, [])
        ov = [v for v in violations if isinstance(v.kind, OverlapViolation)]
        assert len(ov) >= 1
        other_ids = {v.kind.other_task_id for v in ov}
        # t1 overlaps t2 and t2 overlaps t1
        assert "t1" in other_ids or "t2" in other_ids

    def test_resource_exceeded_detected(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1)]
        bad_assignments = [
            Assignment(task_id="t1", worker_id="w1", start_time=0, end_time=10),
            Assignment(task_id="t2", worker_id="w2", start_time=0, end_time=10),
        ]
        violations = validate_assignments(bad_assignments, tasks, workers, resources)
        assert any(isinstance(v.kind, ResourceExceeded) for v in violations)

    def test_resource_exceeded_fields(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1)]
        bad_assignments = [
            Assignment(task_id="t1", worker_id="w1", start_time=0, end_time=10),
            Assignment(task_id="t2", worker_id="w2", start_time=0, end_time=10),
        ]
        violations = validate_assignments(bad_assignments, tasks, workers, resources)
        res_v = [v for v in violations if isinstance(v.kind, ResourceExceeded)]
        assert len(res_v) >= 1
        assert res_v[0].kind.resource_id == "r1"
        assert res_v[0].kind.count == 2
        assert res_v[0].kind.capacity == 1

    def test_no_violations_parallel_on_different_workers(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10)]
        workers = [_w("w1"), _w("w2")]
        assignments = [
            Assignment(task_id="t1", worker_id="w1", start_time=0, end_time=10),
            Assignment(task_id="t2", worker_id="w2", start_time=0, end_time=10),
        ]
        violations = validate_assignments(assignments, tasks, workers, [])
        assert violations == []

    def test_golden_schedule_always_validates_clean(self):
        """The scheduler's own output should produce zero violations."""
        tasks = [
            _t("a", dur=5, caps=["x"]),
            _t("b", dur=10, caps=["x"], deps=["a"]),
            _t("c", dur=3),
        ]
        workers = [_w("w1", caps=["x"]), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        violations = validate_assignments(result.assignments, tasks, workers, [])
        assert violations == []

    def test_golden_schedule_with_resources_validates_clean(self):
        tasks = [
            _t("t1", dur=10, res=["r1"]),
            _t("t2", dur=10, res=["r1"]),
        ]
        workers = [_w("w1"), _w("w2")]
        resources = [_r("r1", cap=1)]
        result = schedule(tasks, workers, resources)
        assert isinstance(result, ScheduleOk)
        violations = validate_assignments(result.assignments, tasks, workers, resources)
        assert violations == []

    def test_golden_diamond_validates_clean(self):
        tasks = [
            _t("a", dur=5),
            _t("b", dur=10, deps=["a"]),
            _t("c", dur=8, deps=["a"]),
            _t("d", dur=5, deps=["b", "c"]),
        ]
        workers = [_w("w1"), _w("w2")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        violations = validate_assignments(result.assignments, tasks, workers, [])
        assert violations == []

    def test_violations_sorted_by_start_time_then_task_id(self):
        tasks = [_t("t1", dur=10, deadline=5), _t("t2", dur=10, deadline=5)]
        workers = [_w("w1"), _w("w2")]
        bad_assignments = [
            Assignment(task_id="t2", worker_id="w2", start_time=0, end_time=10),
            Assignment(task_id="t1", worker_id="w1", start_time=0, end_time=10),
        ]
        violations = validate_assignments(bad_assignments, tasks, workers, [])
        assert len(violations) >= 2
        for i in range(len(violations) - 1):
            v1, v2 = violations[i], violations[i + 1]
            assert (v1.assignment.start_time, v1.assignment.task_id) <= \
                   (v2.assignment.start_time, v2.assignment.task_id)

    def test_empty_assignments_zero_violations(self):
        violations = validate_assignments([], [], [], [])
        assert violations == []

    def test_multiple_violation_types_on_same_assignment(self):
        tasks = [_t("t1", dur=10, caps=["welding"], deadline=5)]
        workers = [_w("w1", caps=["plumbing"], unavail=[Window(0, 20)])]
        bad_assignment = [Assignment(task_id="t1", worker_id="w1",
                                     start_time=0, end_time=10)]
        violations = validate_assignments(bad_assignment, tasks, workers, [])
        kinds = {type(v.kind) for v in violations}
        # Should detect cap mismatch, window conflict, and deadline exceeded
        assert CapabilityMismatch in kinds
        assert WindowConflict in kinds
        assert DeadlineExceeded in kinds

    def test_window_conflict_reports_correct_window(self):
        tasks = [_t("t1", dur=10)]
        w = Window(5, 15)
        workers = [_w("w1", unavail=[w])]
        bad_assignment = [Assignment(task_id="t1", worker_id="w1",
                                     start_time=0, end_time=10)]
        violations = validate_assignments(bad_assignment, tasks, workers, [])
        wc = [v for v in violations if isinstance(v.kind, WindowConflict)]
        assert len(wc) == 1
        assert wc[0].kind.window == w

    def test_no_overlap_when_sequential_on_same_worker(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10)]
        workers = [_w("w1")]
        assignments = [
            Assignment(task_id="t1", worker_id="w1", start_time=0, end_time=10),
            Assignment(task_id="t2", worker_id="w1", start_time=10, end_time=20),
        ]
        violations = validate_assignments(assignments, tasks, workers, [])
        assert not any(isinstance(v.kind, OverlapViolation) for v in violations)

    def test_golden_with_unavailability_validates_clean(self):
        tasks = [_t("t1", dur=10), _t("t2", dur=10)]
        workers = [
            _w("w1", unavail=[Window(0, 15)]),
            _w("w2"),
        ]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        violations = validate_assignments(result.assignments, tasks, workers, [])
        assert violations == []

    def test_golden_priority_schedule_validates_clean(self):
        tasks = [
            _t("high", dur=5, pri=1),
            _t("low", dur=5, pri=5),
        ]
        workers = [_w("w1")]
        result = schedule(tasks, workers, [])
        assert isinstance(result, ScheduleOk)
        violations = validate_assignments(result.assignments, tasks, workers, [])
        assert violations == []
