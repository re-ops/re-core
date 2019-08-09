(ns lxc.sync
  (:require
   [re-core.model :refer (hypervisor)]
   [re-core.provider :refer (from-description)]
   [re-core.presets.systems :refer (validate)]
   [lxc.client :as lxc]))

(defn into-system [node name]
  {:post [#(validate %)]}
  (let [{:keys [properties]} (lxc/get-metadata node {:name name})
        system (from-description (properties :description))]
    (if-let [ip (lxc/ip node {:name name})]
      (assoc-in system [:machine :ip] ip)
      system)))

(defn sync-node [node]
  (let [node' (merge node (hypervisor :lxc :auth))]
    (map (partial into-system node') (lxc/into-names (lxc/list node')))))
