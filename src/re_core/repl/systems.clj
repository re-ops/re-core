(ns re-core.repl.systems
  "Repl systems access"
  (:require
   [clojure.core.strint :refer  (<<)]
   [clansi.core :refer  (style)]
   [re-core.jobs :as jobs :refer (enqueue)]
   [re-core.common :refer (gen-uuid)]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.set :refer (difference)]
   [es.systems :as s]
   [es.jobs :as es]
   [re-core.repl.base :refer [Repl Report select-keys*]])
  (:import
   [re_mote.repl.base Hosts]
   [re_core.repl.base Systems]))

(refer-timbre)

(defprotocol Jobs
  "System jobs"
  (stop [this items])
  (start [this items])
  (create [this items])
  (destroy [this items])
  (clear [this items])
  (reload [this items])
  (status [this jobs])
  (block-wait [this jobs])
  (async-wait [this jobs f args])
  (pretty-print [this m message])
  (watch [this jobs]))

(defprotocol Host
  "Hosts"
  (into-hosts
    [this items]
    [this items k]))

(defn grep-system [k v [id system]]
  (let [sub (select-keys* system [:owner] [:machine :hostname] [:machine :os] [:machine :ip])]
    (= v (sub k))))

(defn schedule-job [action [id system]]
  (let [tid (gen-uuid) m {:identity id :tid tid :args [(assoc system :system-id (Integer. id))]}]
    {:system id :job (enqueue action m) :tid tid}))

(defn run-ack [this {:keys [systems] :as m}]
  (println "The following systems will be effected Y/n (n*):")
  (doseq [[id s] systems]
    (println "      " id (get-in s [:machine :hostname])))
  (if-not (= (read-line) "Y")
    [this {}]
    [this m]))

(extend-type Systems
  Repl
  (ls [this]
    ; fix this using a scroll query in ES!
    #_(let [systems (into [] (s/all-systems))]
        [this {:systems (doall (map (juxt identity s/get) systems))}]))

  (filter-by [this {:keys [systems] :as m} f]
    [this {:systems (filter f systems)}])

  (ack [this {:keys [systems] :as m} opts]
    (if-not (contains? opts :force)
      (run-ack this m)
      [this m]))

  (rm [this systems]
    (doseq [id (map first systems)]
      (s/delete (Integer/valueOf id)))
    [this {:systems []}])

  (grep [this systems k v]
    [this {:systems (filter (partial grep-system k v) (systems :systems))}])

  (add [this specs]
    (let [f (fn [s] (let [id (s/put s)] [id (assoc (s/get id) :system-id id)]))]
      [this {:systems (map f specs)}])))

(defn filter-done [sts]
  (into #{} (filter (fn [{:keys [status]}] (or (#{:done :recently-done} status) (nil? status))) sts)))

(defn run-job [m id systems]
  (merge m {:jobs (map (partial schedule-job id) systems) :queue id}))

(defn result [{:keys [tid] :as job}]
  (merge ((es/get tid) :_source) job))

(defn add-results [this jobs]
  (let [{:keys [success] :as results} (group-by (comp keyword :status) (map result jobs))
        systems (doall (map (juxt identity s/get) (map :identity success)))]
    {:systems systems :results results}))

(extend-type Systems
  Jobs
  (stop [this {:keys [systems] :as m}]
    [this (run-job m "stop" systems)])

  (start [this {:keys [systems] :as m}]
    [this (run-job m "start" systems)])

  (create [this {:keys [systems] :as m}]
    [this (run-job m "create" systems)])

  (reload [this {:keys [systems] :as m}]
    [this (run-job m "reload" systems)])

  (destroy [this {:keys [systems] :as m}]
    [this (run-job m "destroy" systems)])

  (clear [this {:keys [systems] :as m}]
    [this (run-job m "clear" systems)])

  (status [this {:keys [jobs queue]}]
    (map (fn [{:keys [job] :as m}] (assoc m :status (jobs/status queue job))) jobs))

  (block-wait [this {:keys [jobs queue systems] :as js}]
    (loop [done (filter-done (status this js))]
      (when (< (count done) (count jobs))
        (Thread/sleep 100)
        (recur (filter-done (status this js)))))
    [this (add-results this jobs)])

  (async-wait [this {:keys [jobs queue systems] :as js} f & args]
    (let [out *out*]
      (future
        (binding [*out* out]
          (loop [done (filter-done (status this js))]
            (when (< (count done) (count jobs))
              (Thread/sleep 100)
              (recur (filter-done (status this js)))))
          (apply f (into [this (add-results this jobs)] args))))))

  (pretty-print [this {:keys [results chain] :as m} message]
    (let [{:keys [success failure]} results]
      (println "\n")
      (println (style (<< "Running ~{message} summary:") :blue) "\n")
      (doseq [{:keys [hostname]} success]
        (println " " (style "✔" :green) hostname))
      (doseq [{:keys [hostname message]} failure]
        (println " " (style "x" :red) hostname "-" message))
      (println "")
      [this m])))

(extend-type Systems
  Host
  (into-hosts [this m]
    (into-hosts this m :ip))
  (into-hosts [this {:keys [systems]} k]
    (let [{:keys [user]} (:machine (second (first systems)))]
      (Hosts. {:user user} (mapv (fn [[_ system]] (get-in system [:machine k])) systems)))))

(extend-type Systems
  Report
  (summary [this {:keys [success failure] :as m}]
    (println "")
    (println (style "Run summary:" :blue) "\n")
    (doseq [{:keys [identity queue]} success]
      (println " " (style "✔" :green) queue identity))
    (doseq [{:keys [identity queue message]} failure]
      (println " " (style "x" :red) queue identity "-" message))
    (println "")
    [this m]))

(defn refer-systems []
  (require '[re-core.repl.systems :as sys :refer [watch status into-hosts block-wait async-wait pretty-print]]))
