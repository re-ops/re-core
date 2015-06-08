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
    [celestial.security :refer (set-user)]
    [clojure.java.data :refer [from-java]]
    [openstack.common :refer (servers)]
    [celestial.model :refer (set-env)]
    [celestial.common :refer (import-logging)]
    [cheshire.core :refer (generate-string)]
    [clojure.core.logic :refer (!= == run* membero fresh featurec run nafc)]
    [celestial.persistency.systems :as s]))

(import-logging)

(defn list-servers 
   "list all current vms" 
   [tenant]
   (-> (servers tenant) (.listAll true)))

(defn ids [tenant]
  (map #(.getId %) (list-servers tenant)))

(defn data [env]
  (filter :openstack
    (map #(assoc :system-id (s/get-system %) %) s/get-system (s/get-system-index :env env))))

(defn managedo [ms]
   (run* [q]
     (fresh [?openstack ?instance-id ?m]
       (membero ?m ms)
       (featurec ?m  {:openstack ?openstack})
       (featurec ?openstack {:instance-id ?instance-id})
       (== q ?instance-id))))

(defn find-candidates 
  "Searching for candidates VMs" 
  ([ms ids] (find-candidates ms ids [])) 
  ([ms ids excludes]
   (run* [q]
     (fresh [?m ?openstack ?instance-id ?system-id]
       (membero ?instance-id ids)
       (nafc membero ?instance-id excludes)
       (nafc membero ?instance-id (managedo ms))
       (== q ?instance-id)))))

(defn cleanup
  "Clears up instances not managed in Celestial" 
  [{:keys [tenant env es user]}]
  (set-user {:username user}
    (set-env env
     (let [ds (ids tenant) ms (data env) cs (find-candidates ms ds es)]
       (debug "following" cs "ids will be cleared")
       (doseq [c cs]
         #_(.delete servers c)
         (info "cleared" c  "on gc task"))))))
