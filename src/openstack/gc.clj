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

(ns openstack.gc
  "GCing Vm's in openstack"
  (:refer-clojure :exclude [==])
  (:require 
    [clojure.java.data :refer [from-java]]
    [openstack.common :refer (servers)]
    [cheshire.core :refer (generate-string)]
    [clojure.core.logic :refer (!= == run* membero fresh featurec run nafc)]
    [celestial.persistency.systems :as s]))

(defn list-servers 
   "list all current vms" 
   [tenant]
   (-> (servers tenant) (.listAll true)))

(defn ids [tenant]
  (map #(.getId %) (list-servers tenant)))

(defn data [env hypervisor]
  (filter hypervisor 
    (map #(assoc :system-id (s/get-system %) %) s/get-system (s/get-system-index :env env))))

(defn find-candidates 
  "Searching for candidates VMs" 
  ([ms ids] (find-candidates ms ids [])) 
  ([ms ids excludes]
   (run* [q]
     (fresh [?m ?openstack ?instance-id ?system-id]
       (nafc membero ?instance-id ids)
       (nafc membero ?instance-id excludes)
       (featurec ?m  {:openstack ?openstack :system-id ?system-id})
       (featurec ?openstack  {:instance-id ?instance-id} )
       (membero ?m ms)
       (== q [?system-id ?instance-id])))))

