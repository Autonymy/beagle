(ns beagle.dtrace
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Distributed tracing runtime for beagle microservices.
;;
;; Provides span lifecycle, context propagation (HTTP headers + dynamic vars),
;; Ring middleware, and export to file/TCP collectors. Works in both Babashka
;; and JVM Clojure.
;; ---------------------------------------------------------------------------

;; ---- Thread-local trace context (dynamic vars) ----------------------------

(def ^:dynamic *trace-id* nil)
(def ^:dynamic *span-id* nil)
(def ^:dynamic *service* "unknown")
(def ^:dynamic *export-fn* nil)

;; ---- ID generation --------------------------------------------------------

(defn gen-id []
  (str (java.util.UUID/randomUUID)))

(defn gen-short-id []
  (subs (gen-id) 0 8))

;; ---- Span buffer ----------------------------------------------------------

(def span-buffer (atom []))

(defn drain-buffer! []
  (let [spans @span-buffer]
    (reset! span-buffer [])
    spans))

;; ---- Span lifecycle -------------------------------------------------------

(defn start-span
  ([operation] (start-span operation {}))
  ([operation {:keys [tags trace-id parent-span-id service]}]
   (let [tid (or trace-id *trace-id* (gen-id))
         sid (gen-id)
         pid (or parent-span-id *span-id*)]
     {:trace-id tid
      :span-id sid
      :parent-span-id pid
      :service (or service *service*)
      :operation (str operation)
      :start-ms (System/currentTimeMillis)
      :tags (or tags {})
      :events []
      :local-trace []})))

(defn add-event [span name & {:keys [attrs]}]
  (update span :events conj
    {:time-ms (System/currentTimeMillis)
     :name (str name)
     :attrs (or attrs {})}))

(defn add-local-trace [span entries]
  (update span :local-trace into entries))

(defn finish-span
  ([span] (finish-span span {}))
  ([span {:keys [error tags local-trace]}]
   (let [now (System/currentTimeMillis)
         completed (cond-> span
                     true (assoc :end-ms now
                                 :duration-ms (- now (:start-ms span))
                                 :status (if error :error :ok))
                     error (assoc :error-message (str error))
                     tags (update :tags merge tags)
                     local-trace (update :local-trace into local-trace))]
     (if *export-fn*
       (*export-fn* completed)
       (swap! span-buffer conj completed))
     completed)))

;; ---- Span execution wrapper -----------------------------------------------

(defn with-span* [operation opts thunk]
  (let [span (start-span operation opts)]
    (binding [*trace-id* (:trace-id span)
              *span-id* (:span-id span)]
      (try
        (let [result (thunk)]
          (finish-span span)
          result)
        (catch Exception e
          (finish-span span {:error e})
          (throw e))))))

(defn traced-call
  "Wrap a cross-service function call with span tracking.
   Used by auto-instrumentation to mark service boundary crossings."
  [from-service to-service operation target-fn & args]
  (let [span (start-span operation
               {:service from-service
                :tags {"target-service" to-service
                       "kind" "client"
                       "args-count" (count args)}})]
    (binding [*trace-id* (:trace-id span)
              *span-id* (:span-id span)
              *service* to-service]
      (try
        (let [result (apply target-fn args)]
          (finish-span span {:tags {"result-type" (type result)}})
          result)
        (catch Exception e
          (finish-span span {:error e})
          (throw e))))))

;; ---- HTTP context propagation ---------------------------------------------

(def header-trace-id "X-Beagle-Trace-Id")
(def header-span-id "X-Beagle-Span-Id")
(def header-service "X-Beagle-Service")
(def header-sampled "X-Beagle-Sampled")

(defn inject-headers
  "Inject current trace context into outgoing HTTP headers."
  ([] (inject-headers {}))
  ([headers]
   (cond-> headers
     *trace-id* (assoc header-trace-id *trace-id*)
     *span-id* (assoc header-span-id *span-id*)
     *service* (assoc header-service *service*))))

(defn extract-context
  "Extract trace context from incoming HTTP request headers."
  [headers]
  (let [get-h (fn [k] (or (get headers k)
                           (get headers (str/lower-case k))))]
    {:trace-id (get-h header-trace-id)
     :parent-span-id (get-h header-span-id)
     :parent-service (get-h header-service)
     :sampled (not= "0" (get-h header-sampled))}))

;; ---- Ring middleware ------------------------------------------------------

(defn wrap-dtrace
  "Ring middleware: extract trace context from request, create server span,
   propagate context through handler, inject into response headers."
  [handler {:keys [service]}]
  (fn [request]
    (let [ctx (extract-context (:headers request))
          trace-id (or (:trace-id ctx) (gen-id))
          parent-id (:parent-span-id ctx)
          span (start-span (str (:request-method request) " " (:uri request))
                 {:trace-id trace-id
                  :parent-span-id parent-id
                  :service service
                  :tags {"kind" "server"
                         "http.method" (str (:request-method request))
                         "http.url" (str (:uri request))
                         "http.remote-service" (or (:parent-service ctx) "external")}})]
      (binding [*trace-id* trace-id
                *span-id* (:span-id span)
                *service* service]
        (try
          (let [response (handler request)
                status (get response :status 200)]
            (finish-span span {:tags {"http.status" status}})
            (update response :headers merge (inject-headers)))
          (catch Exception e
            (finish-span span {:error e :tags {"http.status" 500}})
            (throw e)))))))

;; ---- HTTP client wrapper --------------------------------------------------

(defn traced-http-call
  "Wrap an outgoing HTTP call with trace context propagation.
   Injects trace headers, creates a client span."
  [method url opts http-fn]
  (let [span (start-span (str method " " url)
               {:tags {"kind" "client"
                       "http.method" (str method)
                       "http.url" url}})]
    (binding [*trace-id* (:trace-id span)
              *span-id* (:span-id span)]
      (try
        (let [traced-opts (update opts :headers inject-headers)
              response (http-fn method url traced-opts)]
          (finish-span span {:tags {"http.status" (:status response)}})
          response)
        (catch Exception e
          (finish-span span {:error e})
          (throw e))))))

;; ---- Export: file (JSONL) -------------------------------------------------

(defn span->json-str [span]
  (let [esc (fn [s] (when s
                       (-> (str s)
                           (str/replace "\\" "\\\\")
                           (str/replace "\"" "\\\"")
                           (str/replace "\n" "\\n"))))
        kv (fn [k v] (str "\"" (name k) "\":" v))
        qv (fn [v] (str "\"" (esc v) "\""))
        tags-str (str "{"
                   (str/join ","
                     (for [[tk tv] (:tags span)]
                       (str "\"" (esc (str tk)) "\":" (qv (str tv)))))
                   "}")
        events-str (str "["
                     (str/join ","
                       (for [ev (:events span)]
                         (str "{\"time_ms\":" (:time-ms ev)
                              ",\"name\":" (qv (:name ev)) "}")))
                     "]")
        local-str (str "["
                    (str/join ","
                      (for [lt (:local-trace span)]
                        (str "{\"op\":" (qv (:op lt))
                             ",\"result\":" (qv (str (:result lt)))
                             ",\"line\":" (or (:line lt) "null")
                             ",\"file\":" (if (:file lt) (qv (:file lt)) "null")
                             "}")))
                    "]")]
    (str "{"
      (str/join ","
        [(kv :trace_id (qv (:trace-id span)))
         (kv :span_id (qv (:span-id span)))
         (kv :parent_span_id (if (:parent-span-id span)
                               (qv (:parent-span-id span)) "null"))
         (kv :service (qv (:service span)))
         (kv :operation (qv (:operation span)))
         (kv :start_ms (:start-ms span))
         (kv :end_ms (or (:end-ms span) "null"))
         (kv :duration_ms (or (:duration-ms span) "null"))
         (kv :status (qv (name (or (:status span) :unknown))))
         (kv :error (if (:error-message span)
                      (qv (:error-message span)) "null"))
         (kv :tags tags-str)
         (kv :events events-str)
         (kv :local_trace local-str)])
      "}")))

(def file-lock (Object.))

(defn export-to-file!
  "Append a span as JSONL to a file. Thread-safe via lock."
  [path span]
  (locking file-lock
    (let [f (java.io.File. ^String path)]
      (.mkdirs (.getParentFile f))
      (spit path (str (span->json-str span) "\n") :append true))))

(defn make-file-exporter
  "Returns an export function that appends spans to <trace-dir>/<service>.jsonl"
  [trace-dir]
  (fn [span]
    (let [service (or (:service span) "unknown")
          path (str trace-dir "/" service ".jsonl")]
      (export-to-file! path span))))

(defn flush-buffer-to-dir!
  "Flush all buffered spans to files in trace-dir, grouped by service."
  [trace-dir]
  (let [spans (drain-buffer!)
        by-service (group-by :service spans)]
    (.mkdirs (java.io.File. ^String trace-dir))
    (doseq [[service svc-spans] by-service]
      (let [path (str trace-dir "/" (or service "unknown") ".jsonl")]
        (doseq [span svc-spans]
          (spit path (str (span->json-str span) "\n") :append true))))
    (count spans)))

;; ---- Export: TCP (to collector daemon) ------------------------------------

(defn export-to-collector!
  "Send a span to a running beagle-dtrace collector via TCP."
  [host port span]
  (try
    (let [sock (java.net.Socket. ^String host ^int port)
          out (java.io.PrintWriter. (.getOutputStream sock) true)]
      (.println out (span->json-str span))
      (.close sock))
    (catch Exception _e nil)))

(defn make-tcp-exporter
  "Returns an export function that sends spans to a TCP collector."
  [host port]
  (fn [span]
    (export-to-collector! host port span)))

;; ---- Initialization -------------------------------------------------------

(defn init!
  "Initialize distributed tracing for a service.
   Options:
     :service    — service name (required)
     :trace-dir  — directory for file-based span export
     :collector  — {:host H :port P} for TCP export
     :export-fn  — custom export function"
  [{:keys [service trace-dir collector export-fn]}]
  (alter-var-root #'*service* (constantly service))
  (let [efn (or export-fn
               (when trace-dir (make-file-exporter trace-dir))
               (when collector (make-tcp-exporter (:host collector) (:port collector))))]
    (when efn (alter-var-root #'*export-fn* (constantly efn))))
  (.addShutdownHook (Runtime/getRuntime)
    (Thread. (fn []
      (when trace-dir (flush-buffer-to-dir! trace-dir))))))

;; ---- Convenience ----------------------------------------------------------

(defn current-context []
  {:trace-id *trace-id*
   :span-id *span-id*
   :service *service*})

(defn child-context
  "Create a child context map for passing to another service/thread."
  []
  {:trace-id (or *trace-id* (gen-id))
   :parent-span-id *span-id*
   :parent-service *service*})
