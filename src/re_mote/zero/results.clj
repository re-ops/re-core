(ns re-mote.zero.results
  "Remote function result collection and analyais"
  (:require
   [re-share.schedule :refer [watch seconds]]
   [clojure.core.match :refer [match]]
   [com.rpl.specter :refer (select transform MAP-VALS MAP-KEYS ALL VAL ATOM multi-path subselect)]
   [re-share.wait :refer (wait-for wait-time curr-time)]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.core.incubator :refer (dissoc-in)]
   [puget.printer :as puget]))

(refer-timbre)

(def buckets 32)

(def results
  (into {} (map (fn [i] [i (ref {})]) (range buckets))))

(defn capacity []
  (map (fn [[k v]] (count @v)) results))

(defn bucket [uuid]
  (results (mod (BigInteger. uuid 16) buckets)))

(defn ttl []
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
      (debug "got all results for" uuid)
      (result uuid))))

(defn missing-results [hosts uuid]
  (filter (comp not (result uuid)) hosts))

(defn pretty-result
  "(pretty-result \"reops-0\" :plus-one)"
  [uuid host]
  (puget/cprint
   (let [r (result uuid)] (r host))))

; Prunning
;; (filter (comp not empty?) (select [MAP-VALS ATOM (multi-path MAP-KEYS (subselect [MAP-VALS MAP-VALS :ttl]))] results))

;; (defn expired? [[uuid rs]]
;;   (< (curr-time) (max (map :ttl rs))))
;;
;; (defn prune []
;;   (let [t (curr-time)]
;;     (doseq [[_ b] results]
;;       (filter expired? b))))
;;
;; (defn prune-watch
;;   "Prunning expired ttl results"
;;   []
;;   (watch :callback-processing (seconds 30) prune-ttl))
;;
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
