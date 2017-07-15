(ns digital.validations
  "Digital ocean validations"
  (:require 
    [re-core.model :refer (check-validity)]
    [clojure.core.strint :refer (<<)]
    [subs.core :as subs :refer (validate! combine every-v every-kv validation when-not-nil)]))

(def machine-entity
  {:machine {
     :hostname #{:required :String} :domain #{:required :String} 
     :user #{:required :String} :os #{:required :Keyword} 
  }})

(def digital-entity
  {:digital-ocean
     {:region #{:required :String} :size #{:required :String}
      :backups #{:Boolean} :private-networking #{:Boolean}
      }
    })

(defmethod check-validity [:digital-ocean :entity] [droplet]
  (validate! droplet (combine machine-entity digital-entity) :error ::invalid-system))
 
(validation :ssh-key* (every-v #{:String}))

(def digital-provider
  {:region #{:required :String} :size #{:required :String}
   :backups #{:Boolean} :private-networking #{:Boolean}
   :ssh-keys #{:Vector :ssh-key*} :image #{:required :String}
  })

(defn provider-validation [droplet]
  (validate! droplet digital-provider :error ::invalid-droplet))
