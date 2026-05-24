# Surface debt — demoted-tier test failures

This file is the work-in queue for the next reconciliation pass.
Entries are appended automatically (by `bin/beagle-test`) when a
demoted-tier test fails after a surface change.

**Reconciliation trigger:** post-Cyclone-self-host + surface stable.
At that moment, work through entries in this file: re-run each demoted
test, rewrite/update it for the current surface, promote the fixed
suite back to active tier (in `beagle-test/tiers.rktd`).

## Total debt: 0 failures across 0 entries

(Counter line above is read by `bin/beagle-test` to surface accumulated
debt in runner output. Format: `## Total debt: N failures across M entries`.
Do not change the line format without updating the runner.)

---

## Entry template

Each entry follows this shape so reconciliation isn't archaeology:

```markdown
## YYYY-MM-DD — short description of surface change

**Surface change:** what was added/removed/changed (one paragraph).

**Demoted-tier failures:**

| Target | Test file | Test name | Was checking |
|---|---|---|---|
| clj | emit-clj-behavioral.rkt | "extend-type on defrecord" | That extend-type emission produces a defrecord with a working method body that returns the expected formatted string |
| js  | emit-js-behavioral.rkt   | "doseq iterates over vec"  | That doseq over a Vec produces .forEach call that prints each element in order |

**Reconciliation guidance:** any notes the next-person needs (e.g.
"these tests should be rewritten to use form X" or "verify the
runtime behavior is unchanged before declaring tests fixed").
```

The "Was checking" column is the load-bearing field — it captures
what behavior the test verified at debt-creation time, so the
reconciler can rewrite the test for the new surface without having
to reverse-engineer intent from the failing test code.

---

## Entries

<!-- Append new entries below, most recent first. -->
