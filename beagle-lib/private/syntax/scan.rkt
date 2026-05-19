#lang racket/base

(require "tokenize.rkt")

(provide
 (struct-out scan-problem)
 (struct-out scan-result)
 scan-delimiters)

(struct scan-problem (type opener closer) #:transparent)
;; type: 'mismatch | 'extra-closer | 'unclosed
;; opener: token or #f
;; closer: token or #f

(struct scan-result (problems stack) #:transparent)
;; problems: (listof scan-problem), in source order
;; stack: final delimiter stack (innermost first), empty when balanced

(define (scan-delimiters tokens)
  (define stack '())
  (define problems '())

  (for ([tok (in-list tokens)])
    (cond
      [(opener? tok)
       (set! stack (cons tok stack))]

      [(closer? tok)
       (define valid-openers (openers-for-closer (token-type tok)))
       (cond
         [(null? stack)
          (set! problems (cons (scan-problem 'extra-closer #f tok) problems))]

         [(memq (token-type (car stack)) valid-openers)
          (set! stack (cdr stack))]

         [else
          (set! problems
            (cons (scan-problem 'mismatch (car stack) tok) problems))
          (set! stack (cdr stack))])]))

  (define unclosed
    (for/list ([tok (in-list stack)])
      (scan-problem 'unclosed tok #f)))

  (scan-result (append (reverse problems) unclosed) stack))
