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

(ns freenas.validations
  "Freenas based validations"
  (:require 
    [celestial.model :refer (check-validity)]
    [celestial.provider :refer (mappings)]
    [clojure.core.strint :refer (<<)] 
    [subs.core :refer (validate! combine validation when-not-nil)]
    ))

(validation :netmask
  (when-not-nil (partial re-find #"\d+\.\d+\.\d+\.\d+\/\d\d") "must be a legal netmask"))


(def machine-common
  {:hostname #{:required :String} :ip #{:required :String :ip} 
   :netmask #{:required :String} :mac #{:required :mac}
  })


(def freenas
  {:freenas {
     :flags #{:String}
   }})

(defmethod check-validity [:freenas :entity] [spec]
  (validate! spec (combine {:machine machine-common} freenas) :error ::invalid-system))

(def jail-mappings {
  :ip :jail_ipv4 :hostname :jail_host :netmask :jail_ipv4_netmask 
   :mac :jail_mac :flags :jail_flags })

(defn validate-provider [spec]
  (validate! spec (combine (mappings machine-common jail-mappings))  :error ::invalid-jail))

