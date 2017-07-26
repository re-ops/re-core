(ns re-core.jobs
  (:refer-clojure :exclude [identity])
  (:require
   [gelfino.timbre :refer (set-tid)]
   [re-core.common :refer (get* minute)]
   [taoensso.carmine.locks :refer (with-lock release-lock)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.persistency.systems :as s]
   [es.jobs :as es]
   [flatland.useful.map :refer (map-vals filter-vals)]
   [minderbinder.time :refer (parse-time-unit)]
   [puny.core :refer (entity)]
   [re-core.workflows :as wf]
   [re-core.model :refer (operations)]
   [taoensso.carmine :as car]
   [taoensso.carmine.message-queue :as mq]
   [components.core :refer (Lifecyle)]
   [re-core.redis :refer (create-worker wcar server-conn clear-locks)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(def workers (atom {}))

(def defaults {:wait-time 5 :expiry 30})

(defn job*
  "Get job conf value"
  [& ks]
  (get-in (get* :re-core :job) ks))

(defn save-status
  "marks jobs as succesful"
  [spec status]
  (let [status-exp (* 1000 60 (or (job* :status-expiry) (* 24 60)))]
    (es/put (merge spec {:status status :end (System/currentTimeMillis)}) status-exp)
    (trace "saved status" (merge spec {:status status :end (System/currentTimeMillis)}))
    {:status status}))

(defn job-exec
  "Executes a job function tries to lock identity first (if used)"
  [f {:keys [message attempt]}]
  (let [{:keys [identity args tid env] :as spec} message]
    (set-tid tid
       (let [{:keys [wait-time expiry]} (map-vals (or (job* :lock) defaults) #(* minute %))
             hostname (when identity (get-in (s/get-system identity) [:machine :hostname]))
             spec' (merge spec (meta f) {:start (System/currentTimeMillis) :hostname hostname})]
          (try
            (apply f args)
            (save-status spec' :success)
          (catch Throwable e
             (error e)
             (save-status (assoc spec' :message (.getMessage e)) :failure)))))))

(defn jobs []
  {:reload [wf/reload 4] :destroy [wf/destroy 4] :provision [wf/provision 4]
   :stage [wf/stage 4] :create [wf/create 4] :start [wf/start 4] :stop [wf/stop 4]
   :clear [wf/clear 2] :clone [wf/clone 2]})

(defn apply-config [js]
  {:post [(= (into #{} (keys %)) operations)]}
  (reduce (fn [m [k [f c]]] (assoc m k [f (or (job* :workers k) c)])) {} js))

(defn create-wks
  "create a count of workers for queue"
  [queue f total]
  (mapv (fn [v] (create-worker (name queue) (partial job-exec (with-meta f {:queue queue})))) (range total)))

(defn initialize-workers []
  (doseq [[q [f c]] (apply-config (jobs))]
    (swap! workers assoc q (create-wks q f c))))

(defn clear-queues []
  (info "Clearing job queues")
  (apply mq/clear-queues (server-conn) (mapv name (keys (jobs)))))

(defn enqueue
  "Placing job in redis queue"
  [queue payload]
  {:pre [(contains? (jobs) (keyword queue))]}
  (when (empty? @workers)
    (throw (ex-info "no workers running!" {:queue queue :payload payload})))
  (trace "submitting" payload "to" queue)
  (wcar (mq/enqueue queue payload)))

(defn status [queue uuid]
  (wcar (mq/message-status queue uuid)))

(def readable-status
  {:queued :queued :locked :processing :recently-done :done :backoff :backing-off nil :unkown})

(defn- message-desc [type js]
  (mapv
   (fn [[jid {:keys [identity args tid] :as message}]]
     {:type type :status (readable-status (status type jid))
      :id identity :jid jid :tid tid}) (apply hash-map js)))

(defn queue-status
  "The entire queue message statuses"
  [job]
  (let [ks [:messages :locks :backoffs]]
    (reduce
     (fn [r message] (into r (message-desc job message))) []
     (apply merge (vals (select-keys (mq/queue-status (server-conn) job) ks))))))

(defn running-jobs-status
  "Get all jobs status"
  []
  (reduce (fn [r t] (into r (queue-status (name t)))) [] (keys (jobs))))

(defn shutdown-workers []
  (doseq [[k ws] @workers]
    (doseq [w ws]
      (trace "shutting down" k w)
      (mq/stop w)))
  (reset! workers {}))

(defrecord Jobs
           []
  Lifecyle
  (setup [this])
  (start [this]
    (info "Starting job workers")
    (when (= (job* :reset-on) :start)
      (clear-queues)
      (clear-locks))
    (initialize-workers))
  (stop [this]
    (info "Stopping job workers")
    (shutdown-workers)
    (when (= (job* :reset-on) :stop)
      (clear-queues)
      (clear-locks))))

(defn instance
  "Creates a jobs instance"
  []
  (Jobs.))
