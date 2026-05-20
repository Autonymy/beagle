# Bug 06: Missing match arm for AttemptFailure (missing case)

**File:** scheduler.bgl, line 257-261
**Category:** Missing case
**Checker catches:** P2+ (exhaustive match on ScheduleAttempt)

**Change:** Remove the AttemptFailure arm from the match in process-single-task.
The match only handles AttemptSuccess, leaving AttemptFailure unhandled.

**Effect:** When scheduling fails for a task, the match throws instead of
recording the failure. This means partial failures crash the scheduler.
