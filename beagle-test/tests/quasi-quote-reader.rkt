#lang racket/base

;; Reader-level tests for the quasiquote/unquote/unquote-splicing prefix
;; macros (`` ` ``, `~`, `~@`) installed in beagle-readtable.
;;
;; SURFACE: matches Clojure — unquote is `~` / `~@`, and `,` is WHITESPACE.
;; (Beagle previously had the CL-style `,`=unquote; that silent divergence was
;; removed — P0 #21.) The base (clj/js/odin) readtable uses `~`; the nix
;; readtable keeps `,`=unquote (Racket default) because `~` is taken by nix
;; tilde-strings (`~"…"`) — see the nix-suite below.
;;
;; These tests only exercise the reader — they do not assert any
;; expansion semantics. The wrapper symbols `quasiquote`/`unquote`/
;; `unquote-splicing` are inert at the beagle level until a defmacro
;; consumes them; here we just confirm the shape of the read datum.

(require rackunit
         rackunit/text-ui
         beagle/lang/reader-impl
         beagle/nix/lang/reader-impl)

(define (read-beagle str)
  (beagle-read (open-input-string str)))

(define (read-beagle-nix str)
  (beagle-nix-read (open-input-string str)))

(define base-suite
  (test-suite "beagle base readtable: quasiquote/unquote prefix readers"

    (test-case "`x reads as (quasiquote x)"
      (check-equal? (read-beagle "`x") '(quasiquote x)))

    (test-case "~x reads as (unquote x)"
      (check-equal? (read-beagle "~x") '(unquote x)))

    (test-case "~@xs reads as (unquote-splicing xs)"
      (check-equal? (read-beagle "~@xs") '(unquote-splicing xs)))

    (test-case "`(a ~b c) reads as (quasiquote (a (unquote b) c))"
      (check-equal? (read-beagle "`(a ~b c)")
                    '(quasiquote (a (unquote b) c))))

    (test-case "`(a ~@bs c) reads as (quasiquote (a (unquote-splicing bs) c))"
      (check-equal? (read-beagle "`(a ~@bs c)")
                    '(quasiquote (a (unquote-splicing bs) c))))

    (test-case "nested ``(a ~~b c) reads as (quasiquote (quasiquote (a (unquote (unquote b)) c)))"
      (check-equal? (read-beagle "``(a ~~b c)")
                    '(quasiquote (quasiquote (a (unquote (unquote b)) c)))))

    (test-case "`[a ~b] reads with beagle bracket semantics (#%brackets ...)"
      (check-equal? (read-beagle "`[a ~b]")
                    '(quasiquote (#%brackets a (unquote b)))))

    (test-case "`{:k ~v} reads with beagle map semantics (#%map ...)"
      (check-equal? (read-beagle "`{:k ~v}")
                    '(quasiquote (#%map :k (unquote v)))))

    (test-case "~(string->symbol s) reads through nested parens"
      (check-equal? (read-beagle "~(string->symbol s)")
                    '(unquote (string->symbol s))))

    (test-case "~@(map f xs) reads through nested parens"
      (check-equal? (read-beagle "~@(map f xs)")
                    '(unquote-splicing (map f xs))))

    ;; `,` is Clojure WHITESPACE (ignored), not unquote.
    (test-case "comma is whitespace: (a , b) reads as (a b)"
      (check-equal? (read-beagle "(a , b)") '(a b)))

    (test-case "comma is whitespace inside quasiquote: `(a ~b, c) reads as (quasiquote (a (unquote b) c))"
      (check-equal? (read-beagle "`(a ~b, c)")
                    '(quasiquote (a (unquote b) c))))

    (test-case "negative: `~` at EOF errors with a clear message"
      (check-exn
        (lambda (e)
          (and (exn:fail? e)
               (regexp-match? #rx"unquote" (exn-message e))))
        (lambda () (read-beagle "~"))))

    (test-case "negative: ``~@`` at EOF errors with a clear message"
      (check-exn
        (lambda (e)
          (and (exn:fail? e)
               (regexp-match? #rx"unquote-splicing" (exn-message e))))
        (lambda () (read-beagle "~@"))))

    (test-case "negative: `` ` `` at EOF errors with a clear message"
      (check-exn
        (lambda (e)
          (and (exn:fail? e)
               (regexp-match? #rx"quasiquote" (exn-message e))))
        (lambda () (read-beagle "`"))))))

(define nix-suite
  (test-suite "beagle/nix readtable: quasiquote + Clojure-whitespace comma"
    ;; nix inherits the base readtable, so `,` is WHITESPACE here too (coherent
    ;; with clj/js/odin). `~` is the nix TILDE-STRING reader (not unquote), so
    ;; quasiquote-unquote on nix is an OPEN question — see beagle-nix-readtable.

    (test-case "`x reads as (quasiquote x) under beagle-nix-readtable"
      (check-equal? (read-beagle-nix "`x") '(quasiquote x)))

    (test-case "comma is whitespace under beagle-nix-readtable: (a , b) -> (a b)"
      (check-equal? (read-beagle-nix "(a , b)") '(a b)))

    (test-case "`(a , b) reads with comma as whitespace (no unquote on nix)"
      (check-equal? (read-beagle-nix "`(a , b)")
                    '(quasiquote (a b))))))

(module+ test
  (run-tests base-suite)
  (run-tests nix-suite))
