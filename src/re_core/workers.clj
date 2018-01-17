(ns re-core.workers
  "Queue workers"
 (:require
   [re-core.workflows :as wf]
   [components.core :refer (Lifecyle)]
   [taoensso.timbre :refer (refer-timbre)]
   [qbits.knit :refer (executor) :as knit]
   [re-core.queue :refer (process)]))

(refer-timbre)

(def e (atom nil))

(def workers
  {:reload [wf/reload 4] :destroy [wf/destroy 4] :provision [wf/provision 4]
   :stage [wf/stage 4] :create [wf/create 4] :start [wf/start 4] :stop [wf/stop 4]
   :clear [wf/clear 2] :clone [wf/clone 2]})

(defn- setup-workers []
  (doseq [[topic [f n]] workers]
    (dotimes [_ n]
      (knit/future @e (process f topic))
      (debug "future for " topic "started"))))

(defn start- []
  (reset! e (executor :fixed  {:num-threads 20}))
  (setup-workers))

(defn stop- [] 
  (when @e
    (.shutdown @e)
    (reset! e nil)))

(defrecord Workers []
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
  (Workers.))
