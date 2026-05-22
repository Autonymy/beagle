---
status: active
priority: 1
---

# Emit targets — gaps, correctness, coverage

## Nix: multi-arity must fail loudly

- [x] `emit-nix.rkt`: multi-arity `defn` errors at emit time (both Racket + Bun emitters).

## Clojure emitter: test backfill

- [x] Create emit-clj-behavioral.rkt — 51 end-to-end tests (compile → bb → verify output).
      Covers: def, defn, defrecord, defunion, let, destructuring, cond/if/when/case,
      loop/recur, for/doseq/dotimes, try/catch, threading, higher-order, atoms, letfn,
      defmulti/defmethod, collections, strings.
- [x] defenum keyword emission fix (emitted symbols instead of keywords).
- [x] defmethod return type annotation leak fix.
- [ ] Expand: add multi-module tests (ns + require round-trip via bb classpath).
- [ ] Expand: add defprotocol/deftype/extend-type behavioral tests.

## Oracle CI

- [ ] Oracle CI integration — raco make cross-check on Bun compiler output
      (moved from self-hosting.md)

## New targets

- [ ] `beagle/elixir`
- [ ] `beagle/bash`

## SQL gaps

- [ ] Parameterized queries (bind params, not string interpolation)
- [ ] Dialect testing (Postgres, MySQL round-trip — only SQLite validated)
- [ ] Transactions (BEGIN/COMMIT/ROLLBACK)
- [ ] UPSERT / ON CONFLICT
- [ ] Views (CREATE VIEW, SELECT from views)
- [ ] Derived tables (subquery in FROM)
- [ ] Schema migrations (versioned DDL with up/down)

## Racket target gaps

- [ ] `defenum` keyword-as-member (checker rejects)
- [ ] `count` on strings
- [ ] `Int/Int` division → Exact-Rational
- [ ] defscalar/collection display format differs between targets

## JS target

- [ ] `js-template` — typed splice sites
- [ ] `js/quote` — structural JS quasiquotation

## Misc

- [ ] E13 confound isolation: full prompt vs cheatsheet, daemon vs no daemon
- [ ] Stale `.zo` files across agents (race conditions with PostToolUse hook)
