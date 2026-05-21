# E16 Bug Format

Each bug is a directory under `bugs/<bug_id>/`.

## Directory structure

```
bugs/
  <bug_id>/
    description.md    # What this bug does
    beagle.patch      # Unified diff against golden/beagle/
    python.patch      # Unified diff against golden/python/
```

Not every bug needs all patches. A bug may only target one track
if the relevant code path does not exist in the other.

## Patch format

Patches are **unified diffs** generated against the golden code:

```bash
# Generate a patch:
diff -ruN golden/beagle/ buggy/beagle/ > bugs/<bug_id>/beagle.patch

# Or using git:
cd golden/beagle && git diff > ../../bugs/<bug_id>/beagle.patch
```

Patches are applied with `patch -p1` from inside the workspace directory.

## description.md format

```markdown
# <Bug ID>

**Category**: off-by-one | logic-inversion | missing-guard | wrong-operator | ...

**What it changes**: One-sentence description of the code mutation.

**Expected oracle impact**: How many tests should fail and in which
test groups (e.g., "~7 failures in dependency graph and edge case tests").
```

## Bug categories

| Category           | Description                                    |
|--------------------|------------------------------------------------|
| off-by-one         | Loop bounds, array indices, boundary checks    |
| logic-inversion    | Flipped condition (< vs >=, and vs or)         |
| missing-guard      | Removed an early-return or null/empty check    |
| wrong-operator     | Arithmetic or comparison operator swap         |
| dropped-field      | Missing field in record construction           |
| reorder            | Swapped order of operations that matters       |
| type-narrowing     | Changed a type that breaks downstream logic    |
