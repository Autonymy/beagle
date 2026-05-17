(ns promotions
  (:require [catalog :as cat]
            [customers :as cust]
            [orders :as ord]))

(defrecord Campaign [id name start-date end-date status])

(defn campaign-id [r] (:id r))

(defn campaign-name [r] (:name r))

(defn campaign-start-date [r] (:start-date r))

(defn campaign-end-date [r] (:end-date r))

(defn campaign-status [r] (:status r))

(defrecord Coupon [id campaign-id code discount-type discount-value min-order max-uses current-uses])

(defn coupon-id [r] (:id r))

(defn coupon-campaign-id [r] (:campaign-id r))

(defn coupon-code [r] (:code r))

(defn coupon-discount-type [r] (:discount-type r))

(defn coupon-discount-value [r] (:discount-value r))

(defn coupon-min-order [r] (:min-order r))

(defn coupon-max-uses [r] (:max-uses r))

(defn coupon-current-uses [r] (:current-uses r))

(defrecord PromotionRule [id campaign-id min-tier min-order-value category-id])

(defn promotionrule-id [r] (:id r))

(defn promotionrule-campaign-id [r] (:campaign-id r))

(defn promotionrule-min-tier [r] (:min-tier r))

(defn promotionrule-min-order-value [r] (:min-order-value r))

(defn promotionrule-category-id [r] (:category-id r))

(defn find-campaign-by-id [campaigns id]
  (first (filterv (fn [c] (= (campaign-id c) id)) campaigns)))

(defn campaign-active? [campaign now]
  (and (= (campaign-status campaign) "active") (<= (campaign-start-date campaign) now) (<= now (campaign-end-date campaign))))

(defn active-campaigns [campaigns now]
  (filterv (fn [c] (campaign-active? c now)) campaigns))

(defn activate-campaign [campaign]
  (->Campaign (campaign-id campaign) (campaign-name campaign) (campaign-start-date campaign) (campaign-end-date campaign) "active"))

(defn deactivate-campaign [campaign]
  (->Campaign (campaign-id campaign) (campaign-name campaign) (campaign-start-date campaign) (campaign-end-date campaign) "inactive"))

(defn campaigns-by-status [campaigns status]
  (filterv (fn [c] (= (campaign-status c) status)) campaigns))

(defn campaign-duration-days [campaign]
  (quot (- (campaign-end-date campaign) (campaign-start-date campaign)) 86400))

(defn find-coupon-by-code [coupons code]
  (first (filterv (fn [c] (= (coupon-code c) code)) coupons)))

(defn find-coupon-by-id [coupons id]
  (first (filterv (fn [c] (= (coupon-id c) id)) coupons)))

(defn coupons-for-campaign [coupons cid]
  (filterv (fn [c] (= (coupon-campaign-id c) cid)) coupons))

(defn coupon-valid? [coupon]
  (< (coupon-current-uses coupon) (coupon-max-uses coupon)))

(defn use-coupon [coupon]
  (if (coupon-valid? coupon) (->Coupon (coupon-id coupon) (coupon-campaign-id coupon) (coupon-code coupon) (coupon-discount-type coupon) (coupon-discount-value coupon) (coupon-min-order coupon) (coupon-max-uses coupon) (+ (coupon-current-uses coupon) 1)) coupon))

(defn coupon-usage-pct [coupon]
  (if (= (coupon-max-uses coupon) 0) 0 (quot (* (coupon-current-uses coupon) 100) (coupon-max-uses coupon))))

(defn coupon-discount-amount [coupon order-total]
  (let [raw (if (= (coupon-discount-type coupon) "percentage") (quot (* order-total (coupon-discount-value coupon)) 100) (coupon-discount-value coupon))]
  (if (> raw order-total) order-total raw)))

(defn best-coupon [coupons order-total]
  (let [valid (filterv coupon-valid? coupons)]
  (if (empty? valid) nil (reduce (fn [best c] (if (> (coupon-discount-amount c order-total) (coupon-discount-amount best order-total)) c best)) (first valid) (rest valid)))))

(defn find-rules-for-campaign [rules cid]
  (filterv (fn [r] (= (promotionrule-campaign-id r) cid)) rules))

(defn tier-qualifies? [customer-tier min-tier]
  (if (= min-tier "") true (let [tier-val (cond
  (= customer-tier "gold") 3
  (= customer-tier "silver") 2
  (= customer-tier "bronze") 1
  true 0)
   min-val (cond
  (= min-tier "gold") 3
  (= min-tier "silver") 2
  (= min-tier "bronze") 1
  true 0)]
  (>= tier-val min-val))))

(defn customer-eligible? [rule customer]
  (tier-qualifies? (cust/customer-tier customer) (promotionrule-min-tier rule)))

(defn order-eligible? [rule order]
  (>= (ord/order-total order) (promotionrule-min-order-value rule)))

(defn rule-applies? [rule customer order]
  (and (customer-eligible? rule customer) (order-eligible? rule order)))

(defn total-discount-given [coupons orders]
  (reduce (fn [acc o] (+ acc (ord/order-discount o))) 0 orders))

(defn campaign-coupon-count [coupons cid]
  (count (coupons-for-campaign coupons cid)))

(defn campaign-total-uses [coupons cid]
  (reduce (fn [acc c] (+ acc (coupon-current-uses c))) 0 (coupons-for-campaign coupons cid)))

(defn campaign-total-discount [coupons cid order-total-fn]
  (let [camp-coupons (coupons-for-campaign coupons cid)]
  (reduce (fn [acc c] (+ acc (* (coupon-current-uses c) (coupon-discount-amount c 10000)))) 0 camp-coupons)))

(defn coupon-remaining-uses [coupon]
  (let [rem (- (coupon-max-uses coupon) (coupon-current-uses coupon))]
  (if (< rem 0) 0 rem)))

(defn coupon-exhausted? [coupon]
  (>= (coupon-current-uses coupon) (coupon-max-uses coupon)))

(defn exhausted-coupons [coupons]
  (filterv (fn [c] (coupon-exhausted? c)) coupons))

(defn valid-coupons [coupons]
  (filterv coupon-valid? coupons))

(defn coupon-applicable? [coupon order-total]
  (and (coupon-valid? coupon) (>= order-total (coupon-min-order coupon))))

(defn applicable-coupons [coupons order-total]
  (filterv (fn [c] (coupon-applicable? c order-total)) coupons))

(defn reset-coupon-uses [coupon]
  (->Coupon (coupon-id coupon) (coupon-campaign-id coupon) (coupon-code coupon) (coupon-discount-type coupon) (coupon-discount-value coupon) (coupon-min-order coupon) (coupon-max-uses coupon) 0))

(defn percentage-coupons [coupons]
  (filterv (fn [c] (= (coupon-discount-type c) "percentage")) coupons))

(defn fixed-coupons [coupons]
  (filterv (fn [c] (= (coupon-discount-type c) "fixed")) coupons))

(defn campaign-upcoming? [campaign now]
  (and (= (campaign-status campaign) "active") (> (campaign-start-date campaign) now)))

(defn campaign-expired? [campaign now]
  (< (campaign-end-date campaign) now))

(defn upcoming-campaigns [campaigns now]
  (filterv (fn [c] (campaign-upcoming? c now)) campaigns))

(defn expired-campaigns [campaigns now]
  (filterv (fn [c] (campaign-expired? c now)) campaigns))

(defn campaign-days-remaining [campaign now]
  (let [diff (- (campaign-end-date campaign) now)]
  (if (<= diff 0) 0 (quot diff 86400))))

(defn campaign-days-elapsed [campaign now]
  (let [diff (- now (campaign-start-date campaign))]
  (if (<= diff 0) 0 (quot diff 86400))))

(defn campaign-progress-pct [campaign now]
  (let [total (- (campaign-end-date campaign) (campaign-start-date campaign))
   elapsed (- now (campaign-start-date campaign))]
  (cond
  (<= total 0) 100
  (<= elapsed 0) 0
  (>= elapsed total) 100
  true (quot (* elapsed 100) total))))

(defn sort-campaigns-by-start [campaigns]
  (sort-by campaign-start-date campaigns))

(defn sort-campaigns-by-end [campaigns]
  (sort-by campaign-end-date campaigns))

(defn total-campaign-count [campaigns]
  (count campaigns))

(defn campaign-count-by-status [campaigns status]
  (count (campaigns-by-status campaigns status)))

(defn rules-for-category [rules cat-id]
  (filterv (fn [r] (= (promotionrule-category-id r) cat-id)) rules))

(defn universal-rules [rules]
  (filterv (fn [r] (= (promotionrule-category-id r) 0)) rules))

(defn category-specific-rules [rules]
  (filterv (fn [r] (not= (promotionrule-category-id r) 0)) rules))

(defn any-rule-applies? [rules customer order]
  (some (fn [r] (rule-applies? r customer order)) rules))

(defn matching-rules [rules customer order]
  (filterv (fn [r] (rule-applies? r customer order)) rules))

(defn campaign-rule-count [rules cid]
  (count (find-rules-for-campaign rules cid)))

(defn eligible-campaigns [campaigns rules customer order now]
  (let [active (active-campaigns campaigns now)]
  (filterv (fn [camp] (let [camp-rules (find-rules-for-campaign rules (campaign-id camp))]
  (any-rule-applies? camp-rules customer order))) active)))

(defn eligible-campaign-count [campaigns rules customer order now]
  (count (eligible-campaigns campaigns rules customer order now)))

(defn best-eligible-coupon [campaigns coupons rules customer order now]
  (let [elig-camps (eligible-campaigns campaigns rules customer order now)
   camp-ids (mapv campaign-id elig-camps)
   camp-coupons (filterv (fn [c] (some (fn [cid] (= cid (coupon-campaign-id c))) camp-ids)) coupons)
   order-total (ord/order-total order)
   usable (filterv (fn [c] (coupon-applicable? c order-total)) camp-coupons)]
  (if (empty? usable) nil (reduce (fn [best c] (if (> (coupon-discount-amount c order-total) (coupon-discount-amount best order-total)) c best)) (first usable) (rest usable)))))

(defn valid-campaign? [campaign]
  (and (> (campaign-id campaign) 0) (not= (campaign-name campaign) "") (< (campaign-start-date campaign) (campaign-end-date campaign))))

(defn valid-coupon? [coupon]
  (and (> (coupon-id coupon) 0) (not= (coupon-code coupon) "") (> (coupon-discount-value coupon) 0) (>= (coupon-min-order coupon) 0) (> (coupon-max-uses coupon) 0) (>= (coupon-current-uses coupon) 0)))

(defn valid-rule? [rule]
  (and (> (promotionrule-id rule) 0) (>= (promotionrule-min-order-value rule) 0) (>= (promotionrule-category-id rule) 0)))

(defn campaign-summary [campaign]
  (str (campaign-name campaign) " [" (campaign-status campaign) "]" " (" (campaign-start-date campaign) " - " (campaign-end-date campaign) ")"))

(defn campaign-detail [campaign]
  (str "Campaign #" (campaign-id campaign) " | " (campaign-name campaign) " | Status: " (campaign-status campaign) " | Duration: " (campaign-duration-days campaign) " days"))

(defn coupon-summary [coupon]
  (str (coupon-code coupon) " (" (coupon-discount-type coupon) " " (coupon-discount-value coupon) ")" " uses: " (coupon-current-uses coupon) "/" (coupon-max-uses coupon)))

(defn coupon-detail [coupon]
  (str "Coupon #" (coupon-id coupon) " | Code: " (coupon-code coupon) " | Type: " (coupon-discount-type coupon) " | Value: " (coupon-discount-value coupon) " | Min-order: " (coupon-min-order coupon) " | Uses: " (coupon-current-uses coupon) "/" (coupon-max-uses coupon) " | Campaign: " (coupon-campaign-id coupon)))

(defn rule-summary [rule]
  (str "Rule #" (promotionrule-id rule) " | Campaign: " (promotionrule-campaign-id rule) " | Min-tier: " (if (= (promotionrule-min-tier rule) "") "any" (promotionrule-min-tier rule)) " | Min-order: " (promotionrule-min-order-value rule) " | Category: " (if (= (promotionrule-category-id rule) 0) "any" (str (promotionrule-category-id rule)))))

(defn sort-coupons-by-value [coupons]
  (reverse (sort-by coupon-discount-value coupons)))

(defn sort-coupons-by-usage [coupons]
  (reverse (sort-by coupon-current-uses coupons)))

(defn sort-coupons-by-remaining [coupons]
  (sort-by coupon-remaining-uses coupons))

(defn sort-rules-by-min-order [rules]
  (reverse (sort-by promotionrule-min-order-value rules)))

(defn total-coupon-uses [coupons]
  (reduce (fn [acc c] (+ acc (coupon-current-uses c))) 0 coupons))

(defn total-coupon-capacity [coupons]
  (reduce (fn [acc c] (+ acc (coupon-max-uses c))) 0 coupons))

(defn overall-coupon-usage-pct [coupons]
  (let [cap (total-coupon-capacity coupons)]
  (if (= cap 0) 0 (quot (* (total-coupon-uses coupons) 100) cap))))

(defn avg-coupon-discount-value [coupons]
  (let [cnt (count coupons)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc c] (+ acc (coupon-discount-value c))) 0 coupons) cnt))))

(defn avg-coupon-uses [coupons]
  (let [cnt (count coupons)]
  (if (= cnt 0) 0 (quot (total-coupon-uses coupons) cnt))))

(defn most-used-coupon [coupons]
  (if (empty? coupons) nil (reduce (fn [best c] (if (> (coupon-current-uses c) (coupon-current-uses best)) c best)) (first coupons) (rest coupons))))

(defn nearly-exhausted-coupon [coupons]
  (let [valid (valid-coupons coupons)]
  (if (empty? valid) nil (reduce (fn [best c] (if (< (coupon-remaining-uses c) (coupon-remaining-uses best)) c best)) (first valid) (rest valid)))))

(defn total-campaign-uses [campaigns coupons]
  (reduce (fn [acc camp] (+ acc (campaign-total-uses coupons (campaign-id camp)))) 0 campaigns))

(defn avg-uses-per-campaign [campaigns coupons]
  (let [cnt (count campaigns)]
  (if (= cnt 0) 0 (quot (total-campaign-uses campaigns coupons) cnt))))

(defn most-popular-campaign [campaigns coupons]
  (if (empty? campaigns) nil (reduce (fn [best camp] (if (> (campaign-total-uses coupons (campaign-id camp)) (campaign-total-uses coupons (campaign-id best))) camp best)) (first campaigns) (rest campaigns))))

(defn campaign-with-most-coupons [campaigns coupons]
  (if (empty? campaigns) nil (reduce (fn [best camp] (if (> (campaign-coupon-count coupons (campaign-id camp)) (campaign-coupon-count coupons (campaign-id best))) camp best)) (first campaigns) (rest campaigns))))

(defn total-rule-count [rules]
  (count rules))

(defn avg-rule-min-order [rules]
  (let [cnt (count rules)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc r] (+ acc (promotionrule-min-order-value r))) 0 rules) cnt))))

(defn max-rule-min-order [rules]
  (if (empty? rules) 0 (reduce (fn [best r] (if (> (promotionrule-min-order-value r) best) (promotionrule-min-order-value r) best)) 0 rules)))

(defn rule-count-by-tier [rules tier]
  (count (filterv (fn [r] (= (promotionrule-min-tier r) tier)) rules)))

(defn customer-total-discount [orders cust-id]
  (let [cust-orders (ord/orders-by-customer orders cust-id)]
  (reduce (fn [acc o] (+ acc (ord/order-discount o))) 0 cust-orders)))

(defn customer-avg-discount [orders cust-id]
  (let [cust-orders (ord/orders-by-customer orders cust-id)
   cnt (count cust-orders)]
  (if (= cnt 0) 0 (quot (customer-total-discount orders cust-id) cnt))))

(defn customer-discount-pct [orders cust-id]
  (let [cust-orders (ord/orders-by-customer orders cust-id)
   subtotal-sum (reduce (fn [acc o] (+ acc (ord/order-subtotal o))) 0 cust-orders)
   discount-sum (customer-total-discount orders cust-id)]
  (if (= subtotal-sum 0) 0 (quot (* discount-sum 100) subtotal-sum))))

(defn orders-with-discount-above [orders threshold]
  (filterv (fn [o] (> (ord/order-discount o) threshold)) orders))

(defn discounted-order-count [orders]
  (count (filterv (fn [o] (> (ord/order-discount o) 0)) orders)))

(defn discount-frequency-pct [orders]
  (let [total (count orders)]
  (if (= total 0) 0 (quot (* (discounted-order-count orders) 100) total))))

(defn avg-discount-per-discounted-order [orders]
  (let [discounted (filterv (fn [o] (> (ord/order-discount o) 0)) orders)
   cnt (count discounted)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc o] (+ acc (ord/order-discount o))) 0 discounted) cnt))))

(defn coupons-for-category [coupons rules cat-id]
  (let [cat-rules (rules-for-category rules cat-id)
   camp-ids (distinct (mapv promotionrule-campaign-id cat-rules))]
  (filterv (fn [c] (some (fn [cid] (= cid (coupon-campaign-id c))) camp-ids)) coupons)))

(defn category-campaign-count [campaigns rules cat-id now]
  (let [cat-rules (rules-for-category rules cat-id)
   camp-ids (distinct (mapv promotionrule-campaign-id cat-rules))
   active (active-campaigns campaigns now)]
  (count (filterv (fn [camp] (some (fn [cid] (= cid (campaign-id camp))) camp-ids)) active))))

(defn customers-qualifying-for-tier [customers min-tier]
  (filterv (fn [c] (tier-qualifies? (cust/customer-tier c) min-tier)) customers))

(defn rule-eligible-customer-count [rule customers]
  (count (customers-qualifying-for-tier customers (promotionrule-min-tier rule))))

(defn simulate-coupon [coupon order-total]
  (let [disc (coupon-discount-amount coupon order-total)
   final (- order-total disc)]
  {:original order-total :discount disc :final final}))

(defn simulate-all-coupons [coupons order-total]
  (mapv (fn [c] (let [disc (coupon-discount-amount c order-total)]
  {:coupon-id (coupon-id c) :code (coupon-code c) :discount disc :final (- order-total disc)})) (valid-coupons coupons)))

(defn bulk-use-coupons [coupons]
  (mapv (fn [c] (if (coupon-valid? c) (use-coupon c) c)) coupons))

(defn deactivate-expired [campaigns now]
  (mapv (fn [c] (if (and (= (campaign-status c) "active") (< (campaign-end-date c) now)) (deactivate-campaign c) c)) campaigns))

(defn reset-campaign-coupons [coupons cid]
  (mapv (fn [c] (if (= (coupon-campaign-id c) cid) (reset-coupon-uses c) c)) coupons))

(defn promotions-dashboard [campaigns coupons rules orders now]
  (let [active (active-campaigns campaigns now)
   total-uses (total-coupon-uses coupons)
   total-cap (total-coupon-capacity coupons)
   total-disc (total-discount-given coupons orders)]
  {:active-campaigns (count active) :total-campaigns (count campaigns) :total-coupons (count coupons) :valid-coupons (count (valid-coupons coupons)) :exhausted-coupons (count (exhausted-coupons coupons)) :total-uses total-uses :total-capacity total-cap :usage-pct (if (= total-cap 0) 0 (quot (* total-uses 100) total-cap)) :total-discount total-disc :total-rules (count rules)}))

(defn campaign-breakdown [campaigns coupons rules]
  (mapv (fn [camp] (let [cid (campaign-id camp)
   camp-coupons (coupons-for-campaign coupons cid)
   camp-rules (find-rules-for-campaign rules cid)]
  {:campaign-id cid :name (campaign-name camp) :status (campaign-status camp) :coupon-count (count camp-coupons) :rule-count (count camp-rules) :total-uses (campaign-total-uses coupons cid) :valid-coupons (count (valid-coupons camp-coupons)) :exhausted-coupons (count (exhausted-coupons camp-coupons))})) campaigns))

(defn coupon-effectiveness-report [coupons]
  (mapv (fn [c] {:coupon-id (coupon-id c) :code (coupon-code c) :type (coupon-discount-type c) :value (coupon-discount-value c) :uses (coupon-current-uses c) :max-uses (coupon-max-uses c) :usage-pct (coupon-usage-pct c) :remaining (coupon-remaining-uses c) :exhausted (coupon-exhausted? c)}) coupons))

(defn tier-distribution [rules]
  {:any (rule-count-by-tier rules "") :bronze (rule-count-by-tier rules "bronze") :silver (rule-count-by-tier rules "silver") :gold (rule-count-by-tier rules "gold")})

(defn top-coupons-by-usage [coupons n]
  (vec (take n (reverse (sort-by coupon-current-uses coupons)))))

(defn top-coupons-by-value [coupons n]
  (vec (take n (sort-coupons-by-value coupons))))

(defn top-campaigns-by-uses [campaigns coupons n]
  (vec (take n (reverse (sort-by (fn [camp] (campaign-total-uses coupons (campaign-id camp))) campaigns)))))

(defn code-exists? [coupons code]
  (some? (find-coupon-by-code coupons code)))

(defn all-coupon-codes [coupons]
  (mapv coupon-code coupons))

(defn coupon-campaign-ids [coupons]
  (distinct (mapv coupon-campaign-id coupons)))

(defn coupon-count-by-type [coupons dtype]
  (count (filterv (fn [c] (= (coupon-discount-type c) dtype)) coupons)))

(defn discount-type-distribution [coupons]
  {:percentage (coupon-count-by-type coupons "percentage") :fixed (coupon-count-by-type coupons "fixed")})

(defn avg-discount-by-type [coupons dtype]
  (let [typed (filterv (fn [c] (= (coupon-discount-type c) dtype)) coupons)
   cnt (count typed)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc c] (+ acc (coupon-discount-value c))) 0 typed) cnt))))

(defn coupons-stackable? [c1 c2]
  (not= (coupon-campaign-id c1) (coupon-campaign-id c2)))

(defn stacked-discount [c1 c2 order-total]
  (let [d1 (coupon-discount-amount c1 order-total)
   d2 (coupon-discount-amount c2 order-total)
   raw (+ d1 d2)]
  (if (> raw order-total) order-total raw)))

(defn best-stacked-discount [coupons order-total]
  (let [valid (valid-coupons coupons)]
  (if (< (count valid) 2) 0 (reduce (fn [best-val c1] (reduce (fn [inner-best c2] (if (and (coupons-stackable? c1 c2) (> (stacked-discount c1 c2 order-total) inner-best)) (stacked-discount c1 c2 order-total) inner-best)) best-val valid)) 0 valid))))

(defn lowest-min-order [coupons]
  (let [valid (valid-coupons coupons)]
  (if (empty? valid) 0 (reduce (fn [best c] (if (< (coupon-min-order c) best) (coupon-min-order c) best)) (coupon-min-order (first valid)) (rest valid)))))

(defn highest-min-order [coupons]
  (let [valid (valid-coupons coupons)]
  (if (empty? valid) 0 (reduce (fn [best c] (if (> (coupon-min-order c) best) (coupon-min-order c) best)) (coupon-min-order (first valid)) (rest valid)))))

(defn avg-min-order [coupons]
  (let [cnt (count coupons)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc c] (+ acc (coupon-min-order c))) 0 coupons) cnt))))
