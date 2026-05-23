# 004 — Records and cross-module imports

**Date:** 2026-05-15 afternoon  
**Commits:** `fe9523e`, `53db2f9`

## Problem

Single-file beagle programs were well-typed, but real systems have
multiple modules. When module A defines `(defrecord Order ...)` and
module B does `(require A :as ord)`, the type checker in B had no
knowledge of Order's fields, constructor signature, or accessor types.

This made beagle equivalent to Clojure for cross-module code —
the exact scenario where type checking should help most.

## Solution

`import-module-types!` in parse.rkt resolves required modules at compile
time and imports:
- Record type names (so `Order` resolves as a type)
- Constructors `->Order` with typed signatures
- Accessors `order-field` with `[Order -> FieldType]`
- Keyword-access field types for `(:field order)` inference

Both qualified (`ord/order-total`) and unqualified names are validated
at call sites.

## Impact

This was the prerequisite for all scaled experiments (E3+). Without it,
beagle couldn't check the most common source of bugs in multi-module
systems: wrong accessor on a cross-module record.
