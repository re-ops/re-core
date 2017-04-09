(ns re-core.repl.systems
  "Repl systems access"
  (:require
    [progrock.core :as pr]
    [re-core.jobs :as jobs :refer (enqueue)]
    [re-core.common :refer (gen-uuid)]
    [taoensso.timbre :refer  (refer-timbre)]
    [clojure.set :refer (difference)]
    [re-core.persistency.systems :as s]
    [re-core.repl.base :refer [Repl select-keys*]])
  (:import 
    [re_mote.repl.base Hosts]
    [re_core.repl.base Systems]))

(refer-timbre)

(defprotocol Jobs
  "System jobs"
  (stop [this items])
  (create [this items])
  (destroy [this items])
  (clear [this items])
  (reload [this items])
  (status [this jobs])
  (watch [this jobs]))

(defprotocol Host
  "Hosts" 
  (hosts [this items ssh])
  )

(defn grep-system [k v [id system]]
  (let [sub (select-keys* system [:owner] [:machine :hostname] [:machine :os] [:machine :ip])]
    (= v (sub k))))

(defn schedule-job [action [id system]]
  (let [m {:identity id :tid (gen-uuid) :args [(assoc system :system-id (Integer. id))]}]
     {:system id :job (enqueue action m)}))

(extend-type Systems
  Repl
  (ls [this]
    (let [systems (into [] (s/all-systems))]
       [this {:systems (doall (map (juxt identity s/get-system) systems))}]))

  (find [this exp])

  (rm [this systems]
     (doseq [id (map first systems)]
       (s/delete-system! (Integer/valueOf id)))
      [this {:systems []}])

  (grep [this systems k v]
      [this {:systems (filter (partial grep-system k v) (systems :systems))}])

  (add [this specs]
     [this {:systems (map (fn [s] (let [id (s/add-system s)] [id (s/get-system id)])) specs)}]))

(defn filter-done [sts]
  (into #{} (filter (fn [{:keys [status]}] (or (#{:done :recently-done} status) (nil? status))) sts)))

(defn run-job [m id systems]
  (merge m {:jobs (map (partial schedule-job id) systems) :queue id}))

(extend-type Systems
  Jobs
   (stop [this {:keys [systems] :as m}] 
     [this (run-job m "stop" systems)])

   (create [this {:keys [systems] :as m}]
     [this (run-job m "create" systems)])

   (reload [this {:keys [systems] :as m}]
      [this (run-job m "reload" systems)])

   (destroy [this {:keys [systems] :as m}]
      [this (run-job m "destroy" systems)])

   (clear [this {:keys [systems] :as m}]
      [this (run-job m "clear" systems)])

   (status [this {:keys [jobs queue]}]
      (map (fn [{:keys [job] :as m }] (assoc m :status (jobs/status queue job))) jobs))

   (watch [this {:keys [jobs queue] :as js}]
     (loop [done (filter-done (status this js))
            bar (pr/progress-bar (count jobs))]
        (if (>= (:progress bar) (:total bar))
          (pr/print (pr/done bar))
          (do (Thread/sleep 100)
            (pr/print bar)
            (let [done' (filter-done (status this js))]
               (recur done' (pr/tick bar (count (difference done' done))))))))))

(extend-type Systems
  Host 
  (hosts [this {:keys [systems]} ssh]
     [(Hosts. ssh (mapv (fn [[_ system]] (get-in system [:machine :ip])) systems)) {}]    
    )
  )

(defn refer-systems []
  (require '[re-core.repl.systems :as sys :refer [stop watch create status reload destroy clear hosts]]))
