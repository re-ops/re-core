(ns es.core
  "Core elasticsearch module"
  (:require
   [components.core :refer (Lifecyle)]
   [es.common :refer (initialize index)]
   [es.node :as node]))

(defrecord Elastic
           []
  Lifecyle
  (setup [this]
    (node/connect)
    (initialize))
  (start [this]
    (node/connect))
  (stop [this]
    (node/stop)))

(defn instance
  "creates a Elastic components"
  []
  (Elastic.))
