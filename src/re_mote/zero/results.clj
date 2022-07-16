(ns re-mote.zero.results
  "Remote function result collection and analyais"
  (:require
   [re-share.schedule :refer [watch seconds]]
   [clojure.core.match :refer [match]]
   [com.rpl.specter :as sp :refer (select transform MAP-VALS ALL ATOM)]
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
  (map (fn [[k v]] [k (map :ttl (vals v))]) (select [MAP-VALS ATOM ALL] results)))

(defn expired
  "Get UUID's for results with expired ttl values for all items"
  [curr]
  (map first (filter (fn [[_ ts]] (every? (fn [t] (> curr t)) ts)) (all-ttl))))

(defn prune []
  (trace "running result prunning")
  (doseq [uuid (expired (curr-time))]
    (trace uuid "have expired ttl and will be cleared")
    (clear-results uuid)))

(defn prune-watch
  "A scheduled job that prunes all expired results based on ttl"
  []
  (watch :result-ttl-prunning (seconds 30) prune))

; Result collection
(defn codes [host v]
  "Mapping result to exit code, keeping compatible with ssh pipeline:
    1. if exit status is present we use that (function terminated with exit code)
    2. we return 256 in a case that an exception was thrown from the function (to keep compatible output)
    Else we return 0 (success)"
  (match [v]
    [{:result {:exit e}}] e
    [{:result {:out _ :exception e}}] (do (info "got exception from" host e) 256)
    :else 0))

(defn with-codes
  [m uuid]
  (transform [ALL] (fn [[host v]] [host (merge {:host host :code (codes host v) :uuid uuid} v)]) m))

(defn all-ready?
  "Checks if all the results for an operation came back and are ready"
  [hosts uuid]
  (every? (set (keys (result uuid))) hosts))

(defn add-timeout
  "Set timeout result for a given host"
  [m host]
  (assoc m host {:result {:err "failed to get result within timeout range" :exit -1 :out ""}}))

(defn get-results
  "Grab available results, hosts which are missing are assumed to have timed out (to be used from a polling function with all-ready?)"
  [hosts uuid]
  (let [present (set (keys (result uuid)))
        missing (clojure.set/difference (into #{} hosts) present)
        result (reduce add-timeout (result uuid) missing)]
    (clear-results uuid)
    (with-codes result uuid)))

(defn collect
  "Collect all results by busy waiting until either all results are back or timeout has been reached (in which case missing hosts will be marked to match)."
  [hosts uuid timeout]
  (try
    (wait-for {:timeout timeout :sleep [100 :ms]} (fn [] (all-ready? hosts uuid)) "Failed to collect all hosts")
    (catch Exception e
      (warn "Failed to get results within specified timeout range"
            (merge (ex-data e) {:missing (missing-results hosts uuid) :uuid uuid :timeout timeout}))))
  (get-results hosts uuid))

(defn refer-zero-results []
  (require '[re-mote.zero.results :as zerors :refer (collect pretty-result clear-results add-result get-results missing-results capacity)]))
