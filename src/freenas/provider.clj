(ns freenas.provider
  "Freenas jails provider"
  (:require 
    [freenas.remote]
    [slingshot.slingshot :refer  [throw+]]
    [celestial.persistency.systems :as s]
    [celestial.common :refer (import-logging)]
    [celestial.provider :refer (wait-for wait-for-ssh)]
    [celestial.core :refer (Vm)] 
    [celestial.model :refer (hypervisor translate vconstruct)])
 )

(import-logging)

(defrecord Instance [spec]
  Vm
  (create [this] 
    )

  (start [this]
     )

  (delete [this]
     )

  (stop [this]
     )

  (status [this] 
     ))
