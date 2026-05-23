# 024 — Surface friction observation (Day 0 of surface redesign)

Authoring representative tasks under the current surface, recording
friction live. The point is data: where does the agent (me) hesitate,
backtrack, or write something it has to fix? Those are the redesign
targets.

Format per task:
- **Task**: what I'm authoring
- **Wrote**: the code I produced
- **Friction**: every moment of "wait, which form?" or "this didn't
  type-check / didn't read as I expected"
- **Notes**: anything generalizable

---

## Task 1 — Typed function with records

**Task**: Define an `Account` record with `holder` (String) and `balance`
(Float). Write a `deposit` function that returns a new account with
balance increased.

**Wrote**:

```
(defrecord Account [(holder : String) (balance : Float)])

(defn deposit [(a : Account) (amount : Float)] : Account
  (Account (account-holder a) (+ (account-balance a) amount)))
```

**Friction**:

1. **Record constructor**: I hesitated between `(Account ...)` and
   `(->Account ...)`. Beagle accepts both — Clojure-style `->Account`
   and bare `Account`. Two idioms, one concept. *Drop one.*

2. **Field access**: I wrote `(account-holder a)` (auto-generated
   accessor name). Alternatives I considered: `(:holder a)` (keyword
   access) and `(.-holder a)` (JS-interop-style). Three idioms for
   "get field from record." *Pick one canonical.*

3. **Record update**: I avoided `with` (the record-update form) and
   instead rebuilt with the constructor. Why? Because I wasn't sure
   `with` worked on the Cyclone target yet (we hit this earlier in the
   session). Even when authoring for Clojure target where I know `with`
   works, I find myself second-guessing because the form's syntax is
   slightly different in different docs. *`with` is a real form for a
   real concept (functional update); decide on its canonical shape.*

4. **The `defn` parameter bracket**: I wrote `[(a : Account) (amount :
   Float)]`. The bracket+paren+colon shape took a moment to assemble.
   Alternatives considered: `[a : Account, amount : Float]` (comma-
   separated like Python/TypeScript) or `(a : Account amount : Float)`
   (flat). The current shape is unambiguous but verbose. *Probably
   keep, but capture rationale.*

**Notes**:
- Three accessor idioms (`(field-name r)`, `(:field r)`, `(.-field r)`)
  is the clearest "too many idioms for one concept" finding in this
  task. The agent reaches for whichever it last saw. Pick one.
- Constructor: bare `Name` vs `->Name` — same problem. Drop `->Name`,
  it's Clojure cosmetic.

---

## Task 2 — Pattern match on defunion

**Task**: Define `Shape` defunion with `Circle [r]`, `Square [s]`,
`Rect [w h]`. Write `area` function using pattern match.

**Wrote**:

```
(defunion Shape
  (Circle [(r : Float)])
  (Square [(s : Float)])
  (Rect   [(w : Float) (h : Float)]))

(defn area [(s : Shape)] : Float
  (match s
    [(Circle r) (* 3.14159 (* r r))]
    [(Square w) (* w w)]
    [(Rect w h) (* w h)]))
```

**Friction**:

1. **Variant declaration bracket**: I wrote `(Circle [(r : Float)])`.
   The paren-around-bracket nesting feels noisy for the simple "one
   typed field" case. Considered: `(Circle (r : Float))` (no inner
   bracket) or `(Circle r : Float)` (inline). The current shape is
   consistent with `defn` params but it's heavy for single-field
   variants. *Trade-off: consistency vs concision. The principle says
   consistency wins.*

2. **Match clause bracket**: `[(Circle r) (* r r)]` — the outer
   `[pattern body]` uses bracket, the pattern uses paren. Same
   bracket-vs-paren question. I had to remember which is which. *Two
   bracket conventions in the same expression. Decide explicitly.*

3. **Wildcard**: I would've reached for `_` if I needed a wildcard.
   Does match support it? Does it bind, or just discard? When I tried
   `(Circle _)` earlier in the session it worked. Not friction now but
   *capture the answer in the spec*.

4. **No exhaustiveness warning?**: The current type checker warns on
   non-exhaustive match. But the wording when I've seen it isn't always
   crisp about *which* variant is missing. *Failure-mode capture.*

**Notes**:
- The match form itself is solid. The friction is at the boundary
  between match's `[]` and the pattern's `()`.
- Single-field variants are common; the current `(Name [(field : T)])`
  shape is heavyweight for them.

---

## Task 3 — List processing pipeline (threading vs let chain vs for)

**Task**: Given a vec of integers, return the sum of squares of the
even ones.

**Wrote** (attempt 1):

```
(defn sum-even-squares [(xs : (Vec Int))] : Int
  (sum (for [x xs :when (= 0 (mod x 2))] (* x x))))
```

**Then I thought**: could've used threading:

```
(defn sum-even-squares [(xs : (Vec Int))] : Int
  (->> xs
       (filter (fn [x] (= 0 (mod x 2))))
       (map (fn [x] (* x x)))
       sum))
```

**Or let chain**:

```
(defn sum-even-squares [(xs : (Vec Int))] : Int
  (let [evens (filter (fn [x] (= 0 (mod x 2))) xs)
        squared (map (fn [x] (* x x)) evens)]
    (sum squared)))
```

**Friction** (this is the big one):

1. **Three valid ways** to express "filter, map, sum." I picked the
   first (`for` with `:when`) but the threading version is more
   idiomatic in Clojure tradition, and the let chain is more idiomatic
   in Scheme tradition. *Which is canonical? The current surface has
   no opinion.* This is exactly the "one canonical idiom per concept"
   failure mode.

2. **`for` with `:when` vs `filter` + `map`**: the `for` version
   reads as one operation; the `filter+map` version reads as two. For
   the type checker they're equivalent. *Pick one.*

3. **Threading macro choice**: `->>` (last-arg) vs `->` (first-arg). I
   knew to use `->>` because `filter`/`map` take the collection last in
   Clojure-style. But the agent has to remember which functions take
   the collection where. *This is a hallucination opportunity.*

4. **Anonymous functions**: `(fn [x] (= 0 (mod x 2)))` is verbose for
   a predicate. Clojure has `#(...)` shorthand (we explicitly rejected
   that — see CLAUDE.md). I'd reach for a lambda combinator like
   `(even? x)` if `even?` were in the stdlib. *Is it? Check.*

**Notes**:
- This task surfaced the **deepest** friction in the audit so far. Three
  valid forms for "process a sequence" is too many. The principles
  demand picking one.
- Strong candidate for canonical: `for` with `:when` for the
  filter+map+reduce pattern when it's small enough; explicit `let`
  chain when steps are larger and warrant naming. Drop threading
  macros as a general-purpose pipeline tool.
- That's a big call. Worth empirical verification before committing.

---

## Task 4 — Recursive algorithm (loop/recur vs named-let)

**Task**: Sum a vec of integers using tail recursion (don't use `sum`).

**Wrote**:

```
(defn sum-vec [(xs : (Vec Int))] : Int
  (loop [i 0
         acc 0]
    (if (>= i (count xs))
      acc
      (recur (+ i 1) (+ acc (nth xs i))))))
```

**Friction**:

1. **`loop`/`recur` vs named-let**: I reached for `loop`/`recur`
   reflexively because that's Clojure. The relayed message earlier
   already pointed out this is pure sugar over named-let. Confirmed
   live: I picked `loop`/`recur` without thinking, which means an
   agent with similar training would also pick reflexively without
   evaluating the alternative. *Drop confirmed.*

2. **`count xs`**: the bounds-check requires getting the length. The
   alternative is to recurse on the vec itself (destructure
   first/rest). Both work. The `count`+`nth` style is index-based; the
   destructure style is list-style. Vec is an array; indexing is
   natural. List would be different. *The agent has to know whether the
   collection is array-indexable.*

3. **`nth xs i`**: I considered `(get xs i)` and `(xs i)` (function-
   call style on vec). Three ways to index. *Pick one.*

4. **`>=`**: a tiny one — when I needed "out of bounds," I wrote
   `(>= i (count xs))`. Could be `(= i (count xs))` since `i` only
   increments by 1 — but `>=` is safer if the recursion ever changes.
   Not friction, just noting that the agent has to choose.

**Notes**:
- `loop`/`recur` was the smoking gun. The fact that I reached for it
  without considering named-let confirms the cleanup verdict.
- Three indexing idioms is a smaller but real "too many idioms" hit.

---

## Task 5 — Conditional pipeline (cond->? threading? if chain?)

**Task**: Given a config map, build an HTTP request: add auth header if
`:token` is set; add user-agent if `:agent` is set; add JSON
Content-Type if `:json?` is true.

**Wrote** (struggled):

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

**Friction** (this one was bad):

1. **This is what `cond->` is for.** Clojure tradition has `cond->`
   exactly to express "conditionally accumulate transformations on a
   value." But I avoided it because — honestly — I'm not 100% sure of
   the syntax under stress. The fact that I avoided a form I know
   exists is a strong friction signal. *Either commit to the form
   (and document it well enough that I don't have to remember) or
   drop it.*

2. **Map construction**: `(mp)` for empty map? Beagle uses `{:k v}`
   for map literals but how do I express "empty map"? `{}` works in
   the reader but what about in code generation contexts? *Friction.*

3. **`get` with default**: `(get config :token nil)` — wait, is `nil`
   the right default to indicate "missing"? Or should I be using
   `contains?`? *Two idioms for "is the key present?"*

4. **`assoc` returns new map**: works because beagle uses immutable
   maps. But the agent has to know that `assoc` isn't a side-effect
   (some languages mutate). *Capture as "non-feature" — assoc does
   NOT mutate.*

5. **The let-chain is ugly.** 4 bindings to express 3 conditional
   additions. A more canonical form would compress this. Either
   `cond->` (if we keep it) or a new "accumulate transformations"
   primitive.

**Notes**:
- `cond->` was designed for exactly this and I avoided it. That's a
  strong signal: either make `cond->` so canonical and well-documented
  that agents reach for it confidently, or drop it and accept the
  let-chain pattern.
- Map literal vs `mp` constructor — at minimum a documentation issue,
  possibly a design issue.

---

## Interim findings (after 5 tasks)

The friction pattern is consistent. The biggest hits are:

### Where the surface has too many idioms

- **Record field access** — 3 forms (`(field-name r)`, `(:field r)`,
  `(.-field r)`). Need 1.
- **Sequence processing** — 3 forms (for-comprehension, threading,
  let-chain). Need 1 canonical with clear use-cases for others.
- **Index access on vec** — 3 forms (`nth`, `get`, function-call).
  Need 1.
- **Constructor invocation** — 2 forms (`Name`, `->Name`). Need 1.
- **Threading macros** — 5 forms (`->`, `->>`, `as->`, `cond->`,
  `some->`). Likely drop most.
- **Conditional accumulator** — `cond->` exists but is avoided under
  stress, suggesting either better docs or a different form.

### Where the surface is doing the right thing

- `match` (despite the bracket/paren boundary friction)
- `defrecord` / `defunion` / `defenum` etc. (beagle-original forms,
  earning their place)
- Vector / map / set / keyword literals
- Typed `defn` shape
- `for` with `:when` (when the right tool)

### Where the surface needs more rigor

- "Single-field variant" syntax in defunion is heavy
- Match's bracket-around-paren-pattern shape is a small but consistent
  re-think moment

### Where the agent reached reflexively (data point for redesign)

- `loop`/`recur` (pulled, even though named-let would do)
- `->>` for filter+map+reduce (one of 3 valid forms)
- `defn` rather than `define + lambda` (correctly, because typed)
- Avoided `cond->` under stress despite it being designed for the case
- Avoided `with` (record update) due to recent target-coupling
  confusion

---

## Task 6 — Polymorphism

**Task**: Define `Printable` protocol with `to-str`. Implement for `Account` and `Shape`.

**Wrote**:

```
(defprotocol Printable
  (to-str [self] : String))

(extend-type Account Printable
  (to-str [self] (str "Account(" (account-holder self) ")")))

(extend-type Circle Printable
  (to-str [self] (str "Circle(r=" (circle-r self) ")")))
```

**Friction**:

1. **`defprotocol`/`extend-type` vs `defmulti`/`defmethod`** — beagle has BOTH. The agent has to pick. Protocols dispatch on first-arg type; multimethods can dispatch on anything. For simple type-based dispatch, both work. *Two idioms, one common case.*
2. **`self` convention** — am I supposed to use `self`, `this`, or just a normal name? *Document the canonical.*
3. **Field access inside method body** — same friction as task 1 (3 idioms).
4. **No return type at the protocol-method-impl level** — declared at protocol, omitted at impl. Easy to misremember and add `: String` to the impl too. *Failure-mode capture: should impls re-declare type or not?*
5. **Cross-type protocol implementation** — works on records I defined. Does it work on defunion variants? On primitives like Int? *Capture in spec.*

**Notes**: Polymorphism story has the most idiom-count noise of any concept. Strong candidate for "audit per use, lean toward dropping the less-used one." Need usage data: how often is `defmulti` actually used vs `defprotocol`?

---

## Task 7 — Macro that generates code

**Task**: Macro `(defrecord-with-builder Name [fields])` that expands to a defrecord + a `make-name-builder` function returning a builder map.

**Wrote** (struggled to remember the proc-macro shape):

```
(define-macro beagle defrecord-with-builder
  [(name : Symbol) (fields : (Vec Form))] : Form
  `(do
     (defrecord ,name ,fields)
     (defn ,(string->symbol (str "make-" (symbol->string name) "-builder")) [] : Any
       {})))
```

**Friction**:

1. **`define-macro beagle` vs `define-macro safe` vs `define-macro unsafe`** — three macro kinds! Which to use for codegen? I picked `beagle` (procedural) but had to think. *Three idioms, one concept (macro). Audit which are actually needed.*
2. **Quasi-quote syntax inside macro body** — `` ` ,name ,fields `` — agent-unfriendly. Has to remember backtick + comma semantics. *Maybe a quote DSL helper would be better.*
3. **`do` form** — I had to wrap the two expansions in `do`. Couldn't expand to multiple top-level forms directly? *Failure-mode capture.*
4. **Type annotations on macro params** — `(name : Symbol) (fields : (Vec Form))` — `Form` is a special macro-only type. *Document where this type comes from.*
5. **String concat for symbol generation** — `(string->symbol (str "make-" (symbol->string name) "-builder"))` is verbose. Clojure has `~(symbol (str ...))` shorthand. Probably should have a `(sym "..." name "...")` helper. *Friction.*

**Notes**: Macro DSL has real friction — quasi-quote is hard to remember under stress, three macro kinds is too many, symbol generation is verbose. This is a strong "needs love" area but probably out of scope for the current redesign (the entire macro story might need its own pass).

---

## Task 8 — Error handling

**Task**: Define `ParseError` with a message field. Function that throws on bad input, caller that catches and returns default.

**Wrote**:

```
(deferror ParseError
  (BadFormat [(msg : String)])
  (Truncated))

(defn parse-int-strict [(s : String)] : Int
  (if (= s "")
    (throw (Truncated))
    (if (= s "abc")  ; placeholder for "can't parse"
      (throw (BadFormat "not a number"))
      42)))

(defn safe-parse [(s : String) (default : Int)] : Int
  (try
    (parse-int-strict s)
    (catch ParseError e default)))
```

**Friction**:

1. **`deferror` vs `defunion`** — deferror is structurally identical to defunion (variants with fields). Why both? Apparently because deferror variants are throwable. *Could be a flag on defunion: `(defunion ParseError #:throwable ... )`. Two forms for ~one concept.*
2. **`throw` vs `raise`** — beagle uses `throw`. Some Lisps use `raise`. *Pick one and stick.*
3. **`(catch ParseError e ...)` — `e` is bound to what?** The thrown value, presumably. But what TYPE? `ParseError` (the union)? Or the specific variant? *Failure-mode capture; bug-prone.*
4. **`catch` clauses inside `try`** — bracket vs paren question recurs.
5. **No `finally`?** — for cleanup code. Does beagle have it? I'd reach for it. *If absent, add or document the alternative.*

**Notes**: deferror could probably be a flag on defunion. The "throw is exception, raise is signal" distinction doesn't apply in beagle — pick one name. catch-binding type is unclear; needs explicit spec.

---

## Task 9 — Cross-module import (selective + alias)

**Task**: Module A defines `square` and `Point`. Module B imports both with selective refer + alias.

**Wrote**:

```
;; module-a.bgl
(ns lib.shapes)
(defrecord Point [(x : Int) (y : Int)])
(defn square [(n : Int)] : Int (* n n))

;; module-b.bgl
(ns app.main)
(require lib.shapes :refer [Point square])
;; or:
(require lib.shapes :as shapes)
;; then use as shapes/Point and shapes/square
```

**Friction**:

1. **`:refer` vs `:as`** — both exist. Which is canonical? Both? *Probably both have use cases (cherry-pick vs whole-module).*
2. **`:refer [...]` syntax** — bracket-list of symbols. Consistent.
3. **`shapes/Point` namespaced reference syntax** — `Module/Name`. The slash collides with division when alias name is short. *Slight friction; document.*
4. **Multi-module require?** — can I do `(require lib.shapes lib.utils)` in one form? Or one require per module? *Unsure; check spec.*
5. **Wildcard refer?** — `:refer :all`? Probably not (anti-pattern), but worth confirming.

**Notes**: Cross-module is mostly fine. The slash-as-namespace-separator is a small friction but probably worth keeping (it's the Clojure-derived convention and works). Minor: confirm one-require-per-module convention.

---

## Task 10 — Side effects (print, timing)

**Task**: Function that prints "starting", does some work, prints "done in N ms".

**Wrote**:

```
(defn timed-work [(n : Int)] : Nil
  (println "starting")
  (let [start (current-time-ms)]  ; ??? not sure of name
    (doseq [i (range n)]
      (println (str "step " (str i))))
    (let [elapsed (- (current-time-ms) start)]
      (println (str "done in " (str elapsed) " ms")))))
```

**Friction**:

1. **`current-time-ms` name** — I guessed. Could be `(System/currentTimeMillis)` (Clojure), `(time)` (Scheme), `(now)`, `(time-ns)`. *Stdlib question — what's beagle's canonical "current monotonic time"?*
2. **`println` vs `print` vs `display`** — Clojure has println, Scheme has display + newline. Beagle has println. Fine. But also has `print`? *Audit.*
3. **`(str x y z)` for concatenation** — works. But why not `(string-append ...)`? Or `(format ...)`? *Three concat idioms possibly.*
4. **`doseq` vs `for`** — `for` returns values, `doseq` is for side effects. I correctly used `doseq` here. But both look similar; agent might confuse. *Capture distinction in spec.*
5. **Type `Nil` for void-returning function** — works but feels heavy. Some languages have `void` or `()`. *Capture.*
6. **`(range n)`** — produces what? A Vec? A List? An iterator? *Type matters for cross-target portability.*

**Notes**: Side-effect orchestration is mostly fine. Timing is the biggest unknown (what's the canonical "now in ms" function?). println/print/display naming and the str-vs-string-append question are minor idiom-count hits.

---

## Findings tally (all 10 tasks)

### High-noise (3+ idioms for one concept) — DROP TO CANONICAL

1. **Record field access**: `(field r)`, `(:field r)`, `(.-field r)` → keep `(field r)` (auto-accessor); drop keyword-as-fn shorthand on records (keep for maps).
2. **Vec indexing**: `nth`, `get`, function-call → keep `(nth v i)` only.
3. **Sequence processing**: `for [:when]`, `(->> filter map)`, let-chain → keep `for` for short; `let`-chain for long; **drop threading macros as default pipeline tool**.
4. **Threading macros**: `->`, `->>`, `as->`, `cond->`, `some->` → keep `->` and `->>` (common two); drop `as->`, `cond->`, `some->`.
5. **Constructor**: `Name`, `->Name` → keep bare `Name`; drop `->Name`.
6. **Recursion**: `loop`/`recur`, named-let → keep canonical `(let loop (...) body)` form; drop `loop`/`recur`.
7. **Sequence stdlib names**: keep `first`/`rest`/`count`/`empty?`/`nil?`; drop `car`/`cdr`/`length`/`null?` aliases.
8. **Increment**: `inc`/`dec` vs `(+ x 1)` → drop `inc`/`dec`; use `+`/`-`.
9. **Negation**: `unless` vs `(when (not c) ...)` → drop `unless`.
10. **Not-equal**: `not=` vs `(not (= a b))` → drop `not=`.
11. **Macro kinds**: `define-macro safe`, `define-macro unsafe`, `define-macro beagle` → audit; likely keep `safe` (default) and `beagle` (procedural); drop `unsafe`.
12. **Throwable union**: `deferror` vs `defunion` → unify as `(defunion Name #:throwable ...)`; deprecate `deferror` form.
13. **Polymorphism**: `defprotocol`/`extend-type` vs `defmulti`/`defmethod` → audit usage; likely keep `defprotocol`+`extend-type` only (multimethods rarely used in practice).

### Medium-noise (2 idioms, both might earn keep) — DOCUMENT

14. **String concat**: `str` vs `string-append` vs `format` → keep `str` (existing canonical); `format` for templates; drop `string-append` alias.
15. **Map access**: `(get m k)`, `(:k m)`, `(.k m)` → keep `(get m k)` for general; keep `(:k m)` for keyword keys; drop `.k` accessor.
16. **for vs doseq**: keep both (semantically distinct — for produces, doseq side-effects); document failure mode.
17. **try/catch bracket convention**: match existing surface; document explicitly.

### Low-noise (idiom is fine; document) — KEEP, RATIONALE

- vector/map/set/keyword literals
- typed defn/def shape
- defrecord, defunion, defenum, defscalar, deftype (beagle-originals)
- match (with explicit bracket-around-paren-pattern note)
- ns/require/declare-extern (cross-module)
- println/print (println prints, print no newline — keep both, document)
- if, cond, case, when, when-let, if-let, if-some (these are distinct enough)

### Needs runtime spec capture

- `current-time-ms` (or equivalent) — what is canonical?
- `catch e` binding type — what's the type of `e`?
- `range` return type — Vec or List?
- macro quasi-quote shape — capture canonical

---

## Day 0 done. Moving to Day 1-2 redesign.

The list above is the redesign input. Phase 1 turns each high-noise item into a spec entry (concept, canonical form, dropped alternatives, failure mode, types, non-features, rationale). Phase 2 implements. Phase 3 migrates corpus.

---

## Process notes for Day 1-2 redesign

After completing the remaining tasks, the redesign sit-down should:

1. Tally idiom-count per concept across all 10 tasks. The top offenders
   get the most attention.
2. For each over-idiomed concept: write the proposed canonical form,
   the rationale, the dropped alternatives, the failure mode for the
   wrong form.
3. Capture the "agent reached reflexively" data — those are the forms
   the canonical surface should privilege (or the forms most likely to
   carry vestigial Clojure-flavor).
4. For forms where the agent avoided a working option under stress
   (like `cond->`), decide: improve the docs/error-messages, or drop.
   Don't keep a form that the canonical user reflexively avoids.
