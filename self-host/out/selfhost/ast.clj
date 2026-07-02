(ns selfhost.ast
  (:require [selfhost.rt :as rt]
            [clojure.string :as str]))

^{:line 17 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^String char-at [^String s i]
  ^{:line 18 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 18 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 18 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (>= i 0) ^{:line 18 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (< i ^{:line 18 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count s))) ^{:line 18 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (subs s i ^{:line 18 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (+ i 1)) ""))

^{:line 20 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^String substring2 [^String s a b]
  ^{:line 21 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [n ^{:line 21 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count s)
   lo ^{:line 22 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 22 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (< a 0) 0 ^{:line 22 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 22 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> a n) n a))
   hi ^{:line 23 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 23 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (< b lo) lo ^{:line 23 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 23 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> b n) n b))]
  ^{:line 24 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (subs s lo hi)))

^{:line 28 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (def ^String BRACKET-TAG "#%brackets")

^{:line 29 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (def ^String MAP-TAG "#%map")

^{:line 30 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (def ^String SET-TAG "#%set")

^{:line 34 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean bracketed? [d]
  ^{:line 35 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 35 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (vector? d) ^{:line 35 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> ^{:line 35 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count d) 0) ^{:line 35 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 35 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (nth d 0) BRACKET-TAG)))

^{:line 37 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn bracket-body [d]
  ^{:line 38 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 38 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (vector? d) ^{:line 38 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (subvec d 1) ^{:line 38 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} []))

^{:line 40 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean map-tagged? [d]
  ^{:line 41 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 41 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (vector? d) ^{:line 41 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> ^{:line 41 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count d) 0) ^{:line 41 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 41 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (nth d 0) MAP-TAG)))

^{:line 43 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn map-body [d]
  ^{:line 44 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 44 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (vector? d) ^{:line 44 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (subvec d 1) ^{:line 44 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} []))

^{:line 46 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean set-tagged? [d]
  ^{:line 47 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 47 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (vector? d) ^{:line 47 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> ^{:line 47 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count d) 0) ^{:line 47 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 47 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (nth d 0) SET-TAG)))

^{:line 49 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn set-body [d]
  ^{:line 50 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 50 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (vector? d) ^{:line 50 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (subvec d 1) ^{:line 50 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} []))

^{:line 52 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn unwrap-items [d ^String what]
  ^{:line 53 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (cond
  ^{:line 54 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (bracketed? d) ^{:line 54 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (bracket-body d)
  ^{:line 55 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (vector? d) d
  :else ^{:line 56 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} []))

^{:line 60 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean dot-method-sym? [^String sym]
  ^{:line 61 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 61 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> ^{:line 61 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count sym) 1) ^{:line 61 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 61 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (char-at sym 0) ".")))

^{:line 63 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean upper-case-char? [code]
  ^{:line 64 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 64 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (>= code 65) ^{:line 64 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (<= code 90)))

^{:line 66 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean static-method-sym? [^String sym]
  ^{:line 67 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [idx ^{:line 67 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (str/index-of sym "/")
   slash-pos ^{:line 68 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if ^{:line 68 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (nil? idx) -1 idx)]
  ^{:line 69 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 69 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> slash-pos 0) ^{:line 69 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (< ^{:line 69 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (+ slash-pos 1) ^{:line 69 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count sym)) ^{:line 70 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (or ^{:line 70 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (upper-case-char? ^{:line 70 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (int ^{:line 70 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (.charAt sym 0))) ^{:line 71 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 71 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (substring2 sym 0 3) "js/")))))

^{:line 73 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean dynamic-var-sym? [^String sym]
  ^{:line 74 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 74 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (>= ^{:line 74 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count sym) 3) ^{:line 75 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 75 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (char-at sym 0) "*") ^{:line 76 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 76 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (char-at sym ^{:line 76 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (- ^{:line 76 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count sym) 1)) "*")))

^{:line 78 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean constructor-sym? [^String sym]
  ^{:line 79 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 79 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> ^{:line 79 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count sym) 1) ^{:line 80 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (upper-case-char? ^{:line 80 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (int ^{:line 80 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (.charAt sym 0))) ^{:line 81 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 81 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (char-at sym ^{:line 81 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (- ^{:line 81 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count sym) 1)) ".")))

^{:line 83 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean keyword-sym? [^String sym]
  ^{:line 84 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 84 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (> ^{:line 84 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count sym) 1) ^{:line 84 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 84 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (char-at sym 0) ":")))

^{:line 90 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-ns-decl [^String name]
  ^{:line 91 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "ns" "name" name})

^{:line 93 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-def [^String name ann value]
  ^{:line 94 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "def" "name" name "ann" ann "value" value})

^{:line 96 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-defonce [^String name ann value]
  ^{:line 97 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "defonce" "name" name "ann" ann "value" value})

^{:line 99 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-defn [^String name params rest-param ret body ^Boolean private-]
  ^{:line 101 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "defn" "name" name "params" params "rest-param" rest-param "ret" ret "body" body "private" private-})

^{:line 104 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-defn-multi [^String name arities ^Boolean private-]
  ^{:line 105 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "defn-multi" "name" name "arities" arities "private" private-})

^{:line 107 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-fn [params rest-param ret body]
  ^{:line 108 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "fn" "params" params "rest-param" rest-param "ret" ret "body" body})

^{:line 110 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-let [bindings body]
  ^{:line 111 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "let" "bindings" bindings "body" body})

^{:line 113 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-if [test then-expr else-expr]
  ^{:line 114 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "if" "test" test "then" then-expr "else" else-expr})

^{:line 116 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-cond [clauses]
  ^{:line 117 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "cond" "clauses" clauses})

^{:line 119 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-when [test body]
  ^{:line 120 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "when" "test" test "body" body})

^{:line 122 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-do [body]
  ^{:line 123 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "do" "body" body})

^{:line 125 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-call [fn-name args]
  ^{:line 126 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "call" "fn" fn-name "args" args})

^{:line 128 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-ref [^String name]
  ^{:line 129 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "ref" "name" name})

^{:line 131 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-literal [^String kind value]
  ^{:line 132 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "literal" "kind" kind "value" value})

^{:line 134 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-vec [items]
  ^{:line 135 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "vec" "items" items})

^{:line 137 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-quoted [datum]
  ^{:line 138 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "quoted" "datum" datum})

^{:line 140 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-unsafe [^String code]
  ^{:line 141 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "unsafe" "code" code})

^{:line 143 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-regex [^String pattern]
  ^{:line 144 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "regex" "pattern" pattern})

^{:line 146 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-loop [bindings body]
  ^{:line 147 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "loop" "bindings" bindings "body" body})

^{:line 149 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-recur [args]
  ^{:line 150 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "recur" "args" args})

^{:line 152 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-for [clauses body]
  ^{:line 153 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "for" "clauses" clauses "body" body})

^{:line 155 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-record [^String name fields]
  ^{:line 156 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "record" "name" name "fields" fields})

^{:line 158 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-method-call [^String method target args]
  ^{:line 159 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "method-call" "method" method "target" target "args" args})

^{:line 161 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-static-call [^String class-method args]
  ^{:line 162 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "static-call" "class-method" class-method "args" args})

^{:line 164 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-map [pairs]
  ^{:line 165 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "map" "pairs" pairs})

^{:line 167 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-set [items]
  ^{:line 168 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "set" "items" items})

^{:line 170 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-kw-access [^String kw target fallback]
  ^{:line 171 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "kw-access" "kw" kw "target" target "default" fallback})

^{:line 173 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-try [body catches finally-body]
  ^{:line 174 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "try" "body" body "catches" catches "finally" finally-body})

^{:line 176 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-catch [^String exception-type ^String name body]
  ^{:line 177 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "catch" "exception-type" exception-type "name" name "body" body})

^{:line 179 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-doseq [clauses body]
  ^{:line 180 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "doseq" "clauses" clauses "body" body})

^{:line 182 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-case [test clauses fallback]
  ^{:line 183 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "case" "test" test "clauses" clauses "default" fallback})

^{:line 185 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-match [target clauses]
  ^{:line 186 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "match" "target" target "clauses" clauses})

^{:line 188 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-with [target updates]
  ^{:line 189 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "with" "target" target "updates" updates})

^{:line 191 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-defrecord [^String name fields]
  ^{:line 192 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "defrecord" "name" name "fields" fields})

^{:line 194 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-defenum [^String name values]
  ^{:line 195 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "defenum" "name" name "values" values})

^{:line 197 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-defunion [^String name members type-params member-fields]
  ^{:line 199 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "defunion" "name" name "members" members "type-params" type-params "member-fields" member-fields})

^{:line 202 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-deferror [^String name members member-fields]
  ^{:line 203 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "deferror" "name" name "members" members "member-fields" member-fields})

^{:line 205 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-defscalar [^String name backing predicates]
  ^{:line 206 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "defscalar" "name" name "backing" backing "predicates" predicates})

^{:line 208 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-when-let [^String name expr body]
  ^{:line 209 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "when-let" "name" name "expr" expr "body" body})

^{:line 211 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-if-let [^String name expr then-body else-body]
  ^{:line 212 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "if-let" "name" name "expr" expr "then" then-body "else" else-body})

^{:line 214 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-when-some [^String name expr body]
  ^{:line 215 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "when-some" "name" name "expr" expr "body" body})

^{:line 217 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-if-some [^String name expr then-body else-body]
  ^{:line 218 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "if-some" "name" name "expr" expr "then" then-body "else" else-body})

^{:line 220 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-condp [^String pred-fn test-expr clauses fallback]
  ^{:line 221 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "condp" "pred-fn" pred-fn "test-expr" test-expr "clauses" clauses "default" fallback})

^{:line 223 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-dotimes [^String name count-expr body]
  ^{:line 224 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "dotimes" "name" name "count-expr" count-expr "body" body})

^{:line 226 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-letfn [fns body]
  ^{:line 227 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "letfn" "fns" fns "body" body})

^{:line 229 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-set! [target value]
  ^{:line 230 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "set!" "target" target "value" value})

^{:line 232 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-await [expr]
  ^{:line 233 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "await" "expr" expr})

^{:line 235 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-block-string [^String text ^String tag]
  ^{:line 236 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "block-string" "text" text "tag" tag})

^{:line 240 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-param [^String name ann]
  ^{:line 241 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"type" "param" "name" name "ann" ann})

^{:line 243 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-map-destructure [keys as-name]
  ^{:line 244 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"type" "map-destructure" "keys" keys "as" as-name})

^{:line 246 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-seq-destructure [names rest-name]
  ^{:line 247 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"type" "seq-destructure" "names" names "rest" rest-name})

^{:line 249 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-let-binding [^String name ann value]
  ^{:line 250 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"name" name "ann" ann "value" value})

^{:line 254 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-pat-wildcard []
  ^{:line 255 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"pattern" "wildcard"})

^{:line 257 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-pat-literal [value]
  ^{:line 258 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"pattern" "literal" "value" value})

^{:line 260 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-pat-record [^String type-name bindings]
  ^{:line 261 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"pattern" "record" "type-name" type-name "bindings" bindings})

^{:line 263 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-pat-map [entries]
  ^{:line 264 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"pattern" "map" "entries" entries})

^{:line 266 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-pat-var [^String name]
  ^{:line 267 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"pattern" "var" "name" name})

^{:line 271 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-inherit [names]
  ^{:line 272 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-inherit" "names" names})

^{:line 274 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-inherit-from [ns-expr names]
  ^{:line 275 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-inherit-from" "ns-expr" ns-expr "names" names})

^{:line 277 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-with [ns-expr body]
  ^{:line 278 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-with" "ns-expr" ns-expr "body" body})

^{:line 280 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-rec-attrs [pairs]
  ^{:line 281 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-rec-attrs" "pairs" pairs})

^{:line 283 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-assert [cond-expr body]
  ^{:line 284 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-assert" "cond-expr" cond-expr "body" body})

^{:line 286 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-get-or [base path fallback]
  ^{:line 287 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-get-or" "base" base "path" path "default" fallback})

^{:line 289 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-has-attr [base path]
  ^{:line 290 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-has-attr" "base" base "path" path})

^{:line 292 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-search-path [^String name]
  ^{:line 293 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-search-path" "name" name})

^{:line 295 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-interpolated-string [parts]
  ^{:line 296 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-interpolated-string" "parts" parts})

^{:line 298 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-multiline-string [lines]
  ^{:line 299 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-multiline-string" "lines" lines})

^{:line 301 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-path [^String path]
  ^{:line 302 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-path" "path" path})

^{:line 304 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-fn-set [formals ^Boolean rest at-name body]
  ^{:line 305 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-fn-set" "formals" formals "rest" rest "at-name" at-name "body" body})

^{:line 307 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-pipe [^String direction lhs rhs]
  ^{:line 308 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-pipe" "direction" direction "lhs" lhs "rhs" rhs})

^{:line 310 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-nix-impl [lhs rhs]
  ^{:line 311 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"node" "nix-impl" "lhs" lhs "rhs" rhs})

^{:line 315 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (def ^String DEFAULT-MODE "strict")

^{:line 316 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (def ^String DEFAULT-TARGET "clj")

^{:line 317 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (def ^String DEFAULT-NAMESPACE "beagle.user")

^{:line 319 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn make-program [^String mode ^String namespace ^String target forms externs requires]
  ^{:line 321 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"mode" mode "namespace" namespace "target" target "forms" forms "externs" externs "requires" requires})

^{:line 326 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean validate-identifier [^String sym]
  ^{:line 327 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [bad-chars ";'\"` (){}[],"]
  ^{:line 328 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (every? ^{:line 328 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (fn [c] ^{:line 328 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (nil? ^{:line 328 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (str/index-of bad-chars c))) ^{:line 329 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (map str ^{:line 329 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (seq sym)))))

^{:line 331 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn ^Boolean validate-module-path [^String path]
  ^{:line 332 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 332 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (every? ^{:line 332 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (fn [c] ^{:line 333 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [code ^{:line 333 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (int ^{:line 333 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (.charAt c 0))]
  ^{:line 334 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (or ^{:line 334 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (upper-case-char? code) ^{:line 335 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 335 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (>= code 97) ^{:line 335 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (<= code 122)) ^{:line 336 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (and ^{:line 336 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (>= code 48) ^{:line 336 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (<= code 57)) ^{:line 337 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= c ".") ^{:line 337 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= c "_") ^{:line 337 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= c "/") ^{:line 337 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= c "-")))) ^{:line 338 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (map str ^{:line 338 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (seq path))) ^{:line 339 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (nil? ^{:line 339 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (str/index-of path ".."))))

^{:line 343 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (def passes ^{:line 343 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (atom ^{:line 343 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} []))

^{:line 344 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (def failures ^{:line 344 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (atom ^{:line 344 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} []))

^{:line 346 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn- expect! [^String label ^Boolean result]
  ^{:line 347 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (if result ^{:line 348 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (do
  ^{:line 348 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (swap! passes conj true)
  nil) ^{:line 349 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (do
  ^{:line 349 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (swap! failures conj label)
  nil)))

^{:line 351 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (defn run-tests! []
  ^{:line 352 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (reset! passes ^{:line 352 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [])
  ^{:line 353 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (reset! failures ^{:line 353 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [])
  ^{:line 357 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "bracketed?" ^{:line 357 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (bracketed? ^{:line 357 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [BRACKET-TAG "a" "b"]))
  ^{:line 358 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "not bracketed?" ^{:line 358 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (not ^{:line 358 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (bracketed? ^{:line 358 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["a" "b"])))
  ^{:line 359 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "bracket-body" ^{:line 359 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 359 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (bracket-body ^{:line 359 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [BRACKET-TAG "x" "y"]) ^{:line 359 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["x" "y"]))
  ^{:line 360 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "map-tagged?" ^{:line 360 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (map-tagged? ^{:line 360 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [MAP-TAG "k" "v"]))
  ^{:line 361 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "not map-tagged?" ^{:line 361 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (not ^{:line 361 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (map-tagged? ^{:line 361 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["k" "v"])))
  ^{:line 362 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "set-tagged?" ^{:line 362 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (set-tagged? ^{:line 362 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [SET-TAG "a"]))
  ^{:line 366 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "dot-method: .foo" ^{:line 366 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (dot-method-sym? ".foo"))
  ^{:line 367 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "dot-method: not foo" ^{:line 367 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (not ^{:line 367 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (dot-method-sym? "foo")))
  ^{:line 368 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "dot-method: not ." ^{:line 368 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (not ^{:line 368 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (dot-method-sym? ".")))
  ^{:line 369 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "static: Math/abs" ^{:line 369 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (static-method-sym? "Math/abs"))
  ^{:line 370 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "static: js/console" ^{:line 370 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (static-method-sym? "js/console"))
  ^{:line 371 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "static: not foo/bar" ^{:line 371 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (not ^{:line 371 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (static-method-sym? "foo/bar")))
  ^{:line 372 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "dynamic: *state*" ^{:line 372 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (dynamic-var-sym? "*state*"))
  ^{:line 373 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "dynamic: not *x" ^{:line 373 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (not ^{:line 373 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (dynamic-var-sym? "*x")))
  ^{:line 374 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "constructor: Point." ^{:line 374 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (constructor-sym? "Point."))
  ^{:line 375 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "constructor: not point." ^{:line 375 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (not ^{:line 375 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (constructor-sym? "point.")))
  ^{:line 376 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "keyword: :name" ^{:line 376 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (keyword-sym? ":name"))
  ^{:line 377 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "keyword: not name" ^{:line 377 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (not ^{:line 377 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (keyword-sym? "name")))
  ^{:line 381 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [node ^{:line 381 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-def "x" nil ^{:line 381 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-literal "number" 42))]
  ^{:line 382 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-def node type" ^{:line 382 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 382 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "node") "def"))
  ^{:line 383 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-def name" ^{:line 383 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 383 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "name") "x"))
  ^{:line 384 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-def value" ^{:line 384 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 384 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 384 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "value") "kind") "number")))
  ^{:line 386 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [node ^{:line 386 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-defn "foo" ^{:line 386 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [^{:line 386 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-param "x" ^{:line 386 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"kind" "prim" "name" "Int"})] nil ^{:line 387 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"kind" "prim" "name" "String"} ^{:line 388 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [^{:line 388 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-call "str" ^{:line 388 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [^{:line 388 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-ref "x")])] false)]
  ^{:line 389 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-defn node type" ^{:line 389 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 389 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "node") "defn"))
  ^{:line 390 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-defn params" ^{:line 390 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 390 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count ^{:line 390 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "params")) 1))
  ^{:line 391 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-defn param name" ^{:line 391 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 391 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 391 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (nth ^{:line 391 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "params") 0) "name") "x")))
  ^{:line 393 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [node ^{:line 393 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-if ^{:line 393 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-literal "bool" true) ^{:line 394 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-literal "string" "yes") ^{:line 395 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-literal "string" "no"))]
  ^{:line 396 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-if" ^{:line 396 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 396 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "node") "if"))
  ^{:line 397 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-if then" ^{:line 397 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 397 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 397 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "then") "value") "yes")))
  ^{:line 399 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [node ^{:line 399 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-match ^{:line 399 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-ref "x") ^{:line 400 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [^{:line 400 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"pattern" ^{:line 400 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-pat-record "Circle" ^{:line 400 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["r"]) "body" ^{:line 400 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-ref "r")}])]
  ^{:line 402 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-match" ^{:line 402 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 402 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "node") "match"))
  ^{:line 403 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-match target" ^{:line 403 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 403 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 403 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "target") "name") "x")))
  ^{:line 405 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [node ^{:line 405 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-defunion "Shape" ^{:line 405 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["Circle" "Rect"] nil nil)]
  ^{:line 406 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-defunion" ^{:line 406 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 406 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "node") "defunion"))
  ^{:line 407 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "make-defunion members" ^{:line 407 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 407 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count ^{:line 407 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "members")) 2)))
  ^{:line 411 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [p ^{:line 411 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-param "x" ^{:line 411 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"kind" "prim" "name" "Int"})]
  ^{:line 412 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "param type" ^{:line 412 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 412 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get p "type") "param"))
  ^{:line 413 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "param name" ^{:line 413 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 413 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get p "name") "x"))
  ^{:line 414 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "param ann" ^{:line 414 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 414 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 414 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get p "ann") "name") "Int")))
  ^{:line 416 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [d ^{:line 416 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-map-destructure ^{:line 416 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["a" "b"] "m")]
  ^{:line 417 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "map-destructure type" ^{:line 417 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 417 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get d "type") "map-destructure"))
  ^{:line 418 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "map-destructure keys" ^{:line 418 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 418 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count ^{:line 418 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get d "keys")) 2)))
  ^{:line 420 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [d ^{:line 420 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-seq-destructure ^{:line 420 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["x" "y"] "rest")]
  ^{:line 421 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "seq-destructure type" ^{:line 421 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 421 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get d "type") "seq-destructure"))
  ^{:line 422 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "seq-destructure rest" ^{:line 422 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 422 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get d "rest") "rest")))
  ^{:line 426 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "pat-wildcard" ^{:line 426 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 426 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 426 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-pat-wildcard) "pattern") "wildcard"))
  ^{:line 427 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "pat-literal" ^{:line 427 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 427 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 427 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-pat-literal 42) "value") 42))
  ^{:line 428 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "pat-record" ^{:line 428 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 428 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 428 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-pat-record "Circle" ^{:line 428 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["r"]) "type-name") "Circle"))
  ^{:line 429 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "pat-var" ^{:line 429 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 429 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get ^{:line 429 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-pat-var "x") "name") "x"))
  ^{:line 433 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [node ^{:line 433 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-nix-inherit ^{:line 433 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} ["a" "b"])]
  ^{:line 434 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "nix-inherit" ^{:line 434 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 434 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "node") "nix-inherit"))
  ^{:line 435 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "nix-inherit names" ^{:line 435 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 435 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count ^{:line 435 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "names")) 2)))
  ^{:line 437 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (let [node ^{:line 437 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-nix-fn-set ^{:line 437 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} [^{:line 437 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} {"name" "x" "default" nil}] true "args" ^{:line 437 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (make-ref "x"))]
  ^{:line 438 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "nix-fn-set" ^{:line 438 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 438 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "node") "nix-fn-set"))
  ^{:line 439 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "nix-fn-set rest" ^{:line 439 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= ^{:line 439 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (get node "rest") true)))
  ^{:line 443 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "DEFAULT-MODE" ^{:line 443 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= DEFAULT-MODE "strict"))
  ^{:line 444 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "DEFAULT-TARGET" ^{:line 444 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= DEFAULT-TARGET "clj"))
  ^{:line 445 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (expect! "DEFAULT-NAMESPACE" ^{:line 445 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (= DEFAULT-NAMESPACE "beagle.user"))
  ^{:line 449 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (doseq [f ^{:line 449 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (deref failures)]
  ^{:line 450 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (selfhost.rt/eprint ^{:line 450 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (str "  FAIL: " f "\n")))
  ^{:line 451 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (println ^{:line 451 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (str "  AST: " ^{:line 451 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count ^{:line 451 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (deref passes)) " passed, " ^{:line 452 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count ^{:line 452 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (deref failures)) " failed"))
  ^{:line 453 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (count ^{:line 453 :file "/home/tom/code/beagle/.worktrees/selfhost/self-host/src/selfhost/ast.bclj"} (deref failures)))
