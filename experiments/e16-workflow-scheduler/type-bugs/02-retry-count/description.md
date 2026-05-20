# Bug 02: Retry count fence error (off-by-one)

**File:** scheduler.bgl, line 184
**Category:** Off-by-one
**Checker catches:** No

**Change:** `(> attempt max-attempts)` → `(>= attempt max-attempts)`

**Effect:** Stops retrying one attempt too early. A task with max_attempts=3
will only get 2 attempts instead of 3.
