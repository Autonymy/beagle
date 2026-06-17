#!/usr/bin/env bb
;; _beagle-callgraph.clj — derive the SCOPE-CORRECT call graph of a beagle source
;; tree from its Fram claim projection, with the transitive blast radius computed
;; by Fram Datalog. Invoked by bin/beagle-callgraph (which feeds it a claim file).
;;
;; This is the graph-native core that replaces regex call-graph scraping: a call
;; binds the defn in its OWN module (module-local lexical scope; else a unique
;; global; else ambiguous/external -> dropped), so same-named functions across
;; modules never collide. The scope-resolution + closure logic is the same one
;; Chartroom benchmarks (its Fram Datalog closure is proven to MATCH in-process
;; truth); cascade consumes the JSON this emits instead of pattern-matching text.
(ns beagle-callgraph
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [cheshire.core :as json]
            [fram.cnf :as c]
            [fram.datalog :as d]))

(def claims-path (first *command-line-args*))

;; ---- parse the @file-delimited claim stream (from beagle-claims) -----------
;; NOTE: Racket ~s emits some escapes (\e, \a, ...) Clojure's EDN reader rejects.
;; Those only appear in LEAF literal objects (never the call-graph predicates
;; name/calls/child/form-kind), so we skip-and-count them — the call graph is
;; unaffected. (Same handling as Chartroom; the source-of-truth roundtrip needs
;; EDN-safe leaves, the call graph does not.)
(defn parse-corpus [path]
  (let [skips (volatile! 0)
        blocks (loop [ls (str/split-lines (slurp path)), cur nil, out []]
                 (if (empty? ls)
                   (if cur (conj out cur) out)
                   (let [l (first ls)]
                     (cond
                       (str/starts-with? l "@file ")
                       (recur (rest ls) {:file (subs l 6) :triples []} (if cur (conj out cur) out))
                       (str/starts-with? l "[")
                       (let [t (try (edn/read-string l) (catch Exception _ (vswap! skips inc) nil))]
                         (recur (rest ls) (if t (update cur :triples conj t) cur) out))
                       :else (recur (rest ls) cur out)))))]
    (when (pos? @skips)
      (binding [*out* *err*] (println "  (skipped" @skips "EDN-unparseable leaf literals)")))
    blocks))

(defn index-by [pred triples]
  (reduce (fn [m [s p o]] (if (= p pred) (assoc m s o) m)) {} triples))

(defn module-of [file]
  (-> file (str/split #"/") last (str/replace #"\.[^.]+$" "")))

;; ---- per-file: attribute every call to its NEAREST enclosing defn ----------
;; (iterative DFS over `child` edges; mention = [caller-defn-key callname])
(defn derive-block [block]
  (let [ts   (:triples block)
        file (:file block)
        fk    (index-by "form-kind" ts)
        names (index-by "name" ts)
        calls (index-by "calls" ts)
        kids  (reduce (fn [m [s p o]] (if (= p "child") (update m s (fnil conj []) o) m)) {} ts)
        childset (reduce (fn [s [_ p o]] (if (= p "child") (conj s o) s)) #{} ts)
        roots (remove childset (keys fk))
        defns (vec (for [[s k] fk :when (= k "defn")]
                     {:key [file s] :name (names s) :file file :module (module-of file)}))
        mentions (volatile! [])]
    (loop [stack (mapv (fn [r] [r nil]) roots)]
      (when (seq stack)
        (let [[node cd] (peek stack)
              st  (pop stack)
              cd2 (if (= (fk node) "defn") node cd)]
          (when (and (= (fk node) "call") cd2 (calls node))
            (vswap! mentions conj [[file cd2] (calls node)]))
          (recur (into st (mapv (fn [k] [k cd2]) (get kids node [])))))))
    {:file file :defns defns :mentions @mentions}))

;; ---- global resolution: callname -> the defn it actually binds -------------
;; same-file local definition wins (module-local lexical scope); else a unique
;; global defn; else ambiguous/external -> dropped. THIS is the scope-correctness
;; the bare-symbol regex incumbent skips.
(defn build-graph [blocks]
  (let [derived  (mapv derive-block blocks)
        defns    (vec (mapcat :defns derived))
        by-name  (group-by :name defns)
        mentions (mapcat :mentions derived)
        resolve-call
        (fn [caller-key callname]
          (let [cands (get by-name callname)
                cfile (first caller-key)
                same  (filter #(= (first (:key %)) cfile) cands)]
            (cond
              (seq same)          (:key (first same))
              (= 1 (count cands)) (:key (first (vec cands)))
              :else               nil)))
        edges (->> mentions
                   (keep (fn [[ck nm]] (when-let [callee (resolve-call ck nm)]
                                         (when (not= ck callee) [ck callee]))))
                   distinct vec)]
    {:defns defns :edges edges}))

;; ---- transitive blast radius via Fram Datalog (the persistent-store seam) ---
(defn -main []
  (let [blocks (parse-corpus claims-path)
        {:keys [defns edges]} (build-graph blocks)
        ctx   (c/new-store)
        tx    (c/begin-tx! ctx "code")
        EDGE  (c/value! ctx "calls-defn")
        k->id (volatile! {})
        ent   (fn [k] (or (get @k->id k)
                          (let [e (c/entity! ctx)] (vswap! k->id assoc k e) e)))
        _     (doseq [[a b] edges] (c/claim! ctx (ent a) EDGE (ent b) tx))
        id->k (into {} (map (fn [[k v]] [v k]) @k->id))
        ;; reaches(x,z): x transitively calls z. Blast radius of D (who breaks if
        ;; D changes) = {x | reaches(x, D)} = its transitive callers.
        db    (d/run-rules ctx
                [(d/rule "reaches" [(d/v :x) (d/v :y)]
                         [(d/lit "triple" [(d/v :x) EDGE (d/v :y)])])
                 (d/rule "reaches" [(d/v :x) (d/v :z)]
                         [(d/lit "triple" [(d/v :x) EDGE (d/v :y)])
                          (d/lit "reaches" [(d/v :y) (d/v :z)])])])
        reaches (set (d/facts db "reaches"))
        blast (reduce (fn [m [xid yid]]
                        (update m (id->k yid) (fnil conj #{}) (id->k xid)))
                      {} reaches)
        key->str (fn [k] (str (first k) "#" (second k)))
        defns-out (mapv (fn [dd] {:key (key->str (:key dd)) :file (:file dd)
                                  :module (:module dd) :name (:name dd)}) defns)
        edges-out (mapv (fn [[a b]] [(key->str a) (key->str b)]) edges)
        blast-out (into {} (map (fn [[k vs]] [(key->str k) (mapv key->str vs)]) blast))]
    (binding [*out* *err*]
      (println (format "callgraph: %d defns, %d scope-correct edges, %d transitive reaches-pairs (Fram Datalog closure)"
                       (count defns) (count edges) (count reaches))))
    (println (json/generate-string {:defns defns-out :edges edges-out :blast blast-out}))))

(-main)
