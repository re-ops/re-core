(comment 
  Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns kvm.validations
  "KVM validations"
  (:require 
    [celestial.model :refer (check-validity)]
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

(defmethod check-validity [:kvm :entity] [domain]
  (validate! domain (combine machine-entity kvm-entity) :error ::invalid-system))

(defn provider-validation [domain]
  (validate! domain domain-provider :error ::invalid-domain))
