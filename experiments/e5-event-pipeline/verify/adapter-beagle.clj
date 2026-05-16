(ns verify.adapter-beagle
  "Adapter for beagle track: maps canonical behavioral API names
   to beagle's emitted function names. Loaded after modules, before tests.")

;; e/make-order-state → e/empty-order-state + assoc status
;; Beagle's empty-order-state hardcodes status="placed". The canonical API
;; accepts status as a parameter, so we assoc it after construction.
(when-not (resolve 'pipeline.events/make-order-state)
  (intern 'pipeline.events 'make-order-state
    (fn [order-id customer-id status items total placed-at]
      (assoc (pipeline.events/empty-order-state order-id customer-id items total placed-at)
             :status status))))

;; hdl/handle-event → hdl/dispatch-event
(when (and (resolve 'pipeline.handlers/dispatch-event)
           (not (resolve 'pipeline.handlers/handle-event)))
  (intern 'pipeline.handlers 'handle-event
    @(resolve 'pipeline.handlers/dispatch-event)))

;; qry/shipment-by-carrier: beagle has (shipments-by-carrier carrier shipments)
;; which filters; the canonical API groups by carrier and returns carrier→count.
(when-not (resolve 'pipeline.queries/shipment-by-carrier)
  (intern 'pipeline.queries 'shipment-by-carrier
    (fn [shipments]
      (reduce
        (fn [counts s]
          (if-let [carrier (:carrier s)]
            (update counts carrier (fnil inc 0))
            counts))
        {}
        shipments))))

;; Stub functions that may not exist in trial code (added to golden after trials ran).
;; These throw so the relevant bug-test fails cleanly rather than crashing load.
(when-not (resolve 'pipeline.analytics/repeat-customer-rate)
  (intern 'pipeline.analytics 'repeat-customer-rate
    (fn [& _] (throw (Exception. "repeat-customer-rate not implemented")))))

(when-not (resolve 'pipeline.analytics/average-time-to-payment)
  (intern 'pipeline.analytics 'average-time-to-payment
    (fn [& _] (throw (Exception. "average-time-to-payment not implemented")))))
