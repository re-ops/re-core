(ns aws.model
  (:use [flatland.useful.map :only  (dissoc-in*)]
        [celestial.model :only (clone)]) 
  )

(defmethod clone :aws [spec]
  "Clones the model replace unique identifiers in the process" 
  (-> spec 
      (dissoc-in* [:machine :ssh-host])))
