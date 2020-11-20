(ns re-flow.pubsub
  "In memory pubsub to rules firing"
  (:require
   [mount.core :as mount :refer (defstate)]
   [taoensso.timbre :refer (refer-timbre)]
   [clojure.core.async :refer (chan pub >!! <!! sub close!)]))

(refer-timbre)

(defn- initialize []
  (let [input (chan)]
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

