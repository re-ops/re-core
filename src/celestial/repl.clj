(ns celestial.repl
  "Repl Driven Celestial "
  (:require
    [clojure.set :refer (intersection difference)]
    [celestial.persistency.systems :as s]
    [celestial.persistency.types :as t]
    [es.systems :as es :refer (set-flush)]
    [celestial.security :refer (set-user current-user)]
    [taoensso.timbre  :as timbre :refer (set-level!)]
    [io.aviso.columns :refer (format-columns write-rows)]
    [io.aviso.ansi :refer :all]
    [gelfino.timbre :refer (get-tid)]
    [celestial.jobs :as jobs :refer (enqueue)]
    [progrock.core :as pr]
    ))

(set-level! :debug)

(def admin {
  :username "admin" :password "foo"
  :envs [:dev :qa :prod]
  :roles #{:celestial.roles/admin}
  :operations [] })

(defmacro with-admin [& body]
  `(with-redefs [celestial.security/current-user (fn [] {:username "admin"})
                celestial.persistency.users/get-user! (fn [a#] admin)]
       ~@body
       ))

(defprotocol Repl
  "Basic shell like functions on Celestial model types"
  (ls [this] [this & opts])
  (find [this exp])
  (rm [this items])
  (grep [this items k v]))


(defprotocol Jobs
  "System jobs"
  (stop [this items])
  (status [this jobs])
  (watch [this jobs]))

(defn select-keys* [m & paths]
  (into {} (map (fn [p] [(last p) (get-in m p)])) paths))

(defn grep-system [k v [id system]]
  (let [sub (select-keys* system [:owner] [:machine :hostname] [:machine :os] [:machine :ip])]
    (= v (sub k))))


(defn schedule-job [action [id system]]
  (with-admin
    {:system id :job (enqueue action {:identity id :tid (get-tid)})}))

(defrecord Systems []
  Repl
  (ls [this]
    (with-admin
      (let [systems (into [] (s/systems-for (celestial.api.systems/working-username)))]
        [this {:systems (doall (map (juxt identity s/get-system) systems))}])))

  (ls [this & opts]
    (let [m (apply hash-map opts)]))

  (find [this exp])

  (rm [this systems]
     (doseq [id (map first systems)]
       (s/delete-system! (Integer/valueOf id)))
      [this {:systems []}])

   (grep [this systems k v]
      [this {:systems (filter (partial grep-system k v) (systems :systems))}])

  Jobs
   (stop [this {:keys [systems]}]
       [this {:jobs (map (partial schedule-job "stop") systems) :queue "stop"}])

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

(defrecord Types [] Repl
  (ls [this]
    [this {:types (map t/get-type (t/all-types))}])
  (ls [this & opts])
  (find [this exp])
  (rm [this types ])
  (grep [this types k v])
  )

(defmulti pretty
  (fn [[_ m]] (intersection (into #{} (keys m)) #{:systems :types :jobs})))

(defn render [[id m]]
 (-> m
   (select-keys* [:owner] [:machine :hostname] [:machine :os] [:machine :ip])
   (assoc :id id)))

(defmethod pretty #{:systems} [[_ {:keys [systems]}]]
  (let [formatter (format-columns bold-white-font [:right 10] "  " reset-font [:right 2] "  "
                     [:right 5] "  " [:right 12] "  " :none)]
    (write-rows *out* formatter [:hostname :id :owner :os :ip] (map render systems))))

(defmethod pretty #{:types} [[_ {:keys [types]}]]
  (let [formatter (format-columns bold-white-font [:right 10] "  " reset-font [:right 10] [:right 20] :none)]
    (write-rows *out* formatter [:type (comp first keys) :description] types)))

(defmethod pretty #{:jobs} [[_ {:keys [jobs]}]]
  (let [formatter (format-columns bold-white-font [:right 3] "  " reset-font [:right 10] :none)]
    (write-rows *out* formatter [:system  :job] jobs)))

(defmacro | [source fun & funs]
  (let [f (first fun) args (rest fun)]
     `(let [[this# res#] ~source]
        (~f this# res# ~@args))
   ))

(defmacro run [f p s & fns]
  (if-not (empty? fns)
    `(run (~p ~f ~s) ~(first fns) ~(second fns) ~@(rest (rest fns)))
    `(~p ~f ~s)))

(def systems (Systems.))
(def types (Types.))

(def stops (run (ls systems) | (grep :os :ubuntu-14.04) | (stop)))
;; (| stops (watch) )
