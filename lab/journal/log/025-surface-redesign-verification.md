# 025 — Surface redesign verification (Day 6-7 of surface redesign)

Re-running representative Day 0 tasks under the new surface to compare
friction. Honest assessment: the changes that landed were narrower
than the Day 0 friction-list suggested, so the verification shows
mixed results — some clear wins, some unchanged areas.

## What changed in the surface (recap)

Dropped (zero or low corpus usage, redundant):
- `defmulti` / `defmethod` (0 real usage)
- `as->` / `cond->` / `cond->>` / `some->` / `some->>` (0 corpus usage)
- `when-not` / `if-not` (2 lines total)
- `inc` / `dec` / `not=` (sugar)

Kept (reversed from initial Day 0 verdict):
- `loop` / `recur` — agent reflexively reaches for it; that's the
  canonical signal, not the redundancy signal
- `->Name` constructor — beagle currently only supports this form;
  bare `(Name args)` isn't implemented, so no redundancy

Untouched (deferred to future pass):
- Record field access (3 idioms: `(field r)`, `(:field r)`, `(.-field r)`)
- Sequence processing (3 idioms: for / threading / let-chain)
- Vec indexing (3 idioms: nth / get / fn-call)
- Threading direction (`->` vs `->>`)
- defprotocol/extend-type story
- Macro DSL (define-macro safe/beagle/unsafe)
- deferror vs defunion #:throwable
- when-let vs when-some vs if-let vs if-some

## Verification: re-running Day 0 tasks

### Task 5 (was: conditional pipeline, biggest friction)

**Original Day 0 attempt** (struggled, picked let-chain after considering
`cond->` and avoiding it):

```
(defn build-request [(config : (Map Keyword Any))] : (Map Keyword String)
  (let [base (mp)
        with-auth (if (get config :token nil) ...)
        with-agent (if (get config :agent nil) ...)
        with-json (if (get config :json? false) ...)]
    with-json))
```

**New surface attempt** (would now be forced to let-chain since
`cond->` is dropped):

```
(defn build-request [(config : (Map Keyword Any))] : (Map Keyword String)
  (let [base (mp)
        with-auth (if (get config :token nil)
                    (assoc base :Authorization (str "Bearer " (get config :token)))
                    base)
        with-agent (if (get config :agent nil)
                     (assoc with-auth :User-Agent (get config :agent))
                     with-auth)
        with-json (if (get config :json? false)
                    (assoc with-agent :Content-Type "application/json")
                    with-agent)]
    with-json))
```

**Friction change**: Zero hesitation about "should I use cond->?"
because cond-> no longer exists. The let-chain IS the canonical
form for this pattern. **Friction reduced** even though the code is
identical in length.

**Verdict**: Win. The principle "one canonical idiom per concept"
worked here — dropping the avoided form removed the cognitive overhead
of "should I be using that other form?"

### Task 4 (was: recursive algorithm with loop/recur)

**Original Day 0 attempt** (reached for loop/recur reflexively):

```
(defn sum-vec [(xs : (Vec Int))] : Int
  (loop [i 0 acc 0]
    (if (>= i (count xs)) acc
        (recur (+ i 1) (+ acc (nth xs i))))))
```

**New surface attempt** (loop/recur kept):

Identical code. No friction change. The hypothesis that "loop/recur
is Clojure-cosplay" was wrong — empirically, it's the canonical form
and the agent reaches for it correctly.

**Verdict**: No change. The Day 0 hypothesis was over-confident; the
new surface preserves what already works.

### Task 8 (was: error handling)

**Original**: `(deferror ParseError ...)` + `try/catch`. Used both.

**New surface**: Same. `deferror` was a candidate for unification into
`defunion #:throwable` but that's deferred. Surface unchanged for this
task.

**Verdict**: No change. Deferred consolidation kept the surface as-is.

### Task 1 (was: record + field access)

**Original**: `(defrecord Account [...])` + `(account-holder a)`.

**New surface**: Identical. The 3-idiom field-access hit (`(field r)`,
`(:field r)`, `(.-field r)`) was NOT addressed in this pass. The
friction it caused on Day 0 persists.

**Verdict**: No change. **Deferred for future pass.**

## Overall: what improved, what didn't

### Improvements (real friction reduction)

1. **Threading macros narrowed from 5 to 2.** Strong win — the 5-way
   choice was real decision surface. Now `->` and `->>` are the only
   options (and they're distinct: first-arg vs last-arg threading).
2. **Conditional accumulator pattern**: no more "should I use cond->?"
   internal debate. The let-chain is canonical.
3. **Negation forms collapsed**: `when-not`/`if-not` removed; agent
   uses `(when (not c) ...)` / `(if (not c) t e)` — explicit, no
   "did I pick the right alias" friction.
4. **Multimethod ambiguity gone**: For type-dispatch, use defprotocol.
   No more "should this be defmulti or defprotocol?"

### Unchanged friction (deferred)

1. **Record field access**: still 3 ways.
2. **Sequence processing**: still 3 ways (for / threading / let-chain).
3. **Vec indexing**: still 3 ways (nth / get / fn-call).
4. **Loop/recur kept (correctly per re-evaluation).**
5. **->Name kept (correctly — no real redundancy).**

### Forms not in this pass but worth examining later

1. **let-family disambiguation** (when-let vs when-some vs if-let vs
   if-some): real but subtle distinctions. Could probably collapse
   when-let/when-some into one form with a `:nil-only?` flag, but
   that's not obviously better.
2. **Macro DSL** (3 kinds: safe / unsafe / beagle): unsafe should
   probably go (per CLAUDE.md "zero escape hatches"), but the macro
   system needs its own dedicated audit.
3. **deferror vs defunion**: structurally identical, fold deferror into
   defunion with a flag.
4. **deftype vs defrecord**: deftype exists for "record-like with
   protocol implementations"; if usage shows it's always paired with
   defrecord + extend-type, the right answer is to combine them.

## Honest read

This pass was conservative — and that's OK. The Day 0 friction list
identified ~13 cleanup targets. After empirical re-evaluation, ~10 of
those were either zero-usage drops or reflexive-canonical keeps. The
deep restructuring (record field access, sequence processing, threading
direction) needs a separate pass because the migration cost is real
and the canonical answer isn't obvious without more data.

What this pass accomplished:
- Removed the bloat that had no defenders (zero-usage forms).
- Removed the small bloat that was pure sugar (inc/dec/not=).
- Validated empirically what's actually canonical (loop/recur).
- Identified what's still bloated and needs future passes.

What this pass deliberately didn't do:
- Restructure record access (would require migrating 100+ files).
- Pick the canonical pipeline form (needs more empirical data on
  for-vs-threading-vs-let usage patterns).
- Touch the polymorphism story (would need a per-form audit).

This is honest. The next surface pass — when it happens — should focus
on those three areas with more empirical observation behind it.

## Cyclone self-host implications

The cleanup didn't change what the Cyclone target needs to handle.
Slightly fewer parser cases to translate. Slightly smaller stdlib.
The architectural insight from the SRFI relay (beagle's stdlib as
the abstraction boundary) is more impactful than the surface drops
for the Cyclone work. That's where the next batch of energy goes.
