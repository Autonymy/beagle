#lang racket/base

(require racket/string)

(provide
 (struct-out token)
 tokenize
 opener?
 closer?
 delimiter?
 matching-closer-type
 closer-text
 opener-text
 openers-for-closer
 opener-for-closer-type)

(struct token (type text line col offset) #:transparent)

;; Token types:
;;   open-paren close-paren open-bracket close-bracket
;;   open-brace close-brace hash-open-brace
;;   string regex char-literal
;;   line-comment block-comment
;;   whitespace newline
;;   atom

(define (opener? tok)
  (and (memq (token-type tok) '(open-paren open-bracket open-brace hash-open-brace)) #t))

(define (closer? tok)
  (and (memq (token-type tok) '(close-paren close-bracket close-brace)) #t))

(define (delimiter? tok)
  (or (opener? tok) (closer? tok)))

(define (matching-closer-type opener-type)
  (case opener-type
    [(open-paren) 'close-paren]
    [(open-bracket) 'close-bracket]
    [(open-brace hash-open-brace) 'close-brace]
    [else #f]))

(define (closer-text type)
  (case type
    [(close-paren) ")"]
    [(close-bracket) "]"]
    [(close-brace) "}"]
    [else "?"]))

(define (opener-text type)
  (case type
    [(open-paren) "("]
    [(open-bracket) "["]
    [(open-brace) "{"]
    [(hash-open-brace) "#{"]
    [else "?"]))

(define (openers-for-closer closer-type)
  (case closer-type
    [(close-paren) '(open-paren)]
    [(close-bracket) '(open-bracket)]
    [(close-brace) '(open-brace hash-open-brace)]
    [else '()]))

(define (opener-for-closer-type closer-type)
  (case closer-type
    [(close-paren) 'open-paren]
    [(close-bracket) 'open-bracket]
    [(close-brace) 'open-brace]
    [else #f]))

;; ---------------------------------------------------------------------------

(define (atom-terminator? ch)
  (or (char-whitespace? ch)
      (memq ch '(#\( #\) #\[ #\] #\{ #\} #\" #\; #\' #\` #\,))))

(define (tokenize input)
  (define len (string-length input))
  (define tokens '())
  (define pos 0)
  (define line 1)
  (define col 0)

  (define (ch) (and (< pos len) (string-ref input pos)))
  (define (ch+ n) (and (< (+ pos n) len) (string-ref input (+ pos n))))

  (define (advance!)
    (define c (string-ref input pos))
    (set! pos (add1 pos))
    (cond
      [(char=? c #\newline) (set! line (add1 line)) (set! col 0)]
      [else (set! col (add1 col))])
    c)

  (define (emit! type text sl sc so)
    (set! tokens (cons (token type text sl sc so) tokens)))

  ;; -- readers for opaque regions --

  (define (read-string-body! type sl sc so prefix)
    (let loop ([acc (reverse (string->list prefix))])
      (define c (ch))
      (cond
        [(not c)
         (emit! type (list->string (reverse acc)) sl sc so)]
        [(char=? c #\\)
         (advance!)
         (define n (ch))
         (cond
           [n (advance!) (loop (cons n (cons #\\ acc)))]
           [else (emit! type (list->string (reverse (cons #\\ acc))) sl sc so)])]
        [(char=? c #\")
         (advance!)
         (emit! type (list->string (reverse (cons #\" acc))) sl sc so)]
        [else
         (advance!)
         (loop (cons c acc))])))

  (define (read-line-comment! sl sc so)
    (let loop ([acc '()])
      (define c (ch))
      (cond
        [(or (not c) (char=? c #\newline))
         (emit! 'line-comment (list->string (reverse acc)) sl sc so)]
        [else (advance!) (loop (cons c acc))])))

  (define (read-block-comment! sl sc so)
    (advance!) (advance!) ; consume #|
    (let loop ([acc (list #\| #\#)] [depth 1])
      (define c (ch))
      (cond
        [(not c)
         (emit! 'block-comment (list->string (reverse acc)) sl sc so)]
        [(and (char=? c #\|) (eqv? (ch+ 1) #\#))
         (advance!) (advance!)
         (if (= depth 1)
             (emit! 'block-comment (list->string (reverse (cons #\# (cons #\| acc)))) sl sc so)
             (loop (cons #\# (cons #\| acc)) (sub1 depth)))]
        [(and (char=? c #\#) (eqv? (ch+ 1) #\|))
         (advance!) (advance!)
         (loop (cons #\| (cons #\# acc)) (add1 depth))]
        [else (advance!) (loop (cons c acc) depth)])))

  (define (read-char-literal! sl sc so)
    (advance!) (advance!) ; consume #\
    (define c (ch))
    (cond
      [(not c)
       (emit! 'char-literal "#\\" sl sc so)]
      [(char-alphabetic? c)
       (let loop ([acc (list #\\ #\#)])
         (define nc (ch))
         (cond
           [(and nc (or (char-alphabetic? nc) (char=? nc #\-)))
            (advance!) (loop (cons nc acc))]
           [else
            (emit! 'char-literal (list->string (reverse acc)) sl sc so)]))]
      [else
       (advance!)
       (emit! 'char-literal (format "#\\~a" c) sl sc so)]))

  (define (read-atom! sl sc so first-char)
    (let loop ([acc (list first-char)])
      (define c (ch))
      (cond
        [(or (not c) (atom-terminator? c))
         (emit! 'atom (list->string (reverse acc)) sl sc so)]
        [(char=? c #\#)
         (cond
           [(eqv? (ch+ 1) #\|)
            (emit! 'atom (list->string (reverse acc)) sl sc so)]
           [else (advance!) (loop (cons c acc))])]
        [else (advance!) (loop (cons c acc))])))

  (define (read-whitespace! sl sc so)
    (let loop ([acc '()])
      (define c (ch))
      (cond
        [(and c (char-whitespace? c) (not (char=? c #\newline)))
         (advance!) (loop (cons c acc))]
        [else
         (emit! 'whitespace (list->string (reverse acc)) sl sc so)])))

  ;; -- main loop --

  (let loop ()
    (define c (ch))
    (when c
      (define sl line) (define sc col) (define so pos)
      (cond
        [(char=? c #\newline)
         (advance!) (emit! 'newline "\n" sl sc so)]

        [(and (char=? c #\return) (eqv? (ch+ 1) #\newline))
         (advance!) (advance!) (emit! 'newline "\r\n" sl sc so)]

        [(and (char-whitespace? c) (not (char=? c #\newline)))
         (read-whitespace! sl sc so)]

        [(char=? c #\() (advance!) (emit! 'open-paren "(" sl sc so)]
        [(char=? c #\)) (advance!) (emit! 'close-paren ")" sl sc so)]
        [(char=? c #\[) (advance!) (emit! 'open-bracket "[" sl sc so)]
        [(char=? c #\]) (advance!) (emit! 'close-bracket "]" sl sc so)]
        [(char=? c #\{) (advance!) (emit! 'open-brace "{" sl sc so)]
        [(char=? c #\}) (advance!) (emit! 'close-brace "}" sl sc so)]

        [(char=? c #\;) (read-line-comment! sl sc so)]

        [(char=? c #\")
         (advance!)
         (read-string-body! 'string sl sc so "\"")]

        [(char=? c #\#)
         (define next (ch+ 1))
         (cond
           [(eqv? next #\{)
            (advance!) (advance!)
            (emit! 'hash-open-brace "#{" sl sc so)]
           [(eqv? next #\|)
            (read-block-comment! sl sc so)]
           [(eqv? next #\")
            (advance!) (advance!) ; consume #"
            (read-string-body! 'regex sl sc so "#\"")]
           [(eqv? next #\\)
            (read-char-literal! sl sc so)]
           [else
            (advance!)
            (read-atom! sl sc so #\#)])]

        [(memq c '(#\' #\` #\,))
         (advance!)
         (cond
           [(and (char=? c #\,) (eqv? (ch) #\@))
            (advance!)
            (emit! 'atom ",@" sl sc so)]
           [else
            (emit! 'atom (string c) sl sc so)])]

        [else
         (advance!)
         (read-atom! sl sc so c)])

      (loop)))

  (reverse tokens))
