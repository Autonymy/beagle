#lang racket/base

(require rackunit
         racket/string
         racket/port
         "../private/parse.rkt"
         "../private/emit.rkt"
         "../private/types.rkt")

(define (mt . xs) (cons MAP-TAG xs))
(define (br . xs) (cons '#%brackets xs))

(define (nix-emit src)
  (define stxs
    (parameterize ([read-square-bracket-with-tag '#%brackets])
      (with-input-from-string src
        (lambda ()
          (let loop ([acc '()])
            (define d (read-syntax 'test))
            (if (eof-object? d) (reverse acc) (loop (cons d acc))))))))
  (define prog
    (with-handlers ([exn:fail? (lambda (e) #f)])
      (parse-program stxs)))
  (and prog
       (with-handlers ([exn:fail? (lambda (e) (exn-message e))])
         (string-trim (emit-program prog)))))

(define (nix-emit-forms . forms)
  (define stxs (map (lambda (f) (datum->syntax #f f)) forms))
  (define prog
    (with-handlers ([exn:fail? (lambda (e) #f)])
      (parse-program stxs)))
  (and prog
       (with-handlers ([exn:fail? (lambda (e) (exn-message e))])
         (string-trim (emit-program prog)))))

;; --- basic forms -----------------------------------------------------------

(test-case "def emits let binding"
  (define out (nix-emit "(define-target nix) (def x : Int 42)"))
  (check-true (string-contains? out "x = 42;"))
  (check-true (string-contains? out "let")))

(test-case "defn emits curried function"
  (define out (nix-emit "(define-target nix) (defn add [(a : Int) (b : Int)] : Int (+ a b))"))
  (check-true (string-contains? out "add = a: b:"))
  (check-true (string-contains? out "a + b")))

(test-case "fn emits lambda"
  (define out (nix-emit "(define-target nix) (def f : Any (fn [(x : Int)] : Int (+ x 1)))"))
  (check-true (string-contains? out "x:"))
  (check-true (string-contains? out "x + 1")))

(test-case "if emits if/then/else"
  (define out (nix-emit "(define-target nix) (if true 1 0)"))
  (check-true (string-contains? out "if true then 1 else 0")))

(test-case "let emits let/in"
  (define out (nix-emit "(define-target nix) (let [x 1 y 2] (+ x y))"))
  (check-true (string-contains? out "let"))
  (check-true (string-contains? out "x = 1;"))
  (check-true (string-contains? out "y = 2;"))
  (check-true (string-contains? out "in"))
  (check-true (string-contains? out "x + y")))

;; --- data structures -------------------------------------------------------

(test-case "vector emits nix list"
  (define out (nix-emit "(define-target nix) [1 2 3]"))
  (check-true (string-contains? out "[ 1 2 3 ]")))

(test-case "map emits nix attrset"
  (define out (nix-emit-forms '(define-target nix) `(def m : Any ,(mt ':a 1 ':b 2))))
  (check-true (string-contains? out "a = 1;"))
  (check-true (string-contains? out "b = 2;"))
  (check-true (string-contains? out "{")))

(test-case "nested attrset"
  (define inner (mt ':inner 42))
  (define out (nix-emit-forms '(define-target nix) `(def m : Any ,(mt ':outer inner))))
  (check-true (and out (string-contains? out "outer ="))))

;; --- records ---------------------------------------------------------------

(test-case "defrecord emits constructor + accessors"
  (define out (nix-emit "(define-target nix) (defrecord Point [(x : Int) (y : Int)])"))
  (check-true (string-contains? out "mkPoint = x: y:"))
  (check-true (string-contains? out "_tag = \"point\""))
  (check-true (string-contains? out "point_x = r: r.x;"))
  (check-true (string-contains? out "point_y = r: r.y;")))

;; --- nix builtins ----------------------------------------------------------

(test-case "builtins/ calls emit as builtins.*"
  (define out (nix-emit "(define-target nix) (builtins/length [1 2 3])"))
  (check-true (string-contains? out "builtins.length")))

(test-case "lib/ calls emit as lib.*"
  (define out (nix-emit "(define-target nix) (lib/mkIf true {:enable true})"))
  (check-true (string-contains? out "lib.mkIf true")))

;; --- stdlib fns ------------------------------------------------------------

(test-case "map fn emits builtins.map"
  (define out (nix-emit-forms '(define-target nix)
    `(map (fn ,(br '(x : Int)) : Int (+ x 1)) ,(br 1 2 3))))
  (check-true (string-contains? out "builtins.map")))

(test-case "filter fn emits builtins.filter"
  (define out (nix-emit-forms '(define-target nix)
    `(filter (fn ,(br '(x : Int)) : Bool (> x 0)) ,(br 1 -1 2))))
  (check-true (string-contains? out "builtins.filter")))

(test-case "nil? emits null check"
  (define out (nix-emit "(define-target nix) (nil? x)"))
  (check-true (string-contains? out "== null")))

(test-case "count emits builtins.length"
  (define out (nix-emit "(define-target nix) (count [1 2 3])"))
  (check-true (string-contains? out "builtins.length")))

(test-case "merge emits //"
  (define out (nix-emit "(define-target nix) (merge {:a 1} {:b 2})"))
  (check-true (string-contains? out "//")))

(test-case "concat emits ++"
  (define out (nix-emit "(define-target nix) (concat [1] [2])"))
  (check-true (string-contains? out "++")))

;; --- keyword access --------------------------------------------------------

(test-case "keyword access emits field selection"
  (define out (nix-emit "(define-target nix) (:name person)"))
  (check-true (string-contains? out "person.name")))

;; --- dotted option paths ---------------------------------------------------

(test-case "dotted keyword keys become Nix option paths"
  (define out (nix-emit-forms '(define-target nix) `(def m : Any ,(mt ':services.openssh.enable #t))))
  (check-true (string-contains? out "services.openssh.enable = true;")))

;; --- cond ------------------------------------------------------------------

(test-case "cond emits nested if/then/else"
  (define out (nix-emit "(define-target nix) (cond [true 1] [false 2])"))
  (check-true (string-contains? out "if true then 1 else"))
  (check-true (string-contains? out "if false then 2")))

;; --- with (record update) --------------------------------------------------

(test-case "with emits attrset merge"
  (define out (nix-emit "(define-target nix) (defrecord Foo [(a : Int)]) (with (->Foo 1) [:a 2])"))
  (check-true (string-contains? out "//")))

;; --- string ops ------------------------------------------------------------

(test-case "str emits string concatenation"
  (define out (nix-emit "(define-target nix) (str \"hello\" \" \" \"world\")"))
  (check-true (string-contains? out "\"hello\" + \" \" + \"world\"")))

;; --- comparison operators --------------------------------------------------

(test-case "comparison operators"
  (define out (nix-emit "(define-target nix) (< 1 2)"))
  (check-true (string-contains? out "1 < 2"))
  (define out2 (nix-emit "(define-target nix) (= 1 1)"))
  (check-true (string-contains? out2 "1 == 1")))
