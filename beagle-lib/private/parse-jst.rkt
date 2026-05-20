#lang racket/base

;; Typed JS target (js/*) parse helpers — extracted from parse.rkt.

(require racket/string
         "ast.rkt"
         "types.rkt")

(define JST-BINARY-OPS
  (hasheq 'js/+  '+   'js/-  '-   'js/*  '*   'js/div  '/   'js/%  '%   'js/**  '**
          'js/=== '===  'js/!== '!==  'js/== '==  'js/!= '!=
          'js/<  '<   'js/>  '>   'js/<= '<=  'js/>= '>=
          'js/&& 'and  'js/|| 'or  'js/?? 'nullish
          'js/in 'in  'js/instanceof 'instanceof))

(define (jst-binary-op? sym)
  (and (symbol? sym) (hash-has-key? JST-BINARY-OPS sym)))

(define (jst-dotted-symbol? sym)
  (define s (symbol->string sym))
  (and (string-contains? s ".")
       (not (string-prefix? s "."))
       (not (string-suffix? s "."))))

(define (jst-split-dotted sym)
  (define parts (string-split (symbol->string sym) "."))
  (for/fold ([acc (string->symbol (car parts))])
            ([p (in-list (cdr parts))])
    (jst-dot acc (string->symbol p))))

(define (parse-jst-callee form)
  (define d (->datum form))
  (cond
    [(and (symbol? d) (jst-dotted-symbol? d))
     (jst-split-dotted d)]
    [else ((current-parse-expr) form)]))

(define (jst-split-ret-body params-form body-forms)
  (define-values (param-list rest-param) ((current-parse-params) params-form))
  (define-values (ret-type body-start)
    (cond
      [(and (>= (length body-forms) 2)
            (eq? (->datum (car body-forms)) ':))
       (values (parse-type (cadr body-forms)) (cddr body-forms))]
      [else (values #f body-forms)]))
  (values param-list rest-param ret-type (map (current-parse-expr) body-start)))

(define (parse-jst-fn name params-form body-forms async? export?)
  (define-values (params rest-param ret-type body)
    (jst-split-ret-body params-form body-forms))
  (jst-fn name params rest-param ret-type body async? export?))

(define (parse-jst-arrow params-form body-forms async?)
  (define-values (params rest-param ret-type body)
    (jst-split-ret-body params-form body-forms))
  (jst-arrow params rest-param ret-type body async?))

(define (parse-jst-try body-forms)
  (define-values (try-body catch-name catch-body finally-body)
    (let loop ([forms body-forms] [acc '()])
      (cond
        [(null? forms)
         (values (reverse acc) #f #f #f)]
        [(eq? (->datum (car forms)) 'catch)
         (when (null? (cdr forms))
           (error 'beagle "js/try: catch requires a binding name"))
         (define cname (->datum (cadr forms)))
         (unless (symbol? cname)
           (error 'beagle "js/try: catch binding must be a symbol, got ~v" cname))
         (define rest-after-name (cddr forms))
         (define-values (cbody rest-after-catch)
           (let cloop ([r rest-after-name] [cacc '()])
             (cond
               [(null? r) (values (reverse cacc) '())]
               [(eq? (->datum (car r)) 'finally) (values (reverse cacc) r)]
               [else (cloop (cdr r) (cons ((current-parse-expr) (car r)) cacc))])))
         (define fbody
           (if (and (pair? rest-after-catch) (eq? (->datum (car rest-after-catch)) 'finally))
               (map (current-parse-expr) (cdr rest-after-catch))
               #f))
         (values (map (current-parse-expr) (reverse acc)) cname cbody fbody)]
        [(eq? (->datum (car forms)) 'finally)
         (values (map (current-parse-expr) (reverse acc)) #f #f (map (current-parse-expr) (cdr forms)))]
        [else (loop (cdr forms) (cons (car forms) acc))])))
  (jst-try try-body catch-name catch-body finally-body))

(define (parse-jst-for-of binding-form iterable-form body-forms)
  (define bd (->datum binding-form))
  (define-values (name elem-type)
    (cond
      [(symbol? bd) (values bd #f)]
      [(and (list? bd) (= (length bd) 3) (eq? (cadr bd) ':))
       (values (car bd) (parse-type (caddr bd)))]
      [else (error 'beagle "js/for-of: binding must be a symbol or (name : Type), got ~v" bd)]))
  (jst-for-of name elem-type ((current-parse-expr) iterable-form) (map (current-parse-expr) body-forms)))

(define (parse-jst-class name-form rest)
  (define name (->datum name-form))
  (unless (symbol? name)
    (error 'beagle "js/class: name must be a symbol, got ~v" name))
  (define-values (extends methods-raw)
    (cond
      [(and (pair? rest) (eq? (->datum (car rest)) 'extends)
            (pair? (cdr rest)))
       (values (parse-jst-callee (cadr rest)) (cddr rest))]
      [else (values #f rest)]))
  (define methods (map parse-jst-class-method methods-raw))
  (jst-class name extends methods #f))

(define (parse-jst-class-method form)
  (define d (->datum form))
  (unless (pair? d)
    (error 'beagle "js/class method: expected list, got ~v" d))
  (define-values (static? async? kind remaining)
    (let loop ([items d] [s? #f] [a? #f])
      (define head (and (pair? items) (car items)))
      (cond
        [(eq? head 'static) (loop (cdr items) #t a?)]
        [(eq? head 'async) (loop (cdr items) s? #t)]
        [(eq? head 'constructor) (values s? a? 'constructor (cdr items))]
        [(eq? head 'get) (values s? a? 'get (cdr items))]
        [(eq? head 'set) (values s? a? 'set (cdr items))]
        [else (values s? a? 'method items)])))
  (define-values (mname params-form body-forms)
    (cond
      [(eq? kind 'constructor)
       (when (null? remaining)
         (error 'beagle "js/class constructor: expected params"))
       (values 'constructor (car remaining) (cdr remaining))]
      [else
       (when (< (length remaining) 2)
         (error 'beagle "js/class method: expected (name (params) body...)"))
       (values (car remaining) (cadr remaining) (cddr remaining))]))
  (define-values (params rest-param ret-type body)
    (jst-split-ret-body (datum->syntax #f params-form) body-forms))
  (jst-method mname params rest-param ret-type body static? async? kind))

(define (parse-jst-object items)
  (let loop ([rest items] [acc '()])
    (cond
      [(null? rest) (jst-object (reverse acc))]
      [(< (length rest) 2)
       (error 'beagle "js/object: expected key-value pairs, got odd count")]
      [else
       (define key (->datum (car rest)))
       (define val ((current-parse-expr) (cadr rest)))
       (loop (cddr rest) (cons (cons key val) acc))])))

(provide
 JST-BINARY-OPS jst-binary-op?
 parse-jst-callee parse-jst-fn parse-jst-arrow parse-jst-try
 parse-jst-for-of parse-jst-class parse-jst-class-method parse-jst-object)
