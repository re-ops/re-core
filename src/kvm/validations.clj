(ns kvm.validations
  "KVM validations"
  (:require 
    [re-core.model :refer (check-validity)]
    [clojure.core.strint :refer (<<)]
    [subs.core :as subs :refer (validate! combine every-v every-kv validation when-not-nil)])
 )

(def machine-entity
  {:machine {
     :hostname #{:required :String} :domain #{:required :String} 
     :user #{:required :String} :os #{:required :Keyword} 
     :cpu #{:required :number} :ram #{:required :number}
  }})

(def kvm-entity
  {:kvm {
     :node #{:required :Keyword}
    }
   })

(def domain-provider
   {:name #{:required :String} :user #{:required :String} 
    :image {:template #{:required :String} :flavor #{:required :Keyword}} 
    :cpu #{:required :number} :ram #{:required :number}
   })

(def node-provider
   {:user #{:required :String} :host #{:required :String} 
    :port #{:required :number} 
    })

(defmethod check-validity [:kvm :entity] [domain]
  (validate! domain (combine machine-entity kvm-entity) :error ::invalid-system))

(defn provider-validation [domain node*]
  (validate! domain domain-provider :error ::invalid-domain)
  (validate! node* node-provider :error ::invalid-node))
