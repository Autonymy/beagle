;; Beagle test tier manifest.
;;
;; Three tiers:
;;
;;   active  — blocks iteration. Active failures fail the build.
;;   demoted — runs continuously but advisory only; doesn't block.
;;   gated   — opt-in only via env var (BEAGLE_ORACLE=1, etc). Runner
;;             treats as "not run this session" rather than pass/fail.
;;
;; --- structural-floor rule ---
;;
;; All emit-*.rkt STRUCTURAL tests stay active regardless of target status.
;; They catch "this surface change broke an entire emitter" before that
;; breakage rots invisibly. Only -behavioral.rkt tests for non-load-bearing
;; targets are demoted. The floor is cheap to maintain (no external
;; interpreter runs) and high-value (immediate visibility on entire-emitter
;; breakage).
;;
;; --- promotion criteria ---
;;
;; Demoted → active requires BOTH:
;;   (a) the surface is stable enough that reconciliation work won't be
;;       re-done immediately, AND
;;   (b) the target is load-bearing for actual work (real use case, not
;;       hypothetical optionality).
;;
;; Just (a) is not enough — keeping a target's behavioral suite current
;; costs ongoing maintenance, and that cost is only worth paying when (b)
;; says someone actually depends on the runtime correctness. A target that
;; never becomes load-bearing stays demoted indefinitely, and that is
;; correct: optionality is preserved (emitter code exists, structural tests
;; pass) at low cost (no behavioral maintenance).
;;
;; --- per-target tier summary (human-readable navigation) ---
;;
;; NOTE: this is a SUMMARY VIEW for navigation. The authoritative tier
;; assignment is the file-level list below — the runner reads from there.
;; "split" below is shorthand for "structural-active + behavioral-demoted";
;; the runner does not know about a "split" tier.

#hasheq(
  (nix     . (active   "Load-bearing via bnix dogfood (firnos config + heist work)"))
  (rkt     . (active   "Oracle target — Typed Racket validates Beagle's type promises via raco make; all current tests are structural"))
  (clj     . (split    "Structural active (floor rule); behavioral demoted — Clojure was the bootstrap target; no current load-bearing use; reactivate when a Clojure-backed app ships"))
  (cljs    . (split    "Structural active; behavioral demoted — same rationale as clj; CLJS-specific tests live inside emit-clj-behavioral suite for now"))
  (js      . (split    "Structural active; behavioral demoted — JS target may become load-bearing via Bun work; currently aspirational"))
  (py      . (split    "Structural active; behavioral demoted — Python target is recent; no load-bearing use yet"))
  (sql     . (active   "Structural-only; no behavioral runner exists yet so nothing to demote"))
  (cyclone . (future   "Cyclone Scheme self-host target — not yet implemented. When it ships, its behavioral tests promote to active (self-host means Cyclone is the substrate Beagle runs on)")))


;; --- authoritative file-level classification ---
;;
;; One-time pass at manifest creation; do not trust filename convention
;; exhaustively. Edit this list directly when promoting/demoting suites.

#hasheq(
  (active . (;; target-agnostic infrastructure
             "check.rkt"
             "lint.rkt"
             "parse.rkt"
             "syntax.rkt"
             "test-tags.rkt"
             "types.rkt"
             ;; structural emit tests (the structural-floor rule)
             "emit.rkt"                ; emit-clj structural
             "emit-js.rkt"
             "emit-nix.rkt"
             "emit-py.rkt"
             "emit-rkt.rkt"
             "emit-sql.rkt"
             ;; typed JS surface (jst-*) — structural only
             "js-fixtures.rkt"
             "js-quote.rkt"
             "jst.rkt"
             ;; Python structural fixtures
             "py-fixtures.rkt"
             ;; Nix (load-bearing target)
             "nix-emit-errors.rkt"
             "nix-lints.rkt"
             "nix-parse.rkt"
             "nix-roundtrip.rkt"
             "validate-nix.rkt"
             ;; SQL (structural only; no behavioral runner exists)
             "sql-fixtures.rkt"
             "sql-roundtrip.rkt"
             "sql-schema-cache.rkt"))

  (demoted . (;; behavioral runs that hit external interpreters
              "emit-clj-behavioral.rkt"  ; requires bb (Babashka)
              "emit-js-behavioral.rkt")) ; requires bun

  (gated . (;; opt-in via env var
            "differential.rkt"          ; BEAGLE_ORACLE=1
            "js-exec-oracle.rkt"        ; requires node/bun at runtime
            "nix-property.rkt"          ; BEAGLE_NIX_EVAL_CHECK=1
            "oracle.rkt"                ; BEAGLE_ORACLE=1
            "oracle-bun.rkt"            ; BEAGLE_ORACLE=1
            "py-exec-oracle.rkt")))     ; requires python at runtime
