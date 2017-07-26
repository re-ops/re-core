(ns gce.validations
  "Google compute validations"
  (:require
   [re-core.model :refer (check-validity)]
   [re-core.provider :refer (mappings)]
   [clojure.core.strint :refer (<<)]
   [subs.core :refer (validate! combine validation when-not-nil every-v every-kv)]))

(def machine-entity
  {:machine {:domain #{:required :String} :ip #{:String}
             :os #{:required :Keyword} :user #{:required :String}}})

(validation :tag* (every-v #{:String}))

(def gce-entity
  {:gce {:machine-type #{:required :String} :zone #{:required :String}
         :tags #{:Vector :tag*} :project-id #{:required :String}}})

(defmethod check-validity [:gce :entity] [spec]
  (validate! spec (combine machine-entity gce-entity) :error ::invalid-system))

(validation :params {:sourceImage #{:required :String}})

(validation :disk {:initializeParams #{:required :Map}})

(validation :disk* (every-v #{:disk}))

(def gce-provider {:name #{:required :String}
                   :machineType #{:required :String}
                   :disks #{:required :disk*}})

(defn validate-provider [gce spec]
  (validate! gce gce-provider :error ::invalid-gce-instance))

