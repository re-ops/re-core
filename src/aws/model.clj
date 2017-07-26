(ns aws.model
  (:use [flatland.useful.map :only  (dissoc-in*)]
        [re-core.model :only (clone)]))

(defmethod clone :aws [spec clone-spec]
  "Clones the model replace unique identifiers in the process"
  (-> spec
      (dissoc-in* [:machine :ip])
      (dissoc-in* [:aws :instance-id])))

(def identifiers
  {:machine #{:ip :hostname :domain} :aws #{:instance-id}})

