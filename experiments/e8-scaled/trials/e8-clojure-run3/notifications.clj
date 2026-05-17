(ns notifications
  (:require [customers :as cust]
            [orders :as ord]
            [shipping :as ship]
            [billing :refer :all]))

(defrecord Template [id name channel subject body-pattern])

(defn template-id [r] (:id r))

(defn template-name [r] (:name r))

(defn template-channel [r] (:channel r))

(defn template-subject [r] (:subject r))

(defn template-body-pattern [r] (:body-pattern r))

(defrecord Notification [id template-id customer-id reference-id reference-type status created-at sent-at])

(defn notification-id [r] (:id r))

(defn notification-template-id [r] (:template-id r))

(defn notification-customer-id [r] (:customer-id r))

(defn notification-reference-id [r] (:reference-id r))

(defn notification-reference-type [r] (:reference-type r))

(defn notification-status [r] (:status r))

(defn notification-created-at [r] (:created-at r))

(defn notification-sent-at [r] (:sent-at r))

(defrecord Preference [customer-id channel enabled])

(defn preference-customer-id [r] (:customer-id r))

(defn preference-channel [r] (:channel r))

(defn preference-enabled [r] (:enabled r))

(defn find-template-by-id [templates id]
  (first (filterv (fn [t] (= (template-id t) id)) templates)))

(defn find-template-by-name [templates name]
  (first (filterv (fn [t] (= (template-name t) name)) templates)))

(defn templates-for-channel [templates channel]
  (filterv (fn [t] (= (template-channel t) channel)) templates))

(defn create-notification [id template customer reference-id reference-type created-at]
  (->Notification id (template-id template) (cust/customer-id customer) reference-id reference-type "pending" created-at 0))

(defn send-notification [n sent-at]
  (if (= (notification-status n) "pending") (->Notification (notification-id n) (notification-template-id n) (notification-customer-id n) (notification-reference-id n) (notification-reference-type n) "sent" (notification-created-at n) sent-at) n))

(defn fail-notification [n]
  (if (= (notification-status n) "pending") (->Notification (notification-id n) (notification-template-id n) (notification-customer-id n) (notification-reference-id n) (notification-reference-type n) "failed" (notification-created-at n) (notification-sent-at n)) n))

(defn retry-notification [n]
  (if (= (notification-status n) "failed") (->Notification (notification-id n) (notification-template-id n) (notification-customer-id n) (notification-reference-id n) (notification-reference-type n) "pending" (notification-created-at n) (notification-sent-at n)) n))

(defn find-notification-by-id [notifications id]
  (first (filterv (fn [n] (= (notification-id n) id)) notifications)))

(defn notifications-for-customer [notifications customer-id]
  (filterv (fn [n] (= (notification-customer-id n) customer-id)) notifications))

(defn notifications-by-status [notifications status]
  (filterv (fn [n] (= (notification-status n) status)) notifications))

(defn pending-notifications [notifications]
  (notifications-by-status notifications "pending"))

(defn sent-notifications [notifications]
  (notifications-by-status notifications "sent"))

(defn notifications-for-reference [notifications reference-id reference-type]
  (filterv (fn [n] (and (= (notification-reference-id n) reference-id) (= (notification-reference-type n) reference-type))) notifications))

(defn customer-preferences [prefs customer-id]
  (filterv (fn [p] (= (preference-customer-id p) customer-id)) prefs))

(defn channel-enabled? [prefs customer-id channel]
  (let [matching (filterv (fn [p] (and (= (preference-customer-id p) customer-id) (= (preference-channel p) channel))) prefs)]
  (if (empty? matching) true (preference-enabled (first matching)))))

(defn set-preference [prefs customer-id channel enabled]
  (let [exists (some (fn [p] (and (= (preference-customer-id p) customer-id) (= (preference-channel p) channel))) prefs)]
  (if exists (mapv (fn [p] (if (and (= (preference-customer-id p) customer-id) (= (preference-channel p) channel)) (->Preference customer-id channel enabled) p)) prefs) (conj prefs (->Preference customer-id channel enabled)))))

(defn delivery-rate-pct [notifications]
  (let [sent-count (count (notifications-by-status notifications "sent"))
   failed-count (count (notifications-by-status notifications "failed"))
   total (+ sent-count failed-count)]
  (if (= total 0) 100 (quot (* sent-count 100) total))))

(defn notifications-sent-count [notifications]
  (count (notifications-by-status notifications "sent")))

(defn notifications-failed-count [notifications]
  (count (notifications-by-status notifications "failed")))

(defn channel-delivery-rate [notifications templates channel]
  (let [channel-template-ids (mapv template-id (templates-for-channel templates channel))
   channel-notifs (filterv (fn [n] (some (fn [tid] (= (notification-template-id n) tid)) channel-template-ids)) notifications)]
  (delivery-rate-pct channel-notifs)))

(defn avg-send-time [notifications]
  (let [sent (notifications-by-status notifications "sent")
   cnt (count sent)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc n] (+ acc (- (notification-sent-at n) (notification-created-at n)))) 0 sent) cnt))))

(defn order-notification [id template order customer created-at]
  (->Notification id (template-id template) (cust/customer-id customer) (ord/order-id order) "order" "pending" created-at 0))

(defn shipment-notification [id template shipment customer created-at]
  (->Notification id (template-id template) (cust/customer-id customer) (ship/shipment-id shipment) "shipment" "pending" created-at 0))

(defn invoice-notification [id template invoice customer created-at]
  (->Notification id (template-id template) (cust/customer-id customer) (billing/invoice-id invoice) "invoice" "pending" created-at 0))

(defn should-notify? [prefs customer template]
  (channel-enabled? prefs (cust/customer-id customer) (template-channel template)))

(defn valid-template? [t]
  (and (> (template-id t) 0) (not= (template-name t) "") (not= (template-channel t) "") (not= (template-subject t) "") (not= (template-body-pattern t) "")))

(defn valid-notification? [n]
  (and (> (notification-id n) 0) (> (notification-template-id n) 0) (> (notification-customer-id n) 0) (> (notification-reference-id n) 0) (not= (notification-reference-type n) "")))

(defn sort-notifications-by-date [notifications]
  (sort-by notification-created-at notifications))

(defn sort-notifications-by-sent [notifications]
  (sort-by notification-sent-at notifications))

(defn notification-status-counts [notifications]
  (let [pending-count (count (pending-notifications notifications))
   sent-count (count (sent-notifications notifications))
   failed-count (count (notifications-by-status notifications "failed"))]
  {:pending pending-count :sent sent-count :failed failed-count}))

(defn notifications-by-reference-type [notifications ref-type]
  (filterv (fn [n] (= (notification-reference-type n) ref-type)) notifications))

(defn customer-notification-count [notifications customer-id]
  (count (notifications-for-customer notifications customer-id)))

(defn customer-sent-count [notifications customer-id]
  (count (filterv (fn [n] (and (= (notification-customer-id n) customer-id) (= (notification-status n) "sent"))) notifications)))

(defn template-notification-count [notifications template-id]
  (count (filterv (fn [n] (= (notification-template-id n) template-id)) notifications)))

(defn template-sent-count [notifications template-id]
  (count (filterv (fn [n] (and (= (notification-template-id n) template-id) (= (notification-status n) "sent"))) notifications)))

(defn template-delivery-rate [notifications template-id]
  (let [tmpl-notifs (filterv (fn [n] (= (notification-template-id n) template-id)) notifications)]
  (delivery-rate-pct tmpl-notifs)))

(defn send-all-pending [notifications sent-at]
  (mapv (fn [n] (if (= (notification-status n) "pending") (send-notification n sent-at) n)) notifications))

(defn retry-all-failed [notifications]
  (mapv (fn [n] (if (= (notification-status n) "failed") (retry-notification n) n)) notifications))

(defn notifications-in-period [notifications start end]
  (filterv (fn [n] (and (>= (notification-created-at n) start) (<= (notification-created-at n) end))) notifications))

(defn notification-count-in-period [notifications start end]
  (count (notifications-in-period notifications start end)))

(defn sent-in-period [notifications start end]
  (filterv (fn [n] (and (= (notification-status n) "sent") (>= (notification-sent-at n) start) (<= (notification-sent-at n) end))) notifications))

(defn channel-opt-in-count [prefs channel]
  (count (filterv (fn [p] (and (= (preference-channel p) channel) (preference-enabled p))) prefs)))

(defn channel-opt-out-count [prefs channel]
  (count (filterv (fn [p] (and (= (preference-channel p) channel) (not (preference-enabled p)))) prefs)))

(defn unique-channels [prefs]
  (distinct (mapv preference-channel prefs)))

(defn preference-summary [prefs]
  (let [channels (unique-channels prefs)]
  (reduce (fn [acc ch] (assoc acc ch (channel-opt-in-count prefs ch))) {} channels)))

(defn notification-summary [n]
  (str "Notification #" (notification-id n) " | Template: " (notification-template-id n) " | Customer: " (notification-customer-id n) " | " (notification-reference-type n) " #" (notification-reference-id n) " | " (notification-status n)))

(defn template-summary [t]
  (str "Template #" (template-id t) " | " (template-name t) " | Channel: " (template-channel t) " | Subject: " (template-subject t)))

(defn notification-detail [n]
  (str "Notification #" (notification-id n) " | Template: " (notification-template-id n) " | Customer: " (notification-customer-id n) " | Ref: " (notification-reference-type n) "#" (notification-reference-id n) " | Status: " (notification-status n) " | Created: " (notification-created-at n) " | Sent: " (notification-sent-at n)))

(defn customer-recent-notifications [notifications customer-id n]
  (let [cust-notifs (notifications-for-customer notifications customer-id)
   sorted (reverse (sort-by notification-created-at cust-notifs))]
  (vec (take n sorted))))

(defn customer-notification-summary [notifications customer-id]
  (let [cust-notifs (notifications-for-customer notifications customer-id)]
  {:total (count cust-notifs) :sent (count (filterv (fn [n] (= (notification-status n) "sent")) cust-notifs)) :pending (count (filterv (fn [n] (= (notification-status n) "pending")) cust-notifs)) :failed (count (filterv (fn [n] (= (notification-status n) "failed")) cust-notifs))}))

(defn order-notifications [notifications order-id]
  (notifications-for-reference notifications order-id "order"))

(defn shipment-notifications [notifications shipment-id]
  (notifications-for-reference notifications shipment-id "shipment"))

(defn invoice-notifications [notifications invoice-id]
  (notifications-for-reference notifications invoice-id "invoice"))

(defn failed-notifications [notifications]
  (notifications-by-status notifications "failed"))

(defn failed-for-customer [notifications customer-id]
  (filterv (fn [n] (and (= (notification-customer-id n) customer-id) (= (notification-status n) "failed"))) notifications))

(defn failure-rate-pct [notifications]
  (let [total (count notifications)]
  (if (= total 0) 0 (quot (* (notifications-failed-count notifications) 100) total))))

(defn fastest-notification [notifications]
  (let [sent (filterv (fn [n] (= (notification-status n) "sent")) notifications)]
  (if (empty? sent) nil (reduce (fn [best n] (let [best-time (- (notification-sent-at best) (notification-created-at best))
   n-time (- (notification-sent-at n) (notification-created-at n))]
  (if (< n-time best-time) n best))) (first sent) (rest sent)))))

(defn slowest-notification [notifications]
  (let [sent (filterv (fn [n] (= (notification-status n) "sent")) notifications)]
  (if (empty? sent) nil (reduce (fn [worst n] (let [worst-time (- (notification-sent-at worst) (notification-created-at worst))
   n-time (- (notification-sent-at n) (notification-created-at n))]
  (if (> n-time worst-time) n worst))) (first sent) (rest sent)))))

(defn notification-dashboard [notifications templates prefs]
  (let [total (count notifications)
   sent-cnt (notifications-sent-count notifications)
   failed-cnt (notifications-failed-count notifications)
   pending-cnt (count (pending-notifications notifications))
   del-rate (delivery-rate-pct notifications)
   avg-time (avg-send-time notifications)
   tmpl-count (count templates)
   pref-count (count prefs)]
  {:total-notifications total :sent sent-cnt :failed failed-cnt :pending pending-cnt :delivery-rate del-rate :avg-send-time avg-time :template-count tmpl-count :preference-count pref-count}))

(defn notifications-by-channel [notifications templates]
  (let [channels (distinct (mapv template-channel templates))]
  (reduce (fn [acc ch] (let [ch-tmpl-ids (mapv template-id (templates-for-channel templates ch))
   cnt (count (filterv (fn [n] (some (fn [tid] (= (notification-template-id n) tid)) ch-tmpl-ids)) notifications))]
  (assoc acc ch cnt))) {} channels)))

(defn channel-sent-count [notifications templates channel]
  (let [ch-tmpl-ids (mapv template-id (templates-for-channel templates channel))
   ch-sent (filterv (fn [n] (and (= (notification-status n) "sent") (some (fn [tid] (= (notification-template-id n) tid)) ch-tmpl-ids))) notifications)]
  (count ch-sent)))

(defn channel-failed-count [notifications templates channel]
  (let [ch-tmpl-ids (mapv template-id (templates-for-channel templates channel))
   ch-failed (filterv (fn [n] (and (= (notification-status n) "failed") (some (fn [tid] (= (notification-template-id n) tid)) ch-tmpl-ids))) notifications)]
  (count ch-failed)))

(defn reference-type-counts [notifications]
  (let [order-cnt (count (notifications-by-reference-type notifications "order"))
   shipment-cnt (count (notifications-by-reference-type notifications "shipment"))
   invoice-cnt (count (notifications-by-reference-type notifications "invoice"))]
  {:order order-cnt :shipment shipment-cnt :invoice invoice-cnt}))

(defn reference-type-delivery-rate [notifications ref-type]
  (delivery-rate-pct (notifications-by-reference-type notifications ref-type)))

(defn notification-age [n now]
  (- now (notification-created-at n)))

(defn notification-age-days [n now]
  (quot (notification-age n now) 86400))

(defn stale-pending [notifications now threshold-days]
  (let [max-seconds (* threshold-days 86400)]
  (filterv (fn [n] (and (= (notification-status n) "pending") (> (- now (notification-created-at n)) max-seconds))) notifications)))

(defn notification-report [notifications templates prefs]
  (let [total (count notifications)
   sent-cnt (notifications-sent-count notifications)
   failed-cnt (notifications-failed-count notifications)
   pending-cnt (count (pending-notifications notifications))
   del-rate (delivery-rate-pct notifications)
   avg-time (avg-send-time notifications)
   ref-counts (reference-type-counts notifications)
   status-counts (notification-status-counts notifications)
   tmpl-count (count templates)
   pref-count (count prefs)]
  {:total-notifications total :sent sent-cnt :failed failed-cnt :pending pending-cnt :delivery-rate del-rate :avg-send-time avg-time :reference-types ref-counts :status-counts status-counts :template-count tmpl-count :preference-count pref-count}))

(defn disable-channel [prefs customer-id channel]
  (set-preference prefs customer-id channel false))

(defn enable-channel [prefs customer-id channel]
  (set-preference prefs customer-id channel true))

(defn customer-enabled-channels [prefs customer-id]
  (let [cust-prefs (customer-preferences prefs customer-id)]
  (mapv preference-channel (filterv (fn [p] (preference-enabled p)) cust-prefs))))

(defn customer-disabled-channels [prefs customer-id]
  (let [cust-prefs (customer-preferences prefs customer-id)]
  (mapv preference-channel (filterv (fn [p] (not (preference-enabled p))) cust-prefs))))

(defn customer-has-preferences? [prefs customer-id]
  (> (count (customer-preferences prefs customer-id)) 0))

(defn notify-all-for-order [next-id template order customers prefs created-at]
  (let [eligible (filterv (fn [c] (should-notify? prefs c template)) customers)]
  (mapv (fn [c] (let [idx (count (filterv (fn [c2] (< (cust/customer-id c2) (cust/customer-id c))) eligible))]
  (order-notification (+ next-id idx) template order c created-at))) eligible)))

(defn notification-with-template-info [n templates]
  (let [tmpl (find-template-by-id templates (notification-template-id n))
   tmpl-name (if (nil? tmpl) "unknown" (template-name tmpl))
   tmpl-channel (if (nil? tmpl) "unknown" (template-channel tmpl))
   tmpl-subject (if (nil? tmpl) "" (template-subject tmpl))]
  {:notification-id (notification-id n) :customer-id (notification-customer-id n) :reference-type (notification-reference-type n) :reference-id (notification-reference-id n) :status (notification-status n) :created-at (notification-created-at n) :sent-at (notification-sent-at n) :template-name tmpl-name :channel tmpl-channel :subject tmpl-subject}))

(defn enriched-notification-list [notifications templates]
  (mapv (fn [n] (notification-with-template-info n templates)) notifications))

(defn has-existing-notification? [notifications customer-id reference-id reference-type]
  (let [matches (filterv (fn [n] (and (= (notification-customer-id n) customer-id) (= (notification-reference-id n) reference-id) (= (notification-reference-type n) reference-type))) notifications)]
  (> (count matches) 0)))

(defn duplicate-notifications [notifications]
  (filterv (fn [n] (let [matches (filterv (fn [other] (and (not= (notification-id other) (notification-id n)) (= (notification-customer-id other) (notification-customer-id n)) (= (notification-reference-id other) (notification-reference-id n)) (= (notification-reference-type other) (notification-reference-type n)))) notifications)]
  (> (count matches) 0))) notifications))

(defn customer-engagement-score [notifications customer-id]
  (let [cust-notifs (notifications-for-customer notifications customer-id)
   sent-cnt (count (filterv (fn [n] (= (notification-status n) "sent")) cust-notifs))
   failed-cnt (count (filterv (fn [n] (= (notification-status n) "failed")) cust-notifs))
   pending-cnt (count (filterv (fn [n] (= (notification-status n) "pending")) cust-notifs))]
  (+ (* sent-cnt 10) (* pending-cnt 2) (- 0 (* failed-cnt 5)))))

(defn top-engaged-customers [notifications customer-ids n]
  (let [scored (reverse (sort-by (fn [cid] (customer-engagement-score notifications cid)) customer-ids))]
  (vec (take n scored))))

(defn avg-daily-notification-volume [notifications start end]
  (let [period-notifs (notifications-in-period notifications start end)
   days (quot (- end start) 86400)]
  (if (<= days 0) (count period-notifs) (quot (count period-notifs) days))))

(defn notifications-created-before [notifications timestamp]
  (filterv (fn [n] (< (notification-created-at n) timestamp)) notifications))

(defn notifications-created-after [notifications timestamp]
  (filterv (fn [n] (> (notification-created-at n) timestamp)) notifications))

(defn template-channels [templates]
  (distinct (mapv template-channel templates)))

(defn templates-by-channel-summary [templates]
  (let [channels (template-channels templates)]
  (reduce (fn [acc ch] (assoc acc ch (count (templates-for-channel templates ch)))) {} channels)))

(defn find-template-by-channel-and-name [templates channel name]
  (first (filterv (fn [t] (and (= (template-channel t) channel) (= (template-name t) name))) templates)))

(defn order-notification-status [notifications order-id]
  (let [order-notifs (order-notifications notifications order-id)]
  (if (empty? order-notifs) "none" (let [sorted (reverse (sort-by notification-created-at order-notifs))]
  (notification-status (first sorted))))))

(defn shipment-notification-status [notifications shipment-id]
  (let [ship-notifs (shipment-notifications notifications shipment-id)]
  (if (empty? ship-notifs) "none" (let [sorted (reverse (sort-by notification-created-at ship-notifs))]
  (notification-status (first sorted))))))

(defn invoice-notification-status [notifications invoice-id]
  (let [inv-notifs (invoice-notifications notifications invoice-id)]
  (if (empty? inv-notifs) "none" (let [sorted (reverse (sort-by notification-created-at inv-notifs))]
  (notification-status (first sorted))))))

(defn retryable-notifications [notifications]
  (failed-notifications notifications))

(defn retry-count-needed [notifications]
  (count (retryable-notifications notifications)))

(defn retry-count-for-customer [notifications customer-id]
  (count (failed-for-customer notifications customer-id)))

(defn within-sla? [n max-seconds]
  (if (= (notification-status n) "sent") (<= (- (notification-sent-at n) (notification-created-at n)) max-seconds) false))

(defn sla-compliance-rate [notifications max-seconds]
  (let [sent (filterv (fn [n] (= (notification-status n) "sent")) notifications)
   cnt (count sent)]
  (if (= cnt 0) 100 (let [compliant (count (filterv (fn [n] (within-sla? n max-seconds)) sent))]
  (quot (* compliant 100) cnt)))))

(defn sla-breaches [notifications max-seconds]
  (filterv (fn [n] (and (= (notification-status n) "sent") (not (within-sla? n max-seconds)))) notifications))

(defn customer-notification-report [notifications templates prefs customer-id]
  (let [cust-notifs (notifications-for-customer notifications customer-id)
   total (count cust-notifs)
   sent-cnt (count (filterv (fn [n] (= (notification-status n) "sent")) cust-notifs))
   failed-cnt (count (filterv (fn [n] (= (notification-status n) "failed")) cust-notifs))
   pending-cnt (count (filterv (fn [n] (= (notification-status n) "pending")) cust-notifs))
   del-rate (delivery-rate-pct cust-notifs)
   avg-time (avg-send-time cust-notifs)
   engagement (customer-engagement-score notifications customer-id)
   cust-prefs (customer-preferences prefs customer-id)
   enabled-channels (customer-enabled-channels prefs customer-id)]
  {:customer-id customer-id :total-notifications total :sent sent-cnt :failed failed-cnt :pending pending-cnt :delivery-rate del-rate :avg-send-time avg-time :engagement-score engagement :preference-count (count cust-prefs) :enabled-channels enabled-channels}))
