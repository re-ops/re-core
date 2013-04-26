(ns proxmox.model
  (:use 
    [flatland.useful.map :only  (dissoc-in*)]
    [celestial.model :only (translate vconstruct clone)] 
    )
 )

(defmethod clone :proxmox [spec]
  "Clones the model replace unique identifiers in the process" 
  (-> spec 
      (dissoc-in* [:proxmox :vmid])
      (dissoc-in* [:machine :ip])))
