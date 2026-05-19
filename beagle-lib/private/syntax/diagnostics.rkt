#lang racket/base

(provide
 (struct-out repair-edit)
 (struct-out structural-diagnostic)
 (struct-out repair-result)
 (struct-out check-result))

(struct repair-edit
  (offset length insert-text line col reason)
  #:transparent)
;; offset: char position in original string
;; length: chars to remove (0 = pure insert)
;; insert-text: text to place (empty = pure delete)
;; line/col: 1-based, for reporting
;; reason: human-readable

(struct structural-diagnostic
  (severity line col end-line end-col message data)
  #:transparent)
;; severity: 'error 'warning 'info
;; data: free-form hash for machine consumption

(struct repair-result
  (output changed? edits confidence diagnostics)
  #:transparent)
;; output: repaired source string
;; changed?: boolean
;; edits: (listof repair-edit)
;; confidence: 'high | 'medium | 'low
;; diagnostics: (listof structural-diagnostic) — non-empty only when confidence is low

(struct check-result
  (valid? diagnostics)
  #:transparent)
