(ns proxmox.model
  (:require 
    [flatland.useful.map :refer  (dissoc-in*)]
    [celestial.model :refer (clone hypervisor)] 
    [celestial.common :refer (get!)]))

(defmethod clone :proxmox [spec]
  "Clones the model replace unique identifiers in the process" 
  (-> spec 
      (dissoc-in* [:proxmox :vmid])
      (dissoc-in* [:machine :ip])
      ))

(defn get-node [node] 
  (hypervisor :proxmox :nodes (keyword node)))

(defn proxmox-master [] 
  (get-node (hypervisor :proxmox :master)))
 
