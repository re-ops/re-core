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

(ns proxmox.validations
  "Proxmox based validations"
  (:require 
    [clojure.core.strint :refer (<<)] 
    [subs.core :refer (validate! combine validation when-not-nil)]
    [celestial.validations :as cv]))

(def common-bridging 
  {:bridge #{:required :String} :interface #{:required :String}
   :netmask #{:required :String :ip} :gateway #{:required :String :ip}})

(def common-resources
   {:cpus #{:required :number} :disk #{:required :number}
    :memory #{:required :number} :hostname #{:required :String}})

(def prox-types #{:ct :vm})

(defn greater-then [from i] (> i from))

(validation :prox-type
  (when-not-nil prox-types (<< "Proxmox VM type must be either ~{prox-types}")))

(validation :greater-then-100
  (when-not-nil (partial greater-then 100) "must be greater then 100"))

(validation :ip 
   (when-not-nil (partial re-find #"\d+\.\d+\.\d+\.\d+") "must be a legal ip address"))

(def proxmox-entity
  {:proxmox {
    :type #{:required :prox-type} :vmid #{:greater-then-100}
    :password #{:required :String} :nameserver #{:String}}})

(def machine-entity
  {:machine {
    :domain #{:required :String} :ip #{:String} :os #{:required :Keyword}}})

(defn entity-validation [{:keys [bridge]}]
  (combine (if bridge {:machine common-bridging} {}) 
    {:machine common-resources} machine-entity proxmox-entity))

(defn validate-entity
  "proxmox based system entity validation for persistence layer" 
  [{:keys [machine] :as proxmox}]
  (validate! proxmox (entity-validation machine) :error ::invalid-system))

(def extended-vs
  {:extended {:id #{:number} :features #{:Vector}}})

(validation :fqdn 
   (when-not-nil (partial re-find #".*\.\w*") "must be fully qualified"))

(def ct-provider
  {:ct {
    :vmid #{:required :number} :password #{:required :String}
    :nameserver #{:String} :hostname #{:String :fqdn :required}
    :ostemplate #{:required :String}}})

(def common-network )

(defn provider-validation [{:keys [bridge] :as network}]
  (combine (if bridge {:network common-bridging} {})
     {:ct common-resources} ct-provider extended-vs {:network {:ip_address #{:String :ip}}}))

(defn validate-provider [ct extended network]
  (validate! {:ct ct :extended extended :network network} 
    (provider-validation network) :error ::invalid-container))

