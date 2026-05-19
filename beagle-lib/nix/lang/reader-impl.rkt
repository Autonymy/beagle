#lang racket/base

;; Nix-specific reader extensions.
;; Adds ''...'' indented string syntax (Nix-native) on top of the base
;; beagle readtable.

(require beagle/lang/reader-impl
         racket/string
         racket/list)

;; Nix ''...'' string reader.
;; '' opens, '' closes.  Triple ''' embeds a literal ''.
;; ''${ escapes interpolation (literal ${).
;; Produces (#%nix-string <raw-text>) to preserve provenance through the AST.

(define (read-nix-indented-string port)
  (let loop ([acc '()])
    (define c (read-char port))
    (cond
      [(eof-object? c)
       (error 'beagle "unterminated '' string")]
      [(char=? c #\')
       (define c2 (peek-char port))
       (cond
         [(and (char? c2) (char=? c2 #\'))
          (define c3 (peek-char port 1))
          (cond
            [(and (char? c3) (char=? c3 #\'))
             ;; ''': preserve as ''' in text (Nix escape for literal '')
             (read-char port) (read-char port)
             (loop (cons #\' (cons #\' (cons #\' acc))))]
            [(and (char? c3) (char=? c3 #\$))
             ;; ''${: preserve as ''${ in text (Nix escape for literal ${)
             (read-char port) (read-char port)
             (loop (cons #\$ (cons #\' (cons #\' acc))))]
            [(and (char? c3) (char=? c3 #\\))
             ;; ''\X: preserve as ''\X in text (Nix escape sequence)
             (read-char port) (read-char port)
             (define esc (read-char port))
             (loop (cons esc (cons #\\ (cons #\' (cons #\' acc)))))]
            [else
             ;; Closing ''
             (read-char port)
             (nix-dedent (list->string (reverse acc)))])]
         [else
          ;; Single ' inside string — literal
          (loop (cons c acc))])]
      [else (loop (cons c acc))])))

(define (nix-dedent str)
  (define lines (string-split str "\n" #:trim? #f))
  (define non-empty
    (filter (lambda (l) (not (regexp-match? #rx"^[ \t]*$" l)))
            (if (and (pair? lines) (string=? (car lines) ""))
                (cdr lines) lines)))
  (define min-indent
    (if (null? non-empty) 0
        (apply min
          (map (lambda (l)
                 (string-length (cadr (regexp-match #rx"^([ \t]*)" l))))
               non-empty))))
  (define stripped
    (map (lambda (l)
           (if (>= (string-length l) min-indent)
               (substring l min-indent) l))
         lines))
  ;; Strip leading empty line and trailing whitespace-only line (Nix '' semantics)
  (define trimmed
    (let* ([s stripped]
           [s (if (and (pair? s) (string=? (car s) "")) (cdr s) s)]
           [s (if (and (pair? s) (regexp-match? #rx"^[ \t]*$" (last s))) (drop-right s 1) s)])
      s))
  (string-join trimmed "\n"))

(define (nix-sq-reader ch port src line col pos)
  (define next (peek-char port))
  (cond
    [(and (char? next) (char=? next #\'))
     ;; Two single quotes: nix indented string
     (read-char port)
     (define text (read-nix-indented-string port))
     (define result (list '#%nix-string text))
     (if src
       (datum->syntax #f result (vector src line col pos #f))
       result)]
    [else
     ;; Single quote: standard Racket quote
     (define expr (if src (read-syntax src port) (read port)))
     (define result (list 'quote expr))
     (if src
       (datum->syntax #f result (vector src line col pos #f))
       result)]))

(define beagle-nix-readtable
  (make-readtable beagle-readtable
    #\' 'terminating-macro nix-sq-reader))

(define (beagle-nix-read in)
  (parameterize ([read-square-bracket-with-tag '#%brackets]
                 [current-readtable beagle-nix-readtable])
    (read in)))

(define (beagle-nix-read-syntax src in)
  (parameterize ([read-square-bracket-with-tag '#%brackets]
                 [current-readtable beagle-nix-readtable])
    (read-syntax src in)))

(provide beagle-nix-read beagle-nix-read-syntax)
