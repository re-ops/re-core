(ns es.core
  "Core elasticsearch module"
  (:require
   [components.core :refer (Lifecyle)]
   [es.common :refer (initialize)]
   [re-share.es.node :as node]
   [re-share.es.common :refer (get-es!)]))

(defrecord Elastic []
  Lifecyle
  (setup [this]
    (let [{:keys [index] :as m} (get-es!)]
      (node/connect m)
      (initialize index)))
  (start [this]
    (node/connect (get-es!)))
  (stop [this]
    (node/stop)))

(defn instance
  "creates a Elastic components"
  []
  (Elastic.))

