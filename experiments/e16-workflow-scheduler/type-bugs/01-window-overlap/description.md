# Bug 01: Window overlap boundary (off-by-one)

**File:** matcher.bgl, line 47
**Category:** Off-by-one
**Checker catches:** No (both `<` and `<=` type-check fine)

**Change:** `(and (< start1 end2) (< start2 end1))` → `(and (<= start1 end2) (< start2 end1))`

**Effect:** Makes windows that share an exact boundary (e.g., [0,10) and [10,20))
register as overlapping when they shouldn't. This causes workers to appear
unavailable when they're actually free, and tasks to be incorrectly serialized.
