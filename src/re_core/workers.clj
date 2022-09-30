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
  {:reload [wf/reload 2] :destroy [wf/destroy 2] :provision [wf/provision 2]
   :stage [wf/stage 2] :create [wf/create 2] :start [wf/start 2] :stop [wf/stop 2]
   :clear [wf/clear 2] :clone [wf/clone 2]})

(defn- setup-workers []
  (doseq [[topic [f n]] workers-m]
    (dotimes [_ n]
      (knit/future (process f topic) {:executor @e})
      (debug "future for " topic "started"))))

(defn start- []
  (reset! e (executor :fixed {:num-threads 20}))
  (setup-workers))

(defn stop- [pool]
  (when pool
    (.shutdownNow pool)
    (try
      (.awaitTermination pool 1000 java.util.concurrent.TimeUnit/NANOSECONDS)
      (info "Workers executor pool has been shutdown")
      (catch java.lang.InterruptedException e
        (error e)))))

(defstate workers
  :start (start-)
  :stop (when @e
          (stop- @e)
          (reset! e nil)))

