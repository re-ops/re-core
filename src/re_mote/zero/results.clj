(ns re-mote.zero.results
  "Remote function result collection and analyais"
  (:require
   [re-share.schedule :refer [watch seconds]]
   [clojure.core.match :refer [match]]
   [com.rpl.specter :refer (select transform MAP-VALS MAP-KEYS ALL VAL ATOM multi-path subselect)]
   [re-share.wait :refer (wait-for wait-time curr-time)]
   [taoensso.timbre :refer  (refer-timbre)]
   [puget.printer :as puget]))

(refer-timbre)

(def buckets 32)

(def results
  (into {} (map (fn [i] [i (ref {})]) (range buckets))))

(defn capacity []
  (map (fn [[_ v]] (count @v)) results))

(defn bucket [uuid]
  (results (mod (BigInteger. uuid 16) buckets)))

(defn ttl
  "The minimum time that this result will be kept in the bucket"
  []
  (wait-time (curr-time) [10 :minute]))

(defn add-result
  ([hostname uuid r]
   (let [v {:result r :ttl (ttl)} b (bucket uuid)]
     (dosync
      (alter b assoc-in [uuid hostname] v))))
  ([hostname uuid r t]
   (let [v {:result r :profile {:time t} :ttl (ttl)} b (bucket uuid)]
     (dosync
      (alter b assoc-in [uuid hostname] v)))))

(defn result [uuid]
  (get @(bucket uuid) uuid {}))

(defn clear-results
  ([]
   (doseq [[k _] results]
     (dosync
      (ref-set (results k) {}))))
  ([uuid]
   (dosync
    (alter (bucket uuid) dissoc uuid))))

(defn get-results [hosts uuid]
  (let [ks (set (keys (result uuid)))]
    (when (every? ks hosts)
      (trace "got all results for" uuid)
      (result uuid))))

(defn missing-results [hosts uuid]
  (filter (comp not (result uuid)) hosts))

(defn pretty-result
  "(pretty-result \"reops-0\" :plus-one)"
  [uuid host]
  (puget/cprint
   (let [r (result uuid)] (r host))))

; Prunning

(defn all-ttl
  "Get all ttl values from the results"
  []
  (let [buckets-ttls (select [MAP-VALS ATOM (multi-path MAP-KEYS (subselect [MAP-VALS MAP-VALS :ttl]))] results)
        ; some buckets return only uuid with no subselected ttls
        pairs (filter (fn [[k v]] (and (string? k) (vector? v))) (partition 2 1 buckets-ttls))]
    (apply hash-map (apply concat pairs))))

(defn expired
  "Get UUID's for results with expired ttl values for all items"
  [curr]
  (map first (filter (fn [[_ ts]] (every? (fn [t] (> curr t)) ts)) (all-ttl))))

(defn prune []
  (debug "running result prunning")
  (doseq [uuid (expired (curr-time))]
    (debug uuid "have expired ttl and will be cleared")
    (clear-results uuid)))

(defn prune-watch
  "A scheduled job that prunes all expired results based on ttl"
  []
  (watch :result-ttl-prunning (seconds 30) prune))

; Result collection
(defn codes [v]
  "Mapping result to exit code, keeping compatible with ssh pipeline:
    1. if exit status is present we use that (function terminated with exit code)
    2. we return 256 in a case that an exception was thrown from the function (to keep compatible output)
    Else we return 0 (success)"
  (match [v]
    [{:result {:exit e}}] e
    [{:result {:out _ :exception e}}] (do (info e) 256)
    :else 0))

(defn with-codes
  [m uuid]
  (transform [ALL] (fn [[h v]] [h (merge {:host h :code (codes v) :uuid uuid} v)]) m))

(defn collect
  "Collect returned results if timeout is provided the collection will run until timeout has reached or all results are back"
  ([hosts uuid]
   (when-let [results (get-results hosts uuid)]
     (clear-results uuid)
     (with-codes results uuid)))
  ([hosts uuid timeout]
   (try
     (wait-for {:timeout timeout :sleep [100 :ms]} (fn [] (get-results hosts uuid)) "Failed to collect all hosts")
     (catch Exception e
       (warn "Failed to get results"
             (merge (ex-data e) {:missing (missing-results hosts uuid) :uuid uuid}))))
   (collect hosts uuid)))

(defn refer-zero-results []
  (require '[re-mote.zero.results :as zerors :refer (collect pretty-result clear-results add-result get-results missing-results capacity)]))
