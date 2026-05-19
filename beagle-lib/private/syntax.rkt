#lang racket/base

;; Beagle structural repair pass 0 — public API.
;;
;; repair-structure : auto-fix delimiter structure before parse
;; check-structure  : validate without modifying
;; structure-edits  : return edits without applying
;; parse-cst / structural patch ops : tree-level editing for agents

(require "syntax/tokenize.rkt"
         "syntax/scan.rkt"
         "syntax/infer.rkt"
         "syntax/repair.rkt"
         "syntax/diagnostics.rkt"
         "syntax/cst.rkt"
         "syntax/patch.rkt"
         "syntax/ledger.rkt")

(provide
 ;; --- Structural repair pass 0 ---
 repair-structure
 check-structure
 structure-edits

 ;; --- Result types ---
 (struct-out repair-result)
 (struct-out check-result)
 (struct-out repair-edit)
 (struct-out structural-diagnostic)

 ;; --- Event ledger ---
 (struct-out event-entry)
 (struct-out delim-counts)
 (struct-out ledger-result)
 build-event-ledger

 ;; --- CST ---
 parse-cst
 cst->string
 cst-content-children
 cst-find-path
 cst-find-by-line
 cst-find-defn

 ;; --- Structural patch ---
 cst-replace-form
 cst-insert-form-after
 cst-delete-form
 cst-wrap-form
 cst-rename-symbol
 cst-add-binding
 cst-add-map-entry)

;; ---------------------------------------------------------------------------
;; Pass 0: structural repair
;; ---------------------------------------------------------------------------

(define (repair-structure source)
  (repair source))

(define (check-structure source)
  (define tokens (tokenize source))
  (define result (scan-delimiters tokens))
  (define problems (scan-result-problems result))
  (check-result
   (null? problems)
   (for/list ([p (in-list problems)])
     (define tok (or (scan-problem-closer p) (scan-problem-opener p)))
     (define tok2 (or (scan-problem-opener p) (scan-problem-closer p)))
     (structural-diagnostic
      'error
      (token-line tok) (token-col tok)
      (token-line tok) (+ (token-col tok) (string-length (token-text tok)))
      (case (scan-problem-type p)
        [(mismatch)
         (format "~a does not match ~a at ~a:~a"
                 (token-text (scan-problem-closer p))
                 (token-text (scan-problem-opener p))
                 (token-line (scan-problem-opener p))
                 (token-col (scan-problem-opener p)))]
        [(extra-closer)
         (format "unmatched ~a" (token-text (scan-problem-closer p)))]
        [(unclosed)
         (format "unclosed ~a at ~a:~a"
                 (token-text (scan-problem-opener p))
                 (token-line (scan-problem-opener p))
                 (token-col (scan-problem-opener p)))])
      (hasheq 'type (symbol->string (scan-problem-type p)))))))

(define (structure-edits source)
  (define result (repair-structure source))
  (repair-result-edits result))

;; ---------------------------------------------------------------------------
;; CST
;; ---------------------------------------------------------------------------

(define (parse-cst source)
  (build-cst (tokenize source)))
