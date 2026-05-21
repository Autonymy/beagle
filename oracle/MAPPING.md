# Beagle → Typed Racket Type Mapping

Validated by P0 spike: 5 hand-emitted programs, all accepted by `raco make`.

## Primitives

| Beagle | Typed Racket | Notes |
|--------|-------------|-------|
| `String` | `String` | direct |
| `Int` | `Integer` | not `Int` (Typed Racket's `Integer` is arbitrary precision) |
| `Float` | `Flonum` | IEEE 754 double |
| `Bool` | `Boolean` | direct |
| `Nil` | `False` | Beagle `nil` maps to `#f` |
| `Any` | `Any` | escape hatch |
| `Keyword` | `Symbol` | Racket has no keyword type; use symbols |
| `Symbol` | `Symbol` | direct |

## Nullable / Option

| Beagle | Typed Racket | Notes |
|--------|-------------|-------|
| `T?` / `(Option T)` | `(Option T)` = `(U T #f)` | Beagle nil → Racket `#f` |
| `nil?` check | `(not x)` | occurrence typing narrows `(Option T)` to `T` in else branch |

## Collections

| Beagle | Typed Racket | Notes |
|--------|-------------|-------|
| `(Vec T)` | `(Listof T)` | immutable linked list; closest semantic match |
| `(Map K V)` | `(HashTable K V)` | immutable hash |
| `(Set T)` | `(Setof T)` | immutable set |
| `(List T)` | `(Listof T)` | same as Vec for oracle purposes |

## Composite Types

| Beagle | Typed Racket | Spike |
|--------|-------------|-------|
| `defrecord` | `(struct Name ([f : T] ...) #:transparent)` | 01 |
| `defunion` | `(define-type Name (U Variant ...))` + one struct per variant | 02 |
| `defscalar` | `(struct Name ([v : T]) #:transparent)` | 05 |
| `defenum` | `(define-type Name (U 'a 'b ...))` | — (literal symbols) |

## Parametric Types

| Beagle | Typed Racket | Spike |
|--------|-------------|-------|
| `(defunion (Result T E) ...)` | `(struct (T) Ok ...)` + `(define-type (Result T E) (U ...))` | 03 |
| `forall` | `(All (T) ...)` | 03 |
| `(forall (T <: Bound) ...)` | `(All (T) ...)` | bounds not expressible; erase to unbounded |

## Function Types

| Beagle | Typed Racket | Notes |
|--------|-------------|-------|
| `(-> A B C)` | `(-> A B C)` | direct |
| `(: name (-> ...))` | `(: name (-> ...))` | top-level annotation |
| multi-arity `defn` | `(case-> ...)` | — |
| rest params | `T *` in `->` | `(->* (A) () #:rest T T)` or `(-> A T * T)` |

## Flow-Sensitive Narrowing

| Beagle pattern | Typed Racket mechanism | Spike |
|---------------|----------------------|-------|
| `(nil? x)` in `if` | `(not x)` — occurrence typing narrows `(Option T)` | 04 |
| `(Circle? x)` in `cond` | struct predicate — narrows `(U Circle Rect)` to `Circle` | 02, 04 |
| `(match x [(Ok v) ...])` | `(cond [(Ok? x) (Ok-value x)] ...)` | 03 |
| let-binding narrowing | same — occurrence typing flows through `define` | 04 |

## Bug Detection Mapping

| E16 bug | Beagle mechanism | Typed Racket mechanism | Spike |
|---------|-----------------|----------------------|-------|
| 04: id swap | `defscalar TaskId String` — newtype | struct wrapper `(struct TaskId ([v : String]))` — nominal distinction | 05 |
| 06: missing match arm | exhaustive match warning (P2) | occurrence typing: `cond` without else on union → `(U T Void)` ≠ `T` | 05 |

## Semantic Gaps (known)

| Beagle feature | Issue | Mitigation |
|---------------|-------|------------|
| `defscalar` bounds | Typed Racket structs don't carry value constraints | Wrap + contract (runtime), or accept as bucket D |
| `Keyword` type | No keyword type in Typed Racket | Map to `Symbol` |
| `unsafe` blocks | No equivalent | Emit `(require/typed ...)` or bucket E |
| Clojure interop (`.-field`, `.method`) | No Clojure runtime | Oracle subset excludes interop |
| `async`/`await` | Racket has futures/places, not JS-style promises | Bucket E for now |
| `defprotocol` / `extend-type` | Racket has interfaces, but structure differs | Emit struct + generic |
