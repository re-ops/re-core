(ns es.core
  "Core elasticsearch module"
  (:require
   [components.core :refer (Lifecyle)]
   [es.common :refer (initialize)]
   [re-core.common :refer (get!)]
   [re-share.es.node :as node]))

(defrecord Elastic
           []
  Lifecyle
  (setup [this]
    (let [{:keys [index] :as m} (get! :elasticsearch)]
      (node/connect m)
      (initialize index)))
  (start [this]
    (node/connect (get! :elasticsearch)))
  (stop [this]
    (node/stop)))

(defn instance
  "creates a Elastic components"
  []
  (Elastic.))
