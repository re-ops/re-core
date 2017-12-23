(ns re-core.queue
  "Durable worker queues"
  (:require
   [re-core.workflows :as wf]
   [components.core :refer (Lifecyle)]
   [taoensso.timbre :refer (refer-timbre)]
   [durable-queue :refer :all]))

(refer-timbre)

(def q (queues "/tmp" {}))

(defrecord Queue []
  Lifecyle
  (setup [this])
  (start [this]
    (info "Starting work queue"))
  (stop [this]
    (info "Stopping work queue")))

(defn instance
  "Creates a jobs instance"
  []
  (Queue.))

(comment
  (take! q :foo 1000 :timed-out!)
  (put! q :foo {:foo 1})
  (take! q :foo)
  (deref *1)
  (complete *1)
  (stats q))

