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
    [celestial.provider :refer (mappings)]
    [clojure.core.strint :refer (<<)] 
    [subs.core :refer (validate! combine validation when-not-nil)]
    ))

(validation :netmask
  (when-not-nil (partial re-find #"\d+\.\d+\.\d+\.\d+\/\d\d") "must be a legal netmask"))


(def machine-entity
  {:hostname #{:required :String} :ip #{:required :String :ip} 
   :netmask #{:required :String :netmask} :mac #{:required :mac}
  })


(def freenas
  {:freenas {
    :type #{:required :jail-type} :flags #{:String}
   }})


(defn validate-entity
  "freenas based system entity validation for persistence layer" 
  [{:keys [machine freenas] :as spec}]
  (validate! spec (combine machine-entity freenas) :error ::invalid-system))

(def jail-mappings {
  :ip :jail_ipv4 :hostname :jail_host :netmask :jail_ipv4_netmask 
   :mac :jail_mac :type :jail_type :flags :jail_flags })

(defn validate-provider [spec]
  (validate! spec (combine (mappings machine-entity ))  :error ::invalid-jail))

