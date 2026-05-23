#lang racket/base
;; One-time export: stdlib type catalog → JSON for the standalone compiler.

(require json
         racket/hash
         "../beagle-lib/private/stdlib-types.rkt"
         "../beagle-lib/private/ast-json.rkt")

(define (export-target target)
  (define h (stdlib-for-target target))
  (for/hasheq ([(k v) (in-hash h)])
    (values k (type->json v))))

(define out
  (hasheq 'js  (export-target 'js)
          'clj (export-target 'clj)
          'py  (export-target 'py)
          'nix (export-target 'nix)
          'rkt (export-target 'rkt)))

(write-json out)
(newline)
