(ns re-core.queue
  "Durable worker queues"
  (:require
   [re-core.workflows :as wf]
   [es.jobs :as jobs]
   [components.core :refer (Lifecyle)]
   [taoensso.timbre :refer (refer-timbre)]
   [qbits.knit :refer (executor) :as knit]
   [durable-queue :refer (take! put! complete! queues) :as dq]))

(refer-timbre)

(def workers
  {:reload [wf/reload 4] :destroy [wf/destroy 4] :provision [wf/provision 4]
   :stage [wf/stage 4] :create [wf/create 4] :start [wf/start 4] :stop [wf/stop 4]
   :clear [wf/clear 2] :clone [wf/clone 2]})

(def q (atom nil))

(defn stats []
  (dq/stats @q))

(def run (atom true))

(def e (atom nil))

(defn- now [] (System/currentTimeMillis))

(defn- save-status
  "marks as succesful"
  [job topic status start f]
  (let [job' (merge job (meta f) {:start start :end (now) :topic topic :status status})]
    (jobs/put job')
    (trace "saved status" status job')))

(defn- process [f topic]
  (while @run
    (let [task (take! @q topic 1000 :timed-out!)]
      (when-not (= task :timed-out!)
        (let [{:keys [identity tid args] :as job} (deref task) start (now)]
          (try
            (debug "start processing " topic identity)
            (apply f args)
            (save-status job topic :success start f)
            (debug "done processing " topic identity)
            (catch Throwable e
              (error e)
              (save-status (assoc job :message (.getMessage e)) topic :failure start f))
            (finally
              (complete! task)))))))
  (debug "worker for" topic "going down"))

(defn enqueue [topic job]
  (put! @q topic job))

(defn status [{:keys [tid] :as job}]
  {:pre [(not (nil? tid))]}
  (when-let [job (jobs/get tid)]
    (-> job :status keyword)))

(defn- setup-workers []
  (doseq [[topic [f n]] workers]
    (dotimes [_ n]
      (knit/future @e (process f topic))
      (debug "future for " topic "started"))))

(defn- start- []
  (reset! run true)
  (reset! q (queues "/tmp" {:complete? (fn [_] true)}))
  (reset! e (executor :fixed  {:num-threads 20}))
  (setup-workers))

(defn- stop- []
  (reset! run false)
  (when @e
    (.shutdown @e)
    (reset! e nil))
  (reset! q nil))

(defrecord Queue []
  Lifecyle
  (setup [this])
  (start [this]
    (info "Starting work queue")
    (start-))
  (stop [this]
    (info "Stopping work queue")
    (stop-)))

(defn instance
  "Creates a jobs instance"
  []
  (Queue.))

(comment
  (stats @q))

