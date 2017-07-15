(ns es.core
  "Core elasticsearch module"
  (:require 
    [components.core :refer (Lifecyle)] 
    [es.common :refer (initialize index)]
    [es.node :refer (connect stop)]))

(defrecord Elastic 
  [] 
  Lifecyle
  (setup [this]
    (connect)
    (initialize))
  (start [this]
    (connect))
  (stop [this]
    (stop)))

(defn instance 
   "creates a Elastic components" 
   []
  (Elastic.))
