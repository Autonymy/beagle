#lang racket/base

(require "tokenize.rkt" racket/string)

(provide
 (struct-out event-entry)
 (struct-out delim-counts)
 (struct-out ledger-result)
 build-event-ledger)

(struct event-entry
  (line col
   depth-before depth-after
   kind char stack-display
   error? detail context)
  #:transparent)

(struct delim-counts
  (open-parens close-parens
   open-brackets close-brackets
   open-braces close-braces)
  #:transparent)

(struct ledger-result (events counts valid?) #:transparent)

(define (build-event-ledger source)
  (define tokens (tokenize source))
  (define source-lines (string-split source "\n" #:trim? #f))
  (define num-lines (length source-lines))

  (define (context-at line col)
    (if (and (>= line 1) (<= line num-lines))
        (let* ([ln (list-ref source-lines (sub1 line))]
               [len (string-length ln)]
               [start (max 0 (- col 10))]
               [end (min len (+ col 30))]
               [pre (if (> start 0) "…" "")]
               [post (if (< end len) "…" "")]
               [snippet (if (<= start end) (substring ln start end) "")])
          (string-append pre snippet post))
        ""))

  (define stack '())
  (define events '())
  (define has-error? #f)
  (define op 0) (define cp 0)
  (define ob 0) (define cb 0)
  (define oc 0) (define cc 0)

  (define (stack-str)
    (define items (map token-text (reverse stack)))
    (define s (apply string-append items))
    (if (> (string-length s) 14)
        (string-append "…" (substring s (- (string-length s) 14)))
        s))

  (for ([tok (in-list tokens)])
    (cond
      [(opener? tok)
       (define db (length stack))
       (set! stack (cons tok stack))
       (case (token-type tok)
         [(open-paren) (set! op (add1 op))]
         [(open-bracket) (set! ob (add1 ob))]
         [(open-brace hash-open-brace) (set! oc (add1 oc))])
       (set! events
         (cons (event-entry (token-line tok) (token-col tok)
                            db (add1 db)
                            'open (token-text tok) (stack-str)
                            #f #f
                            (context-at (token-line tok) (token-col tok)))
               events))]

      [(closer? tok)
       (define db (length stack))
       (define valid (openers-for-closer (token-type tok)))
       (case (token-type tok)
         [(close-paren) (set! cp (add1 cp))]
         [(close-bracket) (set! cb (add1 cb))]
         [(close-brace) (set! cc (add1 cc))])
       (cond
         [(null? stack)
          (set! has-error? #t)
          (set! events
            (cons (event-entry (token-line tok) (token-col tok)
                               db db
                               'extra-closer (token-text tok) ""
                               #t
                               (format "unmatched ~a — nothing to close"
                                       (token-text tok))
                               (context-at (token-line tok) (token-col tok)))
                  events))]
         [(memq (token-type (car stack)) valid)
          (set! stack (cdr stack))
          (set! events
            (cons (event-entry (token-line tok) (token-col tok)
                               db (length stack)
                               'close (token-text tok) (stack-str)
                               #f #f
                               (context-at (token-line tok) (token-col tok)))
                  events))]
         [else
          (set! has-error? #t)
          (define opener (car stack))
          (define expected
            (closer-text (matching-closer-type (token-type opener))))
          (set! stack (cdr stack))
          (set! events
            (cons (event-entry (token-line tok) (token-col tok)
                               db (length stack)
                               'mismatch (token-text tok) (stack-str)
                               #t
                               (format "expected ~a to close ~a at ~a:~a"
                                       expected (token-text opener)
                                       (token-line opener) (token-col opener))
                               (context-at (token-line tok) (token-col tok)))
                  events))])]))

  (when (not (null? stack)) (set! has-error? #t))

  (define unclosed-events
    (for/list ([tok (in-list (reverse stack))])
      (event-entry (token-line tok) (token-col tok)
                   0 0
                   'unclosed (token-text tok) ""
                   #t
                   (format "unclosed ~a" (token-text tok))
                   (context-at (token-line tok) (token-col tok)))))

  (ledger-result
   (append (reverse events) unclosed-events)
   (delim-counts op cp ob cb oc cc)
   (and (not has-error?) (null? stack))))
