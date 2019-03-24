(ns lxc.provider
  "LXC provider for re-core"
  (:require
   [re-core.model :refer (translate vconstruct hypervisor* hypervisor)]
   [re-core.core :refer (Sync Vm)]
   [es.systems :as s]))

(defrecord Container []
  Vm
  (create [this])
  (delete [this])
  (start [this])
  (stop [this])
  (status [this])
  (ip [this]))

(defmethod vconstruct :lxc [{:keys [lxc machine system-id type] :as spec}])

(comment)
