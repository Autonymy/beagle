(ns employees
  (:require [catalog :as cat]
            [orders :as ord]))

(defrecord Employee [id name department role hire-date active])

(defn employee-id [r] (:id r))

(defn employee-name [r] (:name r))

(defn employee-department [r] (:department r))

(defn employee-role [r] (:role r))

(defn employee-hire-date [r] (:hire-date r))

(defn employee-active [r] (:active r))

(defrecord CommissionRule [id employee-id category-id rate-pct])

(defn commissionrule-id [r] (:id r))

(defn commissionrule-employee-id [r] (:employee-id r))

(defn commissionrule-category-id [r] (:category-id r))

(defn commissionrule-rate-pct [r] (:rate-pct r))

(defrecord SalesTarget [id employee-id period target-amount actual-amount])

(defn salestarget-id [r] (:id r))

(defn salestarget-employee-id [r] (:employee-id r))

(defn salestarget-period [r] (:period r))

(defn salestarget-target-amount [r] (:target-amount r))

(defn salestarget-actual-amount [r] (:actual-amount r))

(defn find-employee-by-id [employees id]
  (first (filterv (fn [e] (= (employee-id e) id)) employees)))

(defn active-employees [employees]
  (filterv (fn [e] (employee-active e)) employees))

(defn employees-by-department [employees dept]
  (filterv (fn [e] (= (employee-department e) dept)) employees))

(defn employees-by-role [employees role]
  (filterv (fn [e] (= (employee-role e) role)) employees))

(defn employee-tenure-days [employee now]
  (quot (- now (employee-hire-date employee)) 86400))

(defn senior-employees [employees now min-days]
  (filterv (fn [e] (>= (employee-tenure-days e now) min-days)) employees))

(defn rules-for-employee [rules employee-id]
  (filterv (fn [r] (= (commissionrule-employee-id r) employee-id)) rules))

(defn commission-rate [rules employee-id category-id]
  (let [matches (filterv (fn [r] (and (= (commissionrule-employee-id r) employee-id) (= (commissionrule-category-id r) category-id))) rules)]
  (if (empty? matches) 0 (commissionrule-rate-pct (first matches)))))

(defn line-commission [rules employee-id product ol]
  (let [cat-id (cat/product-category-id product)
   rate (commission-rate rules employee-id cat-id)
   amount (ord/orderline-line-total ol)]
  (quot (* rate amount) 100)))

(defn order-commission [rules employee-id order products]
  (let [lines (ord/order-lines order)]
  (reduce (fn [acc ol] (let [prod-id (ord/orderline-product-id ol)
   product (cat/find-product-by-id products prod-id)]
  (if (nil? product) acc (+ acc (line-commission rules employee-id product ol))))) 0 lines)))

(defn total-commission [rules employee-id orders products]
  (reduce (fn [acc o] (+ acc (order-commission rules employee-id o products))) 0 orders))

(defn top-earner [rules employees orders products]
  (let [actives (active-employees employees)]
  (if (empty? actives) nil (reduce (fn [best e] (let [best-comm (total-commission rules (employee-id best) orders products)
   curr-comm (total-commission rules (employee-id e) orders products)]
  (if (> curr-comm best-comm) e best))) (first actives) (rest actives)))))

(defn targets-for-employee [targets employee-id]
  (filterv (fn [t] (= (salestarget-employee-id t) employee-id)) targets))

(defn target-for-period [targets employee-id period]
  (first (filterv (fn [t] (and (= (salestarget-employee-id t) employee-id) (= (salestarget-period t) period))) targets)))

(defn target-achievement-pct [target]
  (let [ta (salestarget-target-amount target)]
  (if (= ta 0) 0 (quot (* (salestarget-actual-amount target) 100) ta))))

(defn on-target? [target]
  (>= (target-achievement-pct target) 100))

(defn targets-met-count [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)]
  (count (filterv (fn [t] (on-target? t)) emp-targets))))

(defn update-actual [target amount]
  (->SalesTarget (salestarget-id target) (salestarget-employee-id target) (salestarget-period target) (salestarget-target-amount target) amount))

(defn department-headcount [employees dept]
  (count (employees-by-department employees dept)))

(defn department-total-commission [rules employees dept orders products]
  (let [dept-emps (employees-by-department employees dept)]
  (reduce (fn [acc e] (+ acc (total-commission rules (employee-id e) orders products))) 0 dept-emps)))

(defn avg-commission-per-employee [rules employees orders products]
  (let [actives (active-employees employees)
   cnt (count actives)]
  (if (= cnt 0) 0 (let [total (reduce (fn [acc e] (+ acc (total-commission rules (employee-id e) orders products))) 0 actives)]
  (quot total cnt)))))

(defn employee-summary [e]
  (str (employee-name e) " (" (employee-department e) "/" (employee-role e) ")"))

(defn employee-detail [e now]
  (str (employee-name e) " | Dept: " (employee-department e) " | Role: " (employee-role e) " | Tenure: " (employee-tenure-days e now) " days" " | Active: " (employee-active e)))

(defn valid-employee? [e]
  (and (> (employee-id e) 0) (not= (employee-name e) "") (not= (employee-department e) "") (not= (employee-role e) "")))

(defn valid-commission-rule? [r]
  (and (> (commissionrule-id r) 0) (> (commissionrule-employee-id r) 0) (> (commissionrule-category-id r) 0) (> (commissionrule-rate-pct r) 0) (<= (commissionrule-rate-pct r) 100)))

(defn valid-sales-target? [t]
  (and (> (salestarget-id t) 0) (> (salestarget-employee-id t) 0) (not= (salestarget-period t) "") (> (salestarget-target-amount t) 0) (>= (salestarget-actual-amount t) 0)))

(defn employee-commission-breakdown [rules employee-id orders products]
  (let [emp-rules (rules-for-employee rules employee-id)]
  (mapv (fn [r] (let [cat-id (commissionrule-category-id r)
   rate (commissionrule-rate-pct r)
   cat-commission (reduce (fn [acc o] (let [lines (ord/order-lines o)]
  (+ acc (reduce (fn [a ol] (let [prod-id (ord/orderline-product-id ol)
   product (cat/find-product-by-id products prod-id)]
  (if (nil? product) a (if (= (cat/product-category-id product) cat-id) (+ a (quot (* rate (ord/orderline-line-total ol)) 100)) a)))) 0 lines)))) 0 orders)]
  {:category-id cat-id :rate rate :commission cat-commission})) emp-rules)))

(defn employee-top-category [rules employee-id orders products]
  (let [breakdown (employee-commission-breakdown rules employee-id orders products)]
  (if (empty? breakdown) 0 (let [best (reduce (fn [b item] (if (> (:commission item) (:commission b)) item b)) (first breakdown) (rest breakdown))]
  (:category-id best)))))

(defn employee-target-summary [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)
   total-target (reduce (fn [acc t] (+ acc (salestarget-target-amount t))) 0 emp-targets)
   total-actual (reduce (fn [acc t] (+ acc (salestarget-actual-amount t))) 0 emp-targets)
   met (targets-met-count targets employee-id)]
  {:total-target total-target :total-actual total-actual :targets-met met :total-targets (count emp-targets)}))

(defn best-performing-period [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)]
  (if (empty? emp-targets) nil (let [best (reduce (fn [b t] (if (> (target-achievement-pct t) (target-achievement-pct b)) t b)) (first emp-targets) (rest emp-targets))]
  (salestarget-period best)))))

(defn worst-performing-period [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)]
  (if (empty? emp-targets) nil (let [worst (reduce (fn [b t] (if (< (target-achievement-pct t) (target-achievement-pct b)) t b)) (first emp-targets) (rest emp-targets))]
  (salestarget-period worst)))))

(defn department-avg-tenure [employees dept now]
  (let [dept-emps (employees-by-department employees dept)
   cnt (count dept-emps)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc e] (+ acc (employee-tenure-days e now))) 0 dept-emps) cnt))))

(defn department-active-count [employees dept]
  (count (filterv (fn [e] (and (= (employee-department e) dept) (employee-active e))) employees)))

(defn department-active-pct [employees dept]
  (let [total (department-headcount employees dept)]
  (if (= total 0) 0 (quot (* (department-active-count employees dept) 100) total))))

(defn deactivate-employee [employees target-id]
  (mapv (fn [e] (if (= (employee-id e) target-id) (->Employee (employee-id e) (employee-name e) (employee-department e) (employee-role e) (employee-hire-date e) false) e)) employees))

(defn activate-employee [employees target-id]
  (mapv (fn [e] (if (= (employee-id e) target-id) (->Employee (employee-id e) (employee-name e) (employee-department e) (employee-role e) (employee-hire-date e) true) e)) employees))

(defn change-department [employee new-dept]
  (->Employee (employee-id employee) (employee-name employee) new-dept (employee-role employee) (employee-hire-date employee) (employee-active employee)))

(defn change-role [employee new-role]
  (->Employee (employee-id employee) (employee-name employee) (employee-department employee) new-role (employee-hire-date employee) (employee-active employee)))

(defn add-commission-rule [rules rule]
  (conj rules rule))

(defn remove-commission-rule [rules rule-id]
  (filterv (fn [r] (not= (commissionrule-id r) rule-id)) rules))

(defn update-commission-rate [rules rule-id new-rate]
  (mapv (fn [r] (if (= (commissionrule-id r) rule-id) (->CommissionRule (commissionrule-id r) (commissionrule-employee-id r) (commissionrule-category-id r) new-rate) r)) rules))

(defn employee-order-count [employee-id orders]
  (count orders))

(defn employee-revenue [rules employee-id orders products]
  (total-commission rules employee-id orders products))

(defn rank-by-commission [rules employees orders products]
  (let [actives (active-employees employees)]
  (reverse (sort-by (fn [e] (total-commission rules (employee-id e) orders products)) actives))))

(defn rank-by-tenure [employees now]
  (reverse (sort-by (fn [e] (employee-tenure-days e now)) employees)))

(defn commission-leaderboard [rules employees orders products]
  (let [ranked (rank-by-commission rules employees orders products)]
  (mapv (fn [e] {:employee-id (employee-id e) :name (employee-name e) :commission (total-commission rules (employee-id e) orders products)}) ranked)))

(defn total-headcount [employees]
  (count employees))

(defn active-headcount [employees]
  (count (active-employees employees)))

(defn turnover-rate-pct [employees]
  (let [total (total-headcount employees)]
  (if (= total 0) 0 (let [inactive (- total (active-headcount employees))]
  (quot (* inactive 100) total)))))

(defn unique-departments [employees]
  (distinct (mapv employee-department employees)))

(defn unique-roles [employees]
  (distinct (mapv employee-role employees)))

(defn department-roster [employees dept]
  (let [dept-emps (employees-by-department employees dept)]
  (mapv (fn [e] {:id (employee-id e) :name (employee-name e) :role (employee-role e) :active (employee-active e)}) dept-emps)))

(defn target-gap [target]
  (let [diff (- (salestarget-target-amount target) (salestarget-actual-amount target))]
  (if (> diff 0) diff 0)))

(defn target-surplus [target]
  (let [diff (- (salestarget-actual-amount target) (salestarget-target-amount target))]
  (if (> diff 0) diff 0)))

(defn employee-total-target [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)]
  (reduce (fn [acc t] (+ acc (salestarget-target-amount t))) 0 emp-targets)))

(defn employee-total-actual [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)]
  (reduce (fn [acc t] (+ acc (salestarget-actual-amount t))) 0 emp-targets)))

(defn employee-total-gap [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)]
  (reduce (fn [acc t] (+ acc (target-gap t))) 0 emp-targets)))

(defn employee-total-surplus [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)]
  (reduce (fn [acc t] (+ acc (target-surplus t))) 0 emp-targets)))

(defn employee-overall-achievement-pct [targets employee-id]
  (let [total-tgt (employee-total-target targets employee-id)]
  (if (= total-tgt 0) 0 (quot (* (employee-total-actual targets employee-id) 100) total-tgt))))

(defn employees-above-commission [rules employees orders products threshold]
  (filterv (fn [e] (>= (total-commission rules (employee-id e) orders products) threshold)) (active-employees employees)))

(defn employees-below-commission [rules employees orders products threshold]
  (filterv (fn [e] (< (total-commission rules (employee-id e) orders products) threshold)) (active-employees employees)))

(defn commission-spread [rules employees orders products]
  (let [actives (active-employees employees)]
  (if (empty? actives) 0 (let [commissions (mapv (fn [e] (total-commission rules (employee-id e) orders products)) actives)
   max-c (apply max commissions)
   min-c (apply min commissions)]
  (- max-c min-c)))))

(defn median-commission [rules employees orders products]
  (let [actives (active-employees employees)]
  (if (empty? actives) 0 (let [commissions (sort (mapv (fn [e] (total-commission rules (employee-id e) orders products)) actives))
   cnt (count commissions)
   mid (quot cnt 2)]
  (nth commissions mid)))))

(defn category-total-commission [rules employees category-id orders products]
  (let [actives (active-employees employees)]
  (reduce (fn [acc e] (let [rate (commission-rate rules (employee-id e) category-id)]
  (+ acc (reduce (fn [a o] (let [lines (ord/order-lines o)]
  (+ a (reduce (fn [inner ol] (let [prod-id (ord/orderline-product-id ol)
   product (cat/find-product-by-id products prod-id)]
  (if (nil? product) inner (if (= (cat/product-category-id product) category-id) (+ inner (quot (* rate (ord/orderline-line-total ol)) 100)) inner)))) 0 lines)))) 0 orders)))) 0 actives)))

(defn category-avg-commission-rate [rules category-id]
  (let [matching (filterv (fn [r] (= (commissionrule-category-id r) category-id)) rules)
   cnt (count matching)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc r] (+ acc (commissionrule-rate-pct r))) 0 matching) cnt))))

(defn categories-with-rules [rules]
  (distinct (mapv commissionrule-category-id rules)))

(defn rules-for-category [rules category-id]
  (filterv (fn [r] (= (commissionrule-category-id r) category-id)) rules))

(defn category-rule-count [rules category-id]
  (count (rules-for-category rules category-id)))

(defn performance-score [rules targets employee-id orders products]
  (let [comm (total-commission rules employee-id orders products)
   achievement (employee-overall-achievement-pct targets employee-id)
   met (targets-met-count targets employee-id)]
  (+ (quot comm 100) (* achievement 2) (* met 500))))

(defn rank-by-performance [rules targets employees orders products]
  (let [actives (active-employees employees)]
  (reverse (sort-by (fn [e] (performance-score rules targets (employee-id e) orders products)) actives))))

(defn top-performers [rules targets employees orders products n]
  (let [ranked (rank-by-performance rules targets employees orders products)]
  (vec (take n ranked))))

(defn bottom-performers [rules targets employees orders products n]
  (let [ranked (rank-by-performance rules targets employees orders products)]
  (vec (take n (reverse ranked)))))

(defn performance-report [rules targets employees orders products]
  (let [actives (active-employees employees)]
  (mapv (fn [e] (let [eid (employee-id e)]
  {:employee-id eid :name (employee-name e) :department (employee-department e) :commission (total-commission rules eid orders products) :achievement-pct (employee-overall-achievement-pct targets eid) :targets-met (targets-met-count targets eid) :score (performance-score rules targets eid orders products)})) actives)))

(defn department-commission-breakdown [rules employees dept orders products]
  (let [dept-emps (employees-by-department employees dept)]
  (mapv (fn [e] {:employee-id (employee-id e) :name (employee-name e) :commission (total-commission rules (employee-id e) orders products)}) dept-emps)))

(defn department-avg-commission [rules employees dept orders products]
  (let [dept-emps (employees-by-department employees dept)
   cnt (count dept-emps)]
  (if (= cnt 0) 0 (quot (department-total-commission rules employees dept orders products) cnt))))

(defn department-commission-spread [rules employees dept orders products]
  (let [dept-emps (employees-by-department employees dept)]
  (if (empty? dept-emps) 0 (let [commissions (mapv (fn [e] (total-commission rules (employee-id e) orders products)) dept-emps)
   max-c (apply max commissions)
   min-c (apply min commissions)]
  (- max-c min-c)))))

(defn newest-employee [employees]
  (if (empty? employees) nil (reduce (fn [newest e] (if (> (employee-hire-date e) (employee-hire-date newest)) e newest)) (first employees) (rest employees))))

(defn oldest-employee [employees]
  (if (empty? employees) nil (reduce (fn [oldest e] (if (< (employee-hire-date e) (employee-hire-date oldest)) e oldest)) (first employees) (rest employees))))

(defn avg-tenure-days [employees now]
  (let [cnt (count employees)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc e] (+ acc (employee-tenure-days e now))) 0 employees) cnt))))

(defn tenure-spread-days [employees now]
  (if (empty? employees) 0 (let [tenures (mapv (fn [e] (employee-tenure-days e now)) employees)
   max-t (apply max tenures)
   min-t (apply min tenures)]
  (- max-t min-t))))

(defn employees-hired-in-period [employees start end]
  (filterv (fn [e] (and (>= (employee-hire-date e) start) (<= (employee-hire-date e) end))) employees))

(defn hire-count-in-period [employees start end]
  (count (employees-hired-in-period employees start end)))

(defn employee-order-revenue [rules employee-id order products]
  (order-commission rules employee-id order products))

(defn employee-avg-order-commission [rules employee-id orders products]
  (let [cnt (count orders)]
  (if (= cnt 0) 0 (quot (total-commission rules employee-id orders products) cnt))))

(defn employee-highest-commission-order [rules employee-id orders products]
  (if (empty? orders) nil (reduce (fn [best o] (let [best-comm (order-commission rules employee-id best products)
   curr-comm (order-commission rules employee-id o products)]
  (if (> curr-comm best-comm) o best))) (first orders) (rest orders))))

(defn employee-zero-commission-orders [rules employee-id orders products]
  (filterv (fn [o] (= (order-commission rules employee-id o products) 0)) orders))

(defn increment-actual [target amount]
  (->SalesTarget (salestarget-id target) (salestarget-employee-id target) (salestarget-period target) (salestarget-target-amount target) (+ (salestarget-actual-amount target) amount)))

(defn update-target-amount [target new-target]
  (->SalesTarget (salestarget-id target) (salestarget-employee-id target) (salestarget-period target) new-target (salestarget-actual-amount target)))

(defn remaining-to-target [target]
  (let [gap (target-gap target)]
  gap))

(defn target-completion-rate [targets employee-id]
  (let [emp-targets (targets-for-employee targets employee-id)
   cnt (count emp-targets)]
  (if (= cnt 0) 0 (quot (* (targets-met-count targets employee-id) 100) cnt))))

(defn employee-full-report [rules targets employee orders products now]
  (let [eid (employee-id employee)]
  {:employee-id eid :name (employee-name employee) :department (employee-department employee) :role (employee-role employee) :tenure-days (employee-tenure-days employee now) :active (employee-active employee) :total-commission (total-commission rules eid orders products) :achievement-pct (employee-overall-achievement-pct targets eid) :targets-met (targets-met-count targets eid) :performance-score (performance-score rules targets eid orders products)}))

(defn department-full-report [rules targets employees dept orders products now]
  (let [dept-emps (employees-by-department employees dept)]
  {:department dept :headcount (count dept-emps) :active-count (department-active-count employees dept) :avg-tenure (department-avg-tenure employees dept now) :total-commission (department-total-commission rules employees dept orders products) :avg-commission (department-avg-commission rules employees dept orders products)}))

(defn company-summary [rules targets employees orders products now]
  (let [actives (active-employees employees)
   total-comm (reduce (fn [acc e] (+ acc (total-commission rules (employee-id e) orders products))) 0 actives)
   total-met (reduce (fn [acc e] (+ acc (targets-met-count targets (employee-id e)))) 0 actives)]
  {:total-employees (count employees) :active-employees (count actives) :departments (count (unique-departments employees)) :total-commission total-comm :avg-commission (if (= (count actives) 0) 0 (quot total-comm (count actives))) :avg-tenure (avg-tenure-days employees now) :total-targets-met total-met :turnover-rate (turnover-rate-pct employees)}))
