#lang racket/base

;; Nix string emission — escaping + interp + multiline + indented.
;; Calls back into emit-expr via current-emit-expr (set by emit-nix.rkt).

(require racket/string
         racket/format
         "parse.rkt")

(provide escape-nix
         current-emit-expr
         emit-nix-interp-string
         emit-nix-interp-string-inline
         emit-nix-multiline-string
         emit-nix-indented-string)

(define current-emit-expr (make-parameter #f))

(define (emit-expr* e depth)
  (define f (current-emit-expr))
  (unless f (error 'emit-nix-strings "current-emit-expr not set"))
  (f e depth))

(define (indent n) (make-string (* 2 n) #\space))

;; Single source of truth for Nix string escaping.
;; #:multiline? — produce ''…'' string semantics (escapes are different from "…")
;; #:keep-interp? — do NOT escape bare ${ (caller wants the rendered string
;;                  to retain `${X}` as a real Nix interpolation marker).
;;                  Only `emit-key` uses this — for plain-string map keys that
;;                  the user authored with `${X}` to get Nix computed-key
;;                  evaluation. Interpolated-string chunks (`(s …)` / `(ms …)`)
;;                  do NOT pass this flag: their interpolation is expressed
;;                  structurally via expression parts, so bare `${X}` in their
;;                  literal string chunks is the user's literal text and must
;;                  be escaped.
;;
;; Beagle's bnix convention: `$${` in a literal string chunk means
;; "literal `${` in the rendered output." This is independent of
;; keep-interp? and ALWAYS gets translated to the Nix-specific escape
;; (`\${` in "…", `''${` in ''…''). Translation is done through a
;; placeholder so the resulting `\${` / `''${` isn't re-matched by the
;; bare-`${` pass that runs after it.
(define LIT-DOLLAR-PLACEHOLDER "LITDOLLAR")

(define (escape-nix s
                    #:multiline? [multiline? #f]
                    #:keep-interp? [keep-interp? #f])
  (cond
    [multiline?
     (let* ([s (regexp-replace* #rx"''" s "'''")]
            [s (regexp-replace* #rx"\\$\\$\\{" s LIT-DOLLAR-PLACEHOLDER)]
            [s (if keep-interp? s (regexp-replace* #rx"\\$\\{" s "''${"))]
            [s (regexp-replace* (regexp-quote LIT-DOLLAR-PLACEHOLDER) s "''${")])
       s)]
    [else
     (let* ([s (regexp-replace* #rx"\\\\" s "\\\\\\\\")]
            [s (regexp-replace* #rx"\n" s "\\\\n")]
            [s (regexp-replace* #rx"\"" s "\\\\\"")]
            [s (regexp-replace* #rx"\\$\\$\\{" s LIT-DOLLAR-PLACEHOLDER)]
            [s (if keep-interp? s (regexp-replace* #rx"\\$\\{" s "\\\\${"))]
            [s (regexp-replace* (regexp-quote LIT-DOLLAR-PLACEHOLDER) s "\\\\${")])
       s)]))

(define (emit-nix-interp-string-inline parts depth)
  (define chunks
    (for/list ([part (in-list parts)])
      (cond
        [(string? part) (escape-nix #:multiline? #t part)]
        [else (format "${~a}" (emit-expr* part depth))])))
  (string-join chunks ""))

(define (emit-nix-interp-string parts depth)
  (define chunks
    (for/list ([part (in-list parts)])
      (cond
        [(string? part) (escape-nix part)]
        [else (format "${~a}" (emit-expr* part depth))])))
  (format "\"~a\"" (string-join chunks "")))

(define (emit-nix-multiline-string lines depth)
  (define ind (indent (+ depth 1)))
  ;; Each operand of (ms …) is one logical line. Operands joined with \n so
  ;; multi-operand forms produce one physical Nix line per operand. A string
  ;; operand may still contain its own embedded \n (legacy single-operand
  ;; form, e.g. (ms "[default]\nkey =")) — the join + re-split treats those
  ;; as additional physical lines uniformly.
  (define body
    (string-join
      (for/list ([line (in-list lines)])
        (cond
          [(string? line)
           (escape-nix #:multiline? #t line)]
          [(nix-interpolated-string? line)
           (emit-nix-interp-string-inline
            (nix-interpolated-string-parts line) depth)]
          [else (format "${~a}" (emit-expr* line depth))]))
      "\n"))
  (define phys-lines (regexp-split #rx"\n" body))
  (define indented
    (for/list ([l (in-list phys-lines)])
      (if (string=? l "") "" (string-append ind l))))
  (string-append
   "''\n"
   (string-join indented "\n")
   "\n" (indent depth) "''"))

(define (emit-nix-indented-string text depth #:escape? [escape? #t])
  (define ind (indent (+ depth 1)))
  (define lines (regexp-split #rx"\n" text))
  (define (process-line l)
    (if (string=? l "") ""
        (string-append ind (if escape? (escape-nix #:multiline? #t l) l))))
  (string-append
   "''\n"
   (string-join (map process-line lines) "\n")
   "\n" (indent depth) "''"))
