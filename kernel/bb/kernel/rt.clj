(ns kernel.rt
  "Babashka prelude for the differential oracle. Splitmix64 with
  unchecked 64-bit ops so wrap semantics match the Zig prelude bit for
  bit. ctx = {:rng (atom long)}.")

;; signed-long bit patterns of the splitmix constants
(def ^:private GOLDEN -7046029254386353131)  ;; 0x9E3779B97F4A7C15
(def ^:private MIX1   -4658895280553007687)  ;; 0xBF58476D1CE4E5B9
(def ^:private MIX2   -7723592293110705685)  ;; 0x94D049BB133111EB

(defn- mix ^long [^long z0]
  (let [z (unchecked-multiply (bit-xor z0 (unsigned-bit-shift-right z0 30)) MIX1)
        z (unchecked-multiply (bit-xor z (unsigned-bit-shift-right z 27)) MIX2)]
    (bit-xor z (unsigned-bit-shift-right z 31))))

(defn rng-next! ^long [rng]
  (mix (swap! rng #(unchecked-add ^long % GOLDEN))))

(defn rng-below
  "Deterministic draw in [0, n) — unsigned modulo, matching Zig's u64 %."
  ^long [ctx ^long n]
  (Long/remainderUnsigned (rng-next! (:rng ctx)) n))

(defn make-ctx [seed] {:rng (atom (long seed))})
