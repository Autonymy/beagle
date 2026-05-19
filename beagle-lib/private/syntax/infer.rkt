#lang racket/base

(require racket/list
         "tokenize.rkt")

(provide
 (struct-out closer-insertion)
 (struct-out line-info)
 build-line-info
 infer-closer-positions)

(struct closer-insertion (offset line col closer-text opener-token confidence) #:transparent)
;; Where to insert a closer and what type. offset is char position in source.

(struct line-info (number indent last-content-offset has-content?) #:transparent)
;; number: 1-based line number
;; indent: column of first non-ws token (-1 if blank/comment-only)
;; last-content-offset: char offset just past the last non-ws/non-comment token
;; has-content?: #t if line has non-ws non-comment tokens

(define (build-line-info tokens)
  (define table (make-hash)) ; line-number -> mutable line-info builder
  (define max-line 0)

  (for ([tok (in-list tokens)])
    (define ln (token-line tok))
    (define ty (token-type tok))
    (when (> ln max-line) (set! max-line ln))

    (unless (hash-has-key? table ln)
      (hash-set! table ln (vector -1 0 #f))) ; [indent, last-offset, has-content?]

    (define v (hash-ref table ln))

    (when (and (not (memq ty '(whitespace newline line-comment block-comment)))
               (not (vector-ref v 2)))
      (vector-set! v 0 (token-col tok))
      (vector-set! v 2 #t))

    (when (not (memq ty '(whitespace newline line-comment block-comment)))
      (vector-set! v 1 (+ (token-offset tok) (string-length (token-text tok))))))

  (for/vector ([ln (in-range 1 (add1 max-line))])
    (define v (hash-ref table ln (vector -1 0 #f)))
    (line-info ln (vector-ref v 0) (vector-ref v 1) (vector-ref v 2))))

(define (infer-closer-positions tokens unclosed-openers source-length)
  (when (null? unclosed-openers) (list))

  (define lines (build-line-info tokens))
  (define num-lines (vector-length lines))

  (define (get-line n)
    (and (>= n 1) (<= n num-lines) (vector-ref lines (sub1 n))))

  (define (last-content-line-in-scope opener-line opener-col)
    (define result #f)
    (for ([i (in-range opener-line num-lines)])
      (define li (vector-ref lines i))
      (when (and (line-info-has-content? li)
                 (> (line-info-indent li) opener-col))
        (set! result li)))
    result)

  (define (all-unclosed-contiguous? openers)
    (or (<= (length openers) 1)
        (let ([lines (map (lambda (tok) (token-line tok)) openers)])
          (define min-l (apply min lines))
          (define max-l (apply max lines))
          (<= (- max-l min-l) 3))))

  ;; Process innermost first (unclosed-openers is already stack order: innermost first)
  (define insertions '())

  (for ([opener (in-list unclosed-openers)])
    (define opener-line (token-line opener))
    (define opener-col (token-col opener))
    (define closer-type (matching-closer-type (token-type opener)))
    (define text (closer-text closer-type))

    (define target-line (last-content-line-in-scope opener-line opener-col))

    (cond
      [target-line
       (set! insertions
         (cons (closer-insertion
                (line-info-last-content-offset target-line)
                (line-info-number target-line)
                -1 ; col unknown at this point
                text
                opener
                (if (= (line-info-number target-line) num-lines) 'high 'medium))
               insertions))]
      [else
       (define opener-li (get-line opener-line))
       (define fallback-offset
         (if (and opener-li (line-info-has-content? opener-li))
             (line-info-last-content-offset opener-li)
             source-length))
       (set! insertions
         (cons (closer-insertion
                fallback-offset
                opener-line
                -1
                text
                opener
                'high)
               insertions))]))

  (reverse insertions))
