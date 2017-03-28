(ns re-core.repl.systems
  "Repl systems access"
  (:require
    [progrock.core :as pr]
    [re-core.jobs :as jobs :refer (enqueue)]
    [re-core.common :refer (gen-uuid)]
    [taoensso.timbre :refer  (refer-timbre)]
    [clojure.set :refer (difference)]
    [re-core.persistency.systems :as s]
    [re-core.repl.base :refer [Repl select-keys* with-admin]])
  (:import [re-core.repl.base Systems]))

(refer-timbre)

(defprotocol Jobs
  "System jobs"
  (stop [this items])
  (create [this itmes])
  (status [this jobs])
  (watch [this jobs]))


(defn grep-system [k v [id system]]
  (let [sub (select-keys* system [:owner] [:machine :hostname] [:machine :os] [:machine :ip])]
    (= v (sub k))))

(defn schedule-job [action [id system]]
   (let [m {:identity id :tid (gen-uuid) :env (system :env) :user {:username "admin"}}]
    {:system id :job (enqueue action m)}))

(extend-type Systems
  Repl
  (ls [this]
    (with-admin
      (let [systems (into [] (s/systems-for (re-core.api.systems/working-username)))]
        [this {:systems (doall (map (juxt identity s/get-system) systems))}])))

  (find [this exp])

  (rm [this systems]
     (doseq [id (map first systems)]
       (s/delete-system! (Integer/valueOf id)))
      [this {:systems []}])

  (grep [this systems k v]
      [this {:systems (filter (partial grep-system k v) (systems :systems))}])

  (add [this specs]
     [this {:systems (map (fn [s] (with-admin (let [id (s/add-system s)] [id (s/get-system id)]))) specs)}]))

(extend-type Systems
  Jobs
   (stop [this {:keys [systems] :as m}]
       [this (merge m {:jobs (map (partial schedule-job "stop") systems) :queue "stop"})])

   (create [this {:keys [systems] :as m}]
      [this (merge m {:jobs (map (partial schedule-job "create") systems) :queue "create"})])

   (status [this {:keys [jobs queue]}]
      (map (fn [{:keys [job] :as m }] (assoc m :status (jobs/status queue job))) jobs))

   (watch [this {:keys [jobs queue] :as js}]
     (loop [done (into #{} (filter :status (status this js)))
            bar (pr/progress-bar (count jobs))]
        (if (>= (:progress bar) (:total bar))
          (pr/print (pr/done bar))
          (do (Thread/sleep 100)
            (pr/print bar)
            (let [done' (into #{} (filter :status (status this js)))]
               (recur done' (pr/tick bar (count (difference done' done))))))))))


(defn refer-systems []
  (require '[re-core.repl.systems :as sys :refer [stop watch create status]]))

