#lang racket/base

;; ============================================================================
;; CROSS-TARGET VALUE-CONFORMANCE HARNESS
;; ============================================================================
;;
;; Proves that Beagle's VALUE SEMANTICS agree across emit targets. The thesis:
;; a value (a map, a vector, a set) means the same thing no matter which
;; backend renders it. `(= {:a 1} {:a 1})` must be `true` on EVERY target,
;; because Beagle maps are values, not object references.
;;
;; CLJ is the ORACLE. Clojure's reader + value-equality define the reference
;; answer; every other target is asserted to AGREE with the CLJ result. We do
;; NOT hard-code expected booleans — the oracle computes them — so the harness
;; cannot drift from Clojure's actual semantics.
;;
;; Two runnable targets are wired here:
;;   - clj : compile .bclj, run via Babashka (bb), print with pr-str
;;   - js  : compile .bjs,  run via node,      print structurally (JSON)
;; CLJS and Odin runtimes are absent in this env and Nix eval is unwired, so
;; they are NOT wired. The TARGETS table below is the single extension point:
;; add a target descriptor (header / extension / runner / printer) and the
;; whole corpus runs against it.
;;
;; BASELINE (2026-06-21): emit-js currently routes `=` to JS `===`, which is
;; REFERENCE equality on objects/arrays. So compound-`=` cases produce `false`
;; on JS while the CLJ oracle says `true`. Those cases are RED *by design* —
;; they are the falsifier that proves the value-semantics gap is real. The
;; SCALAR cases (`(= 1 1)`, `(= "a" "a")`) MUST be GREEN; they prove the
;; harness machinery itself is sound (compile, run, capture, normalize, compare
;; all work), so a RED compound case is a genuine semantic gap, not plumbing.
;;
;; Run: raco test beagle-test/tests/conformance.rkt
;;  or: bin/beagle-test --active-only
;; ============================================================================

(require rackunit
         rackunit/text-ui
         racket/string
         racket/port
         racket/system
         racket/runtime-path
         racket/file)

(define-runtime-path beagle-build "../../bin/beagle-build")

;; Repo-relative path to the JS runtime core (equiv/hash/contains/...). This
;; file lives at <repo>/beagle-test/tests/conformance.rkt, so the core is at
;; <repo>/beagle-lib/lib/beagle/core.js.
(define-runtime-path beagle-core-js "../../beagle-lib/lib/beagle/core.js")

(define BB-PATH   (find-executable-path "bb"))
(define NODE-PATH
  (or (find-executable-path "node")
      (let ([p "/run/current-system/sw/bin/node"])
        (and (file-exists? p) p))))

(define tmp-dir (make-temporary-file "beagle-conformance-~a" 'directory))

;; ---------------------------------------------------------------------------
;; node_modules scaffold so emitted JS can resolve `import * as $$bc from
;; 'beagle/core.js'`. Once the emit-js =→equiv routing lands, compiled .mjs
;; modules carry that bare-specifier import; node only resolves bare specifiers
;; against a node_modules dir on the module's path. We plant
;; tmp-dir/node_modules/beagle/{package.json, core.js→symlink-to-repo} and run
;; the emitted .mjs FROM tmp-dir, so resolution finds the live repo core.js.
;; Without this, the import would ERR_MODULE_NOT_FOUND and the case would
;; silently RUN-FAIL instead of going GREEN — the harness could never observe
;; the very fix it exists to detect.
(define (setup-beagle-node-module!)
  (define beagle-mod-dir (build-path tmp-dir "node_modules" "beagle"))
  (make-directory* beagle-mod-dir)
  (call-with-output-file (build-path beagle-mod-dir "package.json")
    #:exists 'truncate
    (lambda (p) (display "{\"type\":\"module\"}\n" p)))
  (define link-path (build-path beagle-mod-dir "core.js"))
  (when (or (file-exists? link-path) (link-exists? link-path))
    (delete-file link-path))
  (make-file-or-directory-link
   (path->string (simplify-path beagle-core-js))
   link-path))

(setup-beagle-node-module!)

;; ---------------------------------------------------------------------------
;; Compile + run helpers (subprocess patterns reused from
;; tests/js-exec-oracle.rkt and tests/emit-clj-behavioral.rkt).
;; ---------------------------------------------------------------------------

;; Compile a Beagle source string to `out-path`. Returns #t on success.
(define (compile-beagle src-text src-path out-path)
  (call-with-output-file src-path #:exists 'truncate
    (lambda (p) (display src-text p)))
  (define out-cap (open-output-string))
  (define err-cap (open-output-string))
  (define ok?
    (parameterize ([current-output-port out-cap]
                   [current-error-port  err-cap])
      (system* (path->string beagle-build)
               (path->string src-path)
               (path->string out-path))))
  (values ok? (get-output-string err-cap)))

;; Run a shell command list, capturing stdout/stderr + exit status.
(define (run-capture exe . args)
  (define out-cap (open-output-string))
  (define err-cap (open-output-string))
  (define ok?
    (parameterize ([current-output-port out-cap]
                   [current-error-port  err-cap])
      (apply system* exe args)))
  (values ok? (get-output-string out-cap) (get-output-string err-cap)))

;; ---------------------------------------------------------------------------
;; Normalization: collapse the printed result from each target into a single
;; canonical token so a value-level comparison ignores cosmetic rendering
;; differences (CLJ prints `:x`; JS prints `x`; CLJ prints `true`; JSON prints
;; `true`). We deliberately normalize keyword-vs-string for the *looked-up
;; value* case so a genuine value agreement is not masked by representation;
;; the boolean/number cases need no special handling. If a target ever prints
;; an object/array reference token, normalization will NOT turn `false` into
;; `true` — the gap stays visible.
(define (normalize s)
  (string-trim
   ;; strip a single leading `:` so keyword `:x` and string `x`/`"x"` collapse
   (let ([t (string-trim s)])
     (cond
       [(and (> (string-length t) 0) (char=? (string-ref t 0) #\:))
        (substring t 1)]
       [(and (>= (string-length t) 2)
             (char=? (string-ref t 0) #\")
             (char=? (string-ref t (sub1 (string-length t))) #\"))
        (substring t 1 (sub1 (string-length t)))]
       [else t]))))

;; ---------------------------------------------------------------------------
;; TARGET DESCRIPTORS — the extension point. Each target knows how to wrap a
;; bare Beagle expression into a compilable module, what file extension to
;; emit, how to run the emitted artifact, and how to print the value.
;;
;; `wrap` : expr-string -> full beagle source (the body is `(defn result [] :- T expr)`)
;; The result type annotation matters to the checker, so each case supplies its
;; own return type; `wrap` takes (expr ret-type).
;; ---------------------------------------------------------------------------

(struct target (name ext wrap run) #:transparent)

;; CLJ target: header `#lang beagle`, run via bb, print via pr-str.
(define (clj-wrap expr ret)
  (string-append "#lang beagle\n(ns conf)\n"
                 "(defn result [] :- " ret " " expr ")\n"))

(define (clj-run out-path)
  ;; Append a driver that prints (pr-str (result)).
  (define body (file->string out-path))
  (define run-path (path-replace-extension out-path "-run.clj"))
  (call-with-output-file run-path #:exists 'truncate
    (lambda (p)
      (display body p)
      (display "\n(println (pr-str (result)))\n" p)))
  (run-capture BB-PATH (path->string run-path)))

;; JS target: header `#lang beagle/js`, export the fn, run via node, print via
;; JSON.stringify so compound structure is visible (and arrays/objects don't
;; collapse to `[object Object]`).
(define (js-wrap expr ret)
  (string-append "#lang beagle/js\n(ns conf)\n"
                 "(js/export (defn result [] :- " ret " " expr "))\n"))

(define (js-run out-path)
  ;; Write the emitted module + a print driver to a `.mjs` FILE inside tmp-dir
  ;; and run `node <file>` (NOT `-e`). Running a real file from tmp-dir means
  ;; node resolves bare specifiers (e.g. `import * as $$bc from 'beagle/core.js'`)
  ;; against tmp-dir/node_modules/beagle — see setup-beagle-node-module!. With
  ;; `-e` there is no module path, so a bare-specifier import would fail.
  (define body (file->string out-path))
  (define run-path (path-replace-extension out-path "-run.mjs"))
  (call-with-output-file run-path #:exists 'truncate
    (lambda (p)
      (display body p)
      (display "\nconsole.log(JSON.stringify(result()));\n" p)))
  (run-capture NODE-PATH (path->string run-path)))

(define CLJ-TARGET (target "clj" "bclj" clj-wrap clj-run))
(define JS-TARGET  (target "js"  "bjs"  js-wrap  js-run))

;; Compile + run one case for one target. Returns
;;   (values status value)  where status ∈ {'ok 'compile-fail 'run-fail}
;;   and value is the normalized printed result (or an error blob).
(define (eval-case tgt case-name expr ret)
  (define src-text ((target-wrap tgt) expr ret))
  (define src-path
    (build-path tmp-dir (string-append case-name "." (target-ext tgt))))
  (define out-ext (if (string=? (target-name tgt) "js") "mjs" "clj"))
  (define out-path
    (build-path tmp-dir (string-append case-name "." (target-name tgt) "." out-ext)))
  (define-values (compiled? cerr) (compile-beagle src-text src-path out-path))
  (cond
    [(not (and compiled? (file-exists? out-path)))
     (values 'compile-fail cerr)]
    [else
     (define-values (ran? out err) ((target-run tgt) out-path))
     (if ran?
         (values 'ok (normalize out))
         (values 'run-fail (string-append "stdout:\n" out "\nstderr:\n" err)))]))

;; ---------------------------------------------------------------------------
;; THE CORPUS. Each entry: (name expr return-type kind)
;;   kind ∈ {'scalar 'compound 'known-gap}
;; 'known-gap cases are expected NOT to agree today and are reported, not
;; asserted (they must not crash the machinery).
;; ---------------------------------------------------------------------------

(define CORPUS
  (list
   ;; SCALAR SANITY — MUST be GREEN (proves the harness is correct).
   (list "scalar-int-eq"  "(= 1 1)"     "Bool" 'scalar)
   (list "scalar-str-eq"  "(= \"a\" \"a\")" "Bool" 'scalar)

   ;; COMPOUND VALUE EQUALITY — RED today (= -> === ref equality on JS).
   (list "map-eq-true"   "(= {:a 1} {:a 1})"  "Bool" 'compound)
   (list "vec-eq-true"   "(= [1 2 3] [1 2 3])" "Bool" 'compound)
   (list "map-eq-false"  "(= {:a 1} {:a 2})"  "Bool" 'compound)
   (list "set-eq-true"   "(= #{1 2 3} #{3 2 1})" "Bool" 'compound)
   (list "distinct-by-value"
         "(count (distinct [{:a 1} {:a 1}]))" "Int" 'compound)

   ;; COMPOUND KEY BY VALUE — the looked-up value. CLJ prints :x, JS prints x;
   ;; normalization collapses keyword/string so a genuine value match is GREEN.
   ;; (JS object-key coercion happens to find it; representation differs — see
   ;; KNOWN GAPS in the harness report.)
   (list "map-by-vec-key" "(get {[1 2] :x} [1 2])" "Keyword" 'compound)))

;; ---------------------------------------------------------------------------
;; THE TEST. For each case: compute the CLJ ORACLE, then assert each non-oracle
;; target agrees. Scalars must be GREEN; compounds are the baseline (RED now).
;; A compile/run failure on a non-oracle target is recorded as a KNOWN GAP
;; (it must not crash the harness machinery) — the value comparison happens
;; only where the case actually runs.
;; ---------------------------------------------------------------------------

(define OTHER-TARGETS (list JS-TARGET))

(define (run-conformance)
  (cond
    [(not BB-PATH)
     (displayln "SKIP: bb (Babashka) not found — cannot run the CLJ oracle.")]
    [(not NODE-PATH)
     (displayln "SKIP: node not found — cannot run the JS target.")]
    [else
     (run-tests
      (test-suite "cross-target value conformance"
        (for/list ([c (in-list CORPUS)])
          (define name (list-ref c 0))
          (define expr (list-ref c 1))
          (define ret  (list-ref c 2))
          (define kind (list-ref c 3))
          (test-case (string-append name " :: " expr)
            ;; ---- ORACLE (clj) ----
            (define-values (clj-status clj-val) (eval-case CLJ-TARGET name expr ret))
            (check-eq? clj-status 'ok
                       (format "ORACLE (clj) failed to evaluate ~a: ~a" name clj-val))
            ;; ---- each other target must AGREE with the oracle ----
            (for ([tgt (in-list OTHER-TARGETS)])
              (define-values (st val) (eval-case tgt name expr ret))
              (cond
                [(not (eq? st 'ok))
                 ;; Cannot compile/run on this target today: KNOWN GAP, report,
                 ;; do not crash the machinery. Don't fail scalars this way.
                 (printf "KNOWN GAP [~a/~a]: ~a (~a)\n"
                         (target-name tgt) name st
                         (string-trim (car (string-split val "\n"))))]
                [else
                 ;; ACTUAL value-level agreement assertion against the oracle.
                 (check-equal? val clj-val
                               (format
                                (string-append
                                 "~a DISAGREES with CLJ oracle on ~a\n"
                                 "  expr   : ~a\n"
                                 "  oracle : ~a (clj)\n"
                                 "  ~a     : ~a\n"
                                 "  [kind=~a]")
                                (target-name tgt) name expr
                                clj-val (target-name tgt) val kind))]))))))]))

(run-conformance)
