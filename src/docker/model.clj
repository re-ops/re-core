(ns docker.model
  (:use [flatland.useful.map :only  (dissoc-in*)]
        [celestial.model :only (clone)]))

(defmethod clone :docker [spec]
  "Clones the model replace unique identifiers in the process" 
  (-> spec 
      (dissoc-in* [:docker :container-id])))
