(ns re-flow.pubsub
  "In memory pubsub to rules firing"
  (:require
   [taoensso.timbre :refer (info)]
   [mount.core :as mount :refer (defstate)]
   [taoensso.timbre :refer (refer-timbre)]
   [clojure.core.async :refer (chan dropping-buffer pub >!! <!! sub close!)]))

(refer-timbre)

(defn- initialize []
  (let [input (chan (dropping-buffer 100))]
    {:input input
     :pub (pub input :state)}))

(defstate ^{:on-reload :noop} pubsub
  :start (initialize)
  :stop (close! (pubsub :input)))

(defn publish-?e
  "Publish an ?e"
  [?e]
  (>!! (pubsub :input) ?e))

(defn subscribe-?e
  "Subsribe to ?e with state"
  [state c]
  (sub (pubsub :pub) state c) c)

(comment
  (publish-?e {:state :re-flow.certs/copied})
  (future
    (let [output (subscribe-?e :re-flow.certs/copied (chan))]
      (loop []
        (Thread/sleep 1000)
        (info (<!! output))
        (recur)))))
