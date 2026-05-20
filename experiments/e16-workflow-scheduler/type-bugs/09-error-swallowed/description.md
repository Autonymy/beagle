# Bug 09: Error swallowed — returns Ok on failure (error handling)

**File:** scheduler.bgl, line 323
**Category:** Error handling
**Checker catches:** No (both ScheduleOk and ScheduleError are valid ScheduleResult)

**Change:** In the schedule function, always return ScheduleOk regardless of failures.
Change `(if (empty? final-failures)` to `(if true`

**Effect:** Even when tasks fail to schedule, the function returns ScheduleOk
with whatever assignments it managed. Failures are silently dropped.
