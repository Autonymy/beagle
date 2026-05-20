#lang racket/base

;; SQL-specific parse helpers — extracted from parse.rkt.

(require racket/match
         "ast.rkt"
         "types.rkt")

(define (sql-dot-ref? sym)
  (define s (symbol->string sym))
  (define dot-pos (for/or ([i (in-range (string-length s))])
                    (and (char=? (string-ref s i) #\.) i)))
  (and dot-pos (> dot-pos 0) (< dot-pos (- (string-length s) 1))))

(define (parse-sql-column-ref sym)
  (define s (symbol->string sym))
  (define dot-pos (for/or ([i (in-range (string-length s))])
                    (and (char=? (string-ref s i) #\.) i)))
  (sql-column-ref (string->symbol (substring s 0 dot-pos))
                  (string->symbol (substring s (+ dot-pos 1)))))

(define (parse-sql-constraints items)
  (let loop ([rest items] [acc '()])
    (cond
      [(null? rest) (reverse acc)]
      [(and (eq? (car rest) ':default) (pair? (cdr rest)))
       (loop (cddr rest) (cons (list ':default (parse-sql-expr (datum->syntax #f (cadr rest)))) acc))]
      [(and (eq? (car rest) ':references) (pair? (cdr rest)) (pair? (cddr rest)))
       (loop (cdddr rest) (cons (list ':references (cadr rest) (caddr rest)) acc))]
      [(and (eq? (car rest) ':check) (pair? (cdr rest)))
       (loop (cddr rest) (cons (list ':check (parse-sql-expr (datum->syntax #f (cadr rest)))) acc))]
      [else
       (loop (cdr rest) (cons (car rest) acc))])))

(define (parse-sql-columns fields-form)
  (define items (unwrap-items (->datum fields-form) "deftable column list"))
  (for/list ([item (in-list items)])
    (define d (if (syntax? item) (syntax->datum item) item))
    (define col-items (cond
                        [(bracketed? d) (bracket-body d)]
                        [(list? d) d]
                        [else (error 'beagle "deftable: column must be a list, got ~v" d)]))
    (when (< (length col-items) 3)
      (error 'beagle "deftable column requires at least (name : Type), got ~v" col-items))
    (define col-name (car col-items))
    (unless (eq? (cadr col-items) ':)
      (error 'beagle "deftable column ~a: expected : after name" col-name))
    (define col-type (parse-type (caddr col-items)))
    (define constraints (parse-sql-constraints (cdddr col-items)))
    (sql-column col-name col-type constraints)))

(define (parse-sql-column-names cols-form)
  (define d (->datum cols-form))
  (define items (cond
                  [(bracketed? d) (bracket-body d)]
                  [(list? d) d]
                  [else (error 'beagle "insert: expected column name list, got ~v" d)]))
  (for/list ([item (in-list items)])
    (define s (->datum item))
    (unless (symbol? s)
      (error 'beagle "insert: column name must be a symbol, got ~v" s))
    s))

(define (parse-sql-values-row row)
  (define d (->datum row))
  (define items (cond
                  [(bracketed? d) (bracket-body d)]
                  [(list? d) d]
                  [else (error 'beagle "insert values: expected a row vector, got ~v" d)]))
  (map (lambda (item) ((current-parse-expr) (if (syntax? item) item (datum->syntax #f item)))) items))

(define (parse-sql-set-pairs set-form)
  (define d (->datum set-form))
  (define pairs-raw (cdr d))
  (for/list ([pair-raw (in-list pairs-raw)])
    (define pair-d (->datum pair-raw))
    (define items (cond
                    [(bracketed? pair-d) (bracket-body pair-d)]
                    [(list? pair-d) pair-d]
                    [else (error 'beagle "update set: expected [col expr] pair, got ~v" pair-d)]))
    (when (< (length items) 2)
      (error 'beagle "update set: need [column value], got ~v" items))
    (cons (car items)
          ((current-parse-expr) (datum->syntax #f (cadr items))))))

(define (parse-sql-where-clause rest-forms)
  (for/or ([form (in-list rest-forms)])
    (define d (->datum form))
    (and (pair? d) (eq? (car d) 'where)
         (parse-sql-expr (datum->syntax #f (cadr d))))))

(define SQL-KNOWN-FUNCTIONS
  '(count sum avg min max coalesce upper lower trim length
    concat substring replace position left right lpad rpad
    now date_trunc extract age
    abs ceil floor round mod power sqrt
    nullif greatest least
    count-distinct
    row_number rank dense_rank ntile lag lead
    first_value last_value nth_value))

(define (parse-sql-expr stx)
  (define d (->datum stx))
  (cond
    [(and (symbol? d) (sql-dot-ref? d))
     (parse-sql-column-ref d)]
    [(eq? d '*) '*]
    [(symbol? d) d]
    [(string? d) d]
    [(number? d) d]
    [(boolean? d) d]
    [(eq? d 'nil) 'nil]
    [(and (pair? d) (memq (car d) '(= <> > < >= <= + - * / and or not like between in
                                      is-null is-not-null ||)))
     (call-form (car d) (map (lambda (a) (parse-sql-expr (datum->syntax #f a))) (cdr d)))]
    [(and (pair? d) (eq? (car d) 'case))
     (parse-sql-case (cdr d))]
    [(and (pair? d) (eq? (car d) 'cast))
     (when (< (length d) 3)
       (error 'beagle "cast requires (cast expr Type)"))
     (sql-cast (parse-sql-expr (datum->syntax #f (cadr d)))
               (caddr d))]
    [(and (pair? d) (eq? (car d) 'exists))
     (sql-exists (parse-sql-expr (datum->syntax #f (cadr d))))]
    [(and (pair? d) (eq? (car d) 'select))
     (parse-sql-select (cdr d) #f #f)]
    [(and (pair? d) (eq? (car d) 'select-distinct))
     (parse-sql-select (cdr d) #f #t)]
    [(and (pair? d) (memq (car d) SQL-KNOWN-FUNCTIONS))
     (parse-sql-function-call d)]
    [(pair? d)
     (call-form (car d) (map (lambda (a) (parse-sql-expr (datum->syntax #f a))) (cdr d)))]
    [else d]))

(define (parse-sql-function-call d)
  (define fn-name (car d))
  (define rest-items (cdr d))
  (define-values (args alias over-clauses)
    (let loop ([items rest-items] [acc '()] [alias #f] [over #f])
      (cond
        [(null? items) (values (reverse acc) alias over)]
        [(eq? (->datum (car items)) ':as)
         (loop (cddr items) acc (->datum (cadr items)) over)]
        [(eq? (->datum (car items)) ':over)
         (values (reverse acc) alias (cdr items))]
        [else (loop (cdr items) (cons (car items) acc) alias over)])))
  (define parsed-args (map (lambda (a) (parse-sql-expr (datum->syntax #f a))) args))
  (cond
    [over-clauses
     (define-values (partition-by win-order-by)
       (parse-sql-over-clauses over-clauses))
     (sql-window fn-name parsed-args partition-by win-order-by alias)]
    [(memq fn-name '(count sum avg min max count-distinct))
     (sql-aggregate fn-name (if (null? parsed-args) #f (car parsed-args)) alias)]
    [(<= (length parsed-args) 1)
     (sql-aggregate fn-name (if (null? parsed-args) #f (car parsed-args)) alias)]
    [else
     (define cf (call-form fn-name parsed-args))
     (if alias (sql-alias cf alias) cf)]))

(define (parse-sql-over-clauses clauses)
  (define partition-by #f)
  (define order-by #f)
  (for ([c (in-list clauses)])
    (define cd (->datum c))
    (when (pair? cd)
      (case (car cd)
        [(partition-by)
         (set! partition-by (map (lambda (g)
                                   (define gd (->datum g))
                                   (if (and (symbol? gd) (sql-dot-ref? gd))
                                     (parse-sql-column-ref gd)
                                     gd))
                                 (cdr cd)))]
        [(order-by)
         (set! order-by (parse-sql-order-by (cdr cd)))])))
  (values partition-by order-by))

(define (parse-sql-case items)
  (let loop ([rest items] [clauses '()] [else-expr #f])
    (cond
      [(null? rest)
       (sql-case (reverse clauses) else-expr)]
      [(and (pair? rest) (pair? (cdr rest)) (eq? (car rest) ':else))
       (loop '() clauses (parse-sql-expr (datum->syntax #f (cadr rest))))]
      [(and (pair? rest) (pair? (car rest)) (eq? (caar rest) 'when))
       (define clause-d (car rest))
       (define cond-expr (parse-sql-expr (datum->syntax #f (cadr clause-d))))
       (define result-expr (parse-sql-expr (datum->syntax #f (caddr clause-d))))
       (loop (cdr rest) (cons (sql-case-clause cond-expr result-expr) clauses) else-expr)]
      [else
       (error 'beagle "case: expected (when cond result) clauses, got ~v" (car rest))])))

(define (parse-sql-top-form datum)
  (define d (->datum datum))
  (cond
    [(and (pair? d) (eq? (car d) 'select))
     (parse-sql-select (cdr d) #f #f)]
    [(and (pair? d) (eq? (car d) 'select-distinct))
     (parse-sql-select (cdr d) #f #t)]
    [(and (pair? d) (eq? (car d) 'union))
     (sql-union 'union (parse-sql-top-form (cadr d)) (parse-sql-top-form (caddr d)))]
    [(and (pair? d) (eq? (car d) 'union-all))
     (sql-union 'union-all (parse-sql-top-form (cadr d)) (parse-sql-top-form (caddr d)))]
    [(and (pair? d) (eq? (car d) 'intersect))
     (sql-union 'intersect (parse-sql-top-form (cadr d)) (parse-sql-top-form (caddr d)))]
    [(and (pair? d) (eq? (car d) 'except))
     (sql-union 'except (parse-sql-top-form (cadr d)) (parse-sql-top-form (caddr d)))]
    [else (error 'beagle "expected SQL query form (select/union/etc), got ~v" d)]))

(define (parse-sql-with rest)
  (when (null? rest)
    (error 'beagle "with: requires at least one CTE and a body query"))
  (let loop ([items rest] [ctes '()])
    (if (null? (cdr items))
      (sql-with (reverse ctes) (parse-sql-top-form (car items)))
      (let ([d (->datum (car items))])
        (unless (and (pair? d) (symbol? (car d)) (pair? (cdr d)))
          (error 'beagle "with: CTE must be (name query), got ~v" d))
        (loop (cdr items)
              (cons (sql-cte (car d) (parse-sql-top-form (cadr d))) ctes))))))

(define (parse-sql-select-column col-datum)
  (define d (->datum col-datum))
  (cond
    [(eq? d '*) '*]
    [(and (symbol? d) (sql-dot-ref? d))
     (parse-sql-column-ref d)]
    [(symbol? d) d]
    [(and (pair? d) (memq (car d) SQL-KNOWN-FUNCTIONS))
     (parse-sql-function-call d)]
    [else (parse-sql-expr (datum->syntax #f d))]))

(define (parse-sql-select rest subs distinct?)
  (when (null? rest) (error 'beagle "select requires at least a column list"))
  (define cols-form (car rest))
  (define cols-d (->datum cols-form))
  (define col-items (cond
                      [(bracketed? cols-d) (bracket-body cols-d)]
                      [(list? cols-d) cols-d]
                      [else (error 'beagle "select: first argument must be column list, got ~v" cols-d)]))
  (define columns
    (let loop ([items col-items] [acc '()])
      (cond
        [(null? items) (reverse acc)]
        [(and (pair? (cdr items)) (eq? (->datum (cadr items)) ':as) (pair? (cddr items)))
         (define col (parse-sql-select-column (car items)))
         (define alias (->datum (caddr items)))
         (loop (cdddr items) (cons (sql-alias col alias) acc))]
        [else
         (loop (cdr items) (cons (parse-sql-select-column (car items)) acc))])))

  (define clauses (cdr rest))
  (define from-clause #f)
  (define joins '())
  (define where-clause #f)
  (define group-by #f)
  (define having #f)
  (define order-by #f)
  (define limit-val #f)
  (define offset-val #f)

  (for ([clause (in-list clauses)])
    (define cd (->datum clause))
    (when (pair? cd)
      (case (car cd)
        [(from)
         (define table-name (cadr cd))
         (define alias (parse-sql-as-alias (cddr cd)))
         (set! from-clause (if alias (sql-alias table-name alias) table-name))]
        [(join inner-join)
         (define join-info (parse-sql-join-clause 'inner (cdr cd)))
         (set! joins (append joins (list join-info)))]
        [(left-join)
         (define join-info (parse-sql-join-clause 'left (cdr cd)))
         (set! joins (append joins (list join-info)))]
        [(right-join)
         (define join-info (parse-sql-join-clause 'right (cdr cd)))
         (set! joins (append joins (list join-info)))]
        [(full-join)
         (define join-info (parse-sql-join-clause 'full (cdr cd)))
         (set! joins (append joins (list join-info)))]
        [(cross-join)
         (define join-info (parse-sql-join-clause 'cross (cdr cd)))
         (set! joins (append joins (list join-info)))]
        [(where)
         (set! where-clause (parse-sql-expr (datum->syntax #f (cadr cd))))]
        [(group-by)
         (set! group-by (map (lambda (g)
                               (define gd (->datum g))
                               (if (and (symbol? gd) (sql-dot-ref? gd))
                                 (parse-sql-column-ref gd)
                                 gd))
                             (cdr cd)))]
        [(having)
         (set! having (parse-sql-expr (datum->syntax #f (cadr cd))))]
        [(order-by)
         (set! order-by (parse-sql-order-by (cdr cd)))]
        [(limit)
         (set! limit-val (cadr cd))]
        [(offset)
         (set! offset-val (cadr cd))])))

  (sql-select columns from-clause joins where-clause group-by having order-by limit-val offset-val distinct?))

(define (parse-sql-as-alias rest)
  (let loop ([items rest])
    (cond
      [(null? items) #f]
      [(and (pair? (cdr items)) (eq? (->datum (car items)) ':as))
       (->datum (cadr items))]
      [else (loop (cdr items))])))

(define (parse-sql-join-clause type args)
  (define table (car args))
  (define rest-args (cdr args))
  (define alias (parse-sql-as-alias rest-args))
  (define condition
    (let loop ([items rest-args])
      (cond
        [(null? items) #f]
        [(eq? (->datum (car items)) ':as) (loop (cddr items))]
        [(pair? (->datum (car items)))
         (parse-sql-expr (datum->syntax #f (->datum (car items))))]
        [else (loop (cdr items))])))
  (sql-join type table alias condition))

(define (parse-sql-order-by items)
  (let loop ([rest items] [acc '()])
    (cond
      [(null? rest) (reverse acc)]
      [else
       (define col-d (->datum (car rest)))
       (define col-expr
         (if (and (symbol? col-d) (sql-dot-ref? col-d))
           (parse-sql-column-ref col-d)
           col-d))
       (define-values (dir remaining)
         (if (and (pair? (cdr rest))
                  (memq (->datum (cadr rest)) '(:asc :desc asc desc)))
           (values (let ([v (->datum (cadr rest))])
                     (if (memq v '(:asc asc)) 'asc 'desc))
                   (cddr rest))
           (values 'asc (cdr rest))))
       (loop remaining (cons (sql-order-spec col-expr dir) acc))])))

(provide
 sql-dot-ref? parse-sql-column-ref
 parse-sql-columns parse-sql-column-names parse-sql-values-row
 parse-sql-set-pairs parse-sql-where-clause parse-sql-expr
 parse-sql-top-form parse-sql-with parse-sql-select)
