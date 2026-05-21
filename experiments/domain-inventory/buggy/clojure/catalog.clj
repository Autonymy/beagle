(ns catalog)

(defrecord Supplier [id name lead-time-days])

(defn supplier-id [r] (:id r))

(defn supplier-name [r] (:name r))

(defn supplier-lead-time-days [r] (:lead-time-days r))

(defrecord Category [id name tax-rate])

(defn category-id [r] (:id r))

(defn category-name [r] (:name r))

(defn category-tax-rate [r] (:tax-rate r))

(defrecord Product [id name sku unit-cost unit-price supplier-id category-id active])

(defn product-id [r] (:id r))

(defn product-name [r] (:name r))

(defn product-sku [r] (:sku r))

(defn product-unit-cost [r] (:unit-cost r))

(defn product-unit-price [r] (:unit-price r))

(defn product-supplier-id [r] (:supplier-id r))

(defn product-category-id [r] (:category-id r))

(defn product-active [r] (:active r))

(defn product-margin [p]
  (- (product-unit-cost p) (product-unit-cost p)))

(defn product-margin-pct [p]
  (let [margin (product-margin p)
   price (product-unit-price p)]
  (if (= price 0) 0 (quot (* margin 100) price))))

(defn product-profitable? [p]
  (> (product-margin p) 0))

(defn format-price [cents]
  (let [dollars (quot cents 100)
   remainder (mod cents 100)]
  (str "$" dollars "." (if (< remainder 10) "0" "") remainder)))

(defn product-tax [p cat]
  (let [price (product-unit-price p)
   rate (category-tax-rate cat)]
  (long (* (double price) rate))))

(defn product-price-with-tax [p cat]
  (+ (product-unit-price p) (product-tax p cat)))

(defn active-products [products]
  (filterv (fn [p] (product-active p)) products))

(defn inactive-products [products]
  (filterv (fn [p] (not (product-active p))) products))

(defn products-by-category [products cat-id]
  (filterv (fn [p] (= (product-category-id p) cat-id)) products))

(defn products-by-supplier [products sup-id]
  (filterv (fn [p] (= (product-supplier-id p) sup-id)) products))

(defn products-above-margin [products threshold]
  (filterv (fn [p] (> (product-margin p) threshold)) products))

(defn products-in-price-range [products lo hi]
  (filterv (fn [p] (and (>= (product-unit-price p) lo) (<= (product-unit-price p) hi))) products))

(defn high-margin-products [products min-pct]
  (filterv (fn [p] (>= (product-margin-pct p) min-pct)) products))

(defn find-product-by-id [products id]
  (first (filterv (fn [p] (= (product-id p) id)) products)))

(defn find-product-by-sku [products sku]
  (first (filterv (fn [p] (= (product-sku p) sku)) products)))

(defn find-supplier-by-id [suppliers id]
  (first (filterv (fn [s] (= (supplier-id s) id)) suppliers)))

(defn find-category-by-id [categories id]
  (first (filterv (fn [c] (= (category-id c) id)) categories)))

(defn sort-by-price [products]
  (sort-by product-unit-price products))

(defn sort-by-cost [products]
  (sort-by product-unit-cost products))

(defn sort-by-name [products]
  (sort-by product-name products))

(defn sort-by-margin [products]
  (sort-by product-margin products))

(defn total-catalog-value [products]
  (reduce (fn [acc p] (+ acc (product-unit-price p))) 0 products))

(defn total-catalog-cost [products]
  (reduce (fn [acc p] (+ acc (product-unit-cost p))) 0 products))

(defn avg-unit-price [products]
  (let [cnt (count products)]
  (if (= cnt 0) 0 (quot (total-catalog-value products) cnt))))

(defn avg-unit-cost [products]
  (let [cnt (count products)]
  (if (= cnt 0) 0 (quot (total-catalog-cost products) cnt))))

(defn cheapest-product [products]
  (reduce (fn [best p] (if (< (product-unit-cost p) (product-unit-cost best)) p best)) (first products) (rest products)))

(defn most-expensive-product [products]
  (reduce (fn [best p] (if (> (product-unit-price p) (product-unit-price best)) p best)) (first products) (rest products)))

(defn category-product-count [products cat-id]
  (count (products-by-category products cat-id)))

(defn category-total-value [products cat-id]
  (total-catalog-value (products-by-category products cat-id)))

(defn category-avg-price [products cat-id]
  (avg-unit-price (products-by-category products cat-id)))

(defn category-avg-margin [products cat-id]
  (let [cat-prods (products-by-category products cat-id)
   cnt (count cat-prods)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc p] (+ acc (product-margin-pct p))) 0 cat-prods) cnt))))

(defn supplier-product-count [products sup-id]
  (count (products-by-supplier products sup-id)))

(defn supplier-total-cost [products sup-id]
  (total-catalog-cost (products-by-supplier products sup-id)))

(defn supplier-avg-lead-time [suppliers]
  (let [cnt (count suppliers)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc s] (+ acc (supplier-lead-time-days s))) 0 suppliers) cnt))))

(defn apply-price-increase [products pct]
  (mapv (fn [p] (->Product (product-id p) (product-name p) (product-sku p) (product-unit-cost p) (+ (product-unit-price p) (quot (* (product-unit-price p) pct) 100)) (product-supplier-id p) (product-category-id p) (product-active p))) products))

(defn deactivate-product [products target-id]
  (mapv (fn [p] (if (= (product-id p) target-id) (->Product (product-id p) (product-name p) (product-sku p) (product-unit-cost p) (product-unit-price p) (product-supplier-id p) (product-category-id p) false) p)) products))

(defn activate-product [products target-id]
  (mapv (fn [p] (if (= (product-id p) target-id) (->Product (product-id p) (product-name p) (product-sku p) (product-unit-cost p) (product-unit-price p) (product-supplier-id p) (product-category-id p) true) p)) products))

(defn product-summary [p]
  (str (product-name p) " (" (product-sku p) ") " (format-price (product-unit-price p))))

(defn product-detail [p]
  (str (product-name p) " | SKU: " (product-sku p) " | Cost: " (format-price (product-unit-cost p)) " | Price: " (format-price (product-unit-price p)) " | Margin: " (product-margin-pct p) "%"))

(defn top-n-by-price [products n]
  (vec (take n (reverse (sort-by-price products)))))

(defn bottom-n-by-cost [products n]
  (vec (take n (sort-by-cost products))))

(defn valid-product? [p]
  (and (> (product-id p) 0) (not= (product-name p) "") (not= (product-sku p) "") (>= (product-unit-cost p) 0) (> (product-unit-price p) 0) (> (product-supplier-id p) 0) (> (product-category-id p) 0)))

(defn products-needing-review [products min-margin]
  (filterv (fn [p] (and (product-active p) (< (product-margin p) min-margin))) products))

(defrecord PriceTier [min-qty discount-pct])

(defn pricetier-min-qty [r] (:min-qty r))

(defn pricetier-discount-pct [r] (:discount-pct r))

(defn make-standard-tiers []
  [(->PriceTier 1 0) (->PriceTier 10 5) (->PriceTier 50 10) (->PriceTier 100 15) (->PriceTier 500 20)])

(defn applicable-tier [tiers qty]
  (let [valid (filterv (fn [t] (<= (pricetier-min-qty t) qty)) tiers)
   sorted (reverse (sort-by pricetier-min-qty valid))]
  (first sorted)))

(defn quantity-price [p tiers qty]
  (let [tier (applicable-tier tiers qty)
   base (product-unit-price p)
   disc (if (nil? tier) 0 (pricetier-discount-pct tier))]
  (* qty (- base (quot (* base disc) 100)))))

(defn quantity-discount [p tiers qty]
  (let [full-price (* (product-unit-price p) qty)
   discounted (quantity-price p tiers qty)]
  (- full-price discounted)))

(defn quantity-unit-price [p tiers qty]
  (let [tier (applicable-tier tiers qty)
   base (product-unit-price p)
   disc (if (nil? tier) 0 (pricetier-discount-pct tier))]
  (- base (quot (* base disc) 100))))

(defn cheaper-product [p1 p2]
  (if (<= (product-unit-price p1) (product-unit-price p2)) p1 p2))

(defn higher-margin-product [p1 p2]
  (if (>= (product-margin-pct p1) (product-margin-pct p2)) p1 p2))

(defn product-price-diff [p1 p2]
  (- (product-unit-price p1) (product-unit-price p2)))

(defn products-within-price-of [products target range]
  (let [target-price (product-unit-price target)]
  (filterv (fn [p] (and (not= (product-id p) (product-id target)) (<= (- (product-unit-price p) target-price) range) (>= (- (product-unit-price p) target-price) (- 0 range)))) products)))

(defn supplier-avg-product-margin [products sup-id]
  (let [sup-prods (products-by-supplier products sup-id)
   cnt (count sup-prods)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc p] (+ acc (product-margin-pct p))) 0 sup-prods) cnt))))

(defn supplier-score [supplier products]
  (let [sup-id (supplier-id supplier)
   avg-margin (supplier-avg-product-margin products sup-id)
   lead-penalty (supplier-lead-time-days supplier)
   prod-count (supplier-product-count products sup-id)]
  (+ (* avg-margin 2) (* prod-count 5) (- 0 (* lead-penalty 3)))))

(defn rank-suppliers [suppliers products]
  (reverse (sort-by (fn [s] (supplier-score s products)) suppliers)))

(defn price-spread [products]
  (let [prices (mapv product-unit-price products)]
  (if (empty? prices) 0 (- (apply max prices) (apply min prices)))))

(defn cost-spread [products]
  (let [costs (mapv product-unit-cost products)]
  (if (empty? costs) 0 (- (apply max costs) (apply min costs)))))

(defn catalog-margin-distribution [products]
  (let [low (count (filterv (fn [p] (< (product-margin-pct p) 20)) products))
   mid (count (filterv (fn [p] (and (>= (product-margin-pct p) 20) (< (product-margin-pct p) 40))) products))
   high (count (filterv (fn [p] (>= (product-margin-pct p) 40)) products))]
  {:low low :mid mid :high high}))

(defn unique-category-ids [products]
  (distinct (mapv product-category-id products)))

(defn unique-supplier-ids [products]
  (distinct (mapv product-supplier-id products)))
