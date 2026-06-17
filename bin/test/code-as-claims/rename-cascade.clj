#!/usr/bin/env bb
;; rename-cascade: a CROSS-FILE scope-correct rename as a claim edit on the
;; canonical Fram store. Renames a def `old`->`new` defined in module <target-ns>:
;;   - in the home module: the unqualified symbol `old` (def + intra-module callers)
;;   - in EVERY module: the qualified symbol `<alias>/old` -> `<alias>/new`, for each
;;     alias the module bound to <target-ns> via (require <target-ns> :as <alias>)
;; Same-named symbols bound to OTHER modules (a different alias, or another module's
;; own `old`) are untouched — scope is resolved through the require graph, not text.
;; This is the "rename a def, fix all readers across files" cascade. Supersedes
;; (claim-native, recoverable). Re-extracts each file's live claims to <outdir>.
;;
;;   bb -cp <fram>/out rename-cascade.clj <old> <new> <home-file-substr> <target-ns> <outdir> <edn>...
(ns rename-cascade
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [fram.cnf :as c]))

(def argv *command-line-args*)
(def old-name   (nth argv 0))
(def new-name   (nth argv 1))
(def home-substr (nth argv 2))
(def target-ns  (nth argv 3))
(def outdir     (nth argv 4))
(def edn-files  (drop 5 argv))

(def ctx (c/new-store))
(def tx  (c/begin-tx! ctx "author"))
(def SUP (c/value! ctx "supersedes"))
(c/set-supersedes-pred! ctx SUP)
(def file->ents (atom {}))

(defn load-edn [path]
  (let [lines (str/split-lines (slurp path))
        src   (-> (first (filter #(str/starts-with? % "@file") lines)) (subs 6))
        local (atom {})
        ent   (fn [lid] (or (@local lid)
                            (let [e (c/entity! ctx)]
                              (swap! local assoc lid e)
                              (swap! file->ents update src (fnil conj []) e) e)))]
    (doseq [line lines :when (str/starts-with? line "[")]
      (let [[s p o] (edn/read-string line)
            L (ent s) P (c/value! ctx p)
            R (if (number? o) (ent o) (c/value! ctx o))]
        (c/claim! ctx L P R tx)))
    src))
(def srcs (mapv load-edn edn-files))

(def Vp   (c/value! ctx "v"))
(def KIND (c/value! ctx "kind"))
(def SYM  (c/value! ctx "symbol"))
(defn symbol-leaf? [e] (some #(= SYM (:r (c/claim-of ctx %))) (c/by-lp ctx e KIND)))
(defn field-child [e fname]
  (let [P (c/value-id ctx fname)]
    (when P (let [cids (c/by-lp ctx e P)] (when (seq cids) (:r (c/claim-of ctx (first cids))))))))
(defn sym-val [e]
  (when (and e (symbol-leaf? e))
    (let [vc (filter #(= Vp (:p (c/claim-of ctx %))) (c/by-l ctx e))]
      (when (seq vc) (c/literal ctx (:r (c/claim-of ctx (first vc))))))))

;; per-file alias -> ns, parsed from (require <ns> :as <alias>) list nodes
(defn requires-in [ents]
  (into {} (for [e ents
                 :when (= "require" (sym-val (field-child e "f0")))
                 :let [ns* (sym-val (field-child e "f1"))
                       as* (sym-val (field-child e "f2"))
                       al  (sym-val (field-child e "f3"))]
                 :when (and ns* (= as* ":as") al)]
             [al ns*])))
(def alias-map (into {} (for [[src ents] @file->ents] [src (requires-in ents)])))

(def home-files (filter #(str/includes? % home-substr) (keys @file->ents)))

;; rename one value-string old->new for symbol leaves restricted to `ents`
(defn rename-leaves! [ents from to]
  (let [FROMv (c/value-id ctx from)]
    (if (nil? FROMv) 0
        (let [eset (set ents) n (atom 0) TOv (c/value! ctx to)]
          (doseq [cid (vec (c/by-pr ctx Vp FROMv))]
            (let [e (:l (c/claim-of ctx cid))]
              (when (and (eset e) (symbol-leaf? e))
                (let [ncid (c/claim! ctx e Vp TOv tx)] (c/claim! ctx ncid SUP cid tx))
                (swap! n inc))))
          @n))))

;; 1. home module: unqualified old -> new
(def home-renamed
  (reduce + (for [f home-files] (rename-leaves! (@file->ents f) old-name new-name))))
;; 2. every module: <alias>/old -> <alias>/new for aliases bound to target-ns
(def qual-renamed
  (reduce + (for [[src ents] @file->ents
                  [al ns*] (alias-map src)
                  :when (= ns* target-ns)]
              (rename-leaves! ents (str al "/" old-name) (str al "/" new-name)))))

(defn base [src] (-> src (str/split #"/") last))
(defn extract-file! [src out-path]
  (with-open [w (io/writer out-path)]
    (binding [*out* w]
      (println (str "@file " src))
      (doseq [e (@file->ents src) cid (c/by-l ctx e)]
        (let [cl (c/claim-of ctx cid) p (:p cl) r (:r cl) ps (c/literal ctx p)]
          (when (not= ps "supersedes")
            (if (c/value-object? ctx r)
              (println (str "[" e " " (pr-str ps) " " (pr-str (c/literal ctx r)) "]"))
              (println (str "[" e " " (pr-str ps) " " r "]")))))))))
(.mkdirs (io/file outdir))
(doseq [src srcs] (extract-file! src (str outdir "/" (base src) ".edn")))

(binding [*out* *err*]
  (println (str "cross-file rename `" old-name "` -> `" new-name "` (home module ns " target-ns "): "
                home-renamed " unqualified (home) + " qual-renamed " qualified (cross-file refs)")))
