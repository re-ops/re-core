(ns re-mote.log
  "log collection"
  (:require
   [re-share.time :refer (local-now to-long minus)]
   [clojure.string :refer (join upper-case)]
   [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender)]
   [taoensso.timbre.appenders.core :refer (println-appender)]
   [clansi.core :refer (style)]
   [taoensso.timbre :refer (refer-timbre set-level! merge-config!)]
   [clojure.core.strint :refer (<<)]
   [clojure.java.io :refer (reader)]
   [re-share.log :as log]
   [re-share.schedule :refer (watch seconds)])
  (:import
   [java.time.temporal ChronoUnit]))

(refer-timbre)

(def logs (atom {}))

(def hosts (atom #{}))

(defn log-output
  "Output log stream"
  [out host]
  (with-open [r (reader out)]
    (doseq [line (line-seq r)]
      (debug  (<< "[~{host}]:") line))))

(defn process-line
  "process a single log line"
  [host line]
  (when (or (@hosts host) (@hosts "*")) (info (<< "[~{host}]:") line)) line)

(defn collect-log
  "Collect log output into logs atom"
  [uuid]
  (fn [out host]
    (with-open [r (reader out)]
      (let [lines (doall (map (partial process-line host) (line-seq r)))]
        (swap! logs (fn [m] (assoc m uuid  {:ts (local-now) :lines lines})))))))

(defn get-log
  "Getting log entry and clearing it"
  [uuid & clear]
  (when-let [{:keys [lines]} (get @logs uuid)]
    (when clear (swap! logs (fn [m] (dissoc m uuid))))
    lines))

(defn get-logs
  "Getting logs for all hosts"
  [hosts]
  (doall
   (map
    (fn [{:keys [uuid] :as m}]
      (if-not uuid
        m
        (dissoc (assoc m :out (join "\n" (get-log uuid))) :uuid))) hosts)))

(defn purge
  "Clearing dead non collected logs"
  []
  (let [minute-ago (to-long (minus (local-now) 1 ChronoUnit/MINUTES))
        old (filter (fn [[uuid {:keys [ts]}]] (<= (to-long ts) minute-ago)) @logs)]
    (doseq [[uuid _] old]
      (trace "purged log" uuid)
      (swap! logs (fn [m] (dissoc m uuid))))
    :ok))

(defn run-purge
  "Collected in memory log purge"
  [s]
  (watch :collected-logs-purge (seconds s) (fn [] (trace "purging logs at" (local-now)) (purge))))

(defn setup-logging
  "Sets up logging configuration:
    - stale logs removale interval
    - steam collect logs
    - log level
  "
  [& {:keys [interval level] :or {interval 10 level :info}}]
  (log/setup "re-mote" ["net.schmizz.*" "org.elasticsearch.*" "org.apache.http.*"])
  (set-level! level)
  (run-purge interval))

(defn log-hosts
  "Log a specific host by passing him as an argument
   Log all hosts by passing '*'
   Clearing all with an empty call"
  ([] (reset! hosts #{}))
  ([hs] (swap! hosts conj hs)))

(defn refer-logging []
  (require '[re-mote.log :as log :refer (log-hosts setup-logging)]))
