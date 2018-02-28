(ns es.core
  "Core elasticsearch module"
  (:require
   [components.core :refer (Lifecyle)]
   [es.common :refer (initialize index)]
   [re-core.common :refer (get!)]
   [re-share.es.node :as node]))

(defrecord Elastic
           []
  Lifecyle
  (setup [this]
    (node/connect (get! :elasticsearch))
    (initialize))
  (start [this]
    (node/connect (get! :elasticsearch)))
  (stop [this]
    (node/stop)))

(defn instance
  "creates a Elastic components"
  []
  (Elastic.))
