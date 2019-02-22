(ns re-core.workers
  "Queue workers"
  (:require
   [re-core.workflows :as wf]
   [mount.core :as mount :refer (defstate)]
   [taoensso.timbre :refer (refer-timbre)]
   [qbits.knit :refer (executor) :as knit]
   [re-core.queue :refer (process)]))

(refer-timbre)

(def e (atom nil))

(def workers-m
  {:reload [wf/reload 4] :destroy [wf/destroy 4] :provision [wf/provision 4]
   :stage [wf/stage 4] :create [wf/create 4] :start [wf/start 4] :stop [wf/stop 4]
   :clear [wf/clear 2] :clone [wf/clone 2]})

(defn- setup-workers []
  (doseq [[topic [f n]] workers-m]
    (dotimes [_ n]
      (knit/future (process f topic) {:executor @e})
      (debug "future for " topic "started"))))

(defn start- []
  (reset! e (executor :fixed {:num-threads 20}))
  (setup-workers))

(defn stop- []
  (when @e
    (.shutdown @e)
    (reset! e nil)))

(defstate workers
  :start (start-)
  :stop (stop-))

