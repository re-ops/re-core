(ns lxc.sync
  (:require
   [re-core.model :refer (hypervisor)]
   [re-core.provider :refer (from-description)]
   [re-core.presets.common :as sp :refer (validate)]
   [clojure.data.json :as json]
   [lxc.client :as lxc]))

(defn into-system [node name]
  {:post [#(validate %)]}
  (from-description
   (get-in (lxc/get node {:name name}) [:metadata :description])))

(defn sync-node [node]
  (let [node' (merge node (hypervisor :lxc :auth))]
    (map (partial into-system node') (lxc/into-names (lxc/list node')))))
