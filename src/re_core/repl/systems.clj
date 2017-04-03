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
  (:import [re_core.repl.base Systems]))

(refer-timbre)

(defprotocol Jobs
  "System jobs"
  (stop [this items])
  (create [this itmes])
  (destroy [this itmes])
  (reload [this itmes])
  (status [this jobs])
  (watch [this jobs]))


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

(extend-type Systems
  Jobs
   (stop [this {:keys [systems] :as m}]
       [this (merge m {:jobs (map (partial schedule-job "stop") systems) :queue "stop"})])

   (create [this {:keys [systems] :as m}]
      [this (merge m {:jobs (map (partial schedule-job "create") systems) :queue "create"})])

   (reload [this {:keys [systems] :as m}]
      [this (merge m {:jobs (map (partial schedule-job "reload") systems) :queue "reload"})])

   (destroy [this {:keys [systems] :as m}]
      [this (merge m {:jobs (map (partial schedule-job "destroy") systems) :queue "destroy"})])

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

(defn refer-systems []
  (require '[re-core.repl.systems :as sys :refer [stop watch create status reload destroy]]))
