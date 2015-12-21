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

(ns digital.provider
   "Digital ocean provider"
   (:require  
     [clojure.core.strint :refer (<<)]
     [celestial.model :refer (translate vconstruct hypervisor*)]
     [celestial.provider :refer (selections mappings transform os->template wait-for wait-for-ssh)]
     [celestial.persistency.systems :as s]
     [celestial.common :refer (import-logging get*)]
     [celestial.core :refer (Vm)]
     [digitalocean.v2.core :as do]))

(import-logging)


(defn run-action [type* id]
  (let [post-action (do/generic :post (<< "droplets/~{id}/actions"))]
    (post-action (hypervisor* :digital-ocean :token) nil  {:type (name type*)}))
  )

(defrecord Droplet [token spec]
  Vm
  (create [this]
     (let [{:keys [droplet]} (do/create-droplet token nil props)
           {:keys [id]} droplet ])
          (s/partial-system (spec :system-id) {:digital-ocean {:id id}})    
       )

  (delete [this])

  (start [this]
    (run-action "power_on" (get-in spec [:digital-ocean :id]))
    #_(wait-for-ssh (ip-address this) (:user extended) [5 :minute]))

  (stop [this]
     (run-action "power_off" (get-in spec [:digital-ocean :id]))
    )

  (status [this]))

(defmethod vconstruct :digital-ocean [{:keys [digital-ocean] :as spec}]
  (let [{:keys [token]} digital-ocean]
    ))


#_(clojure.pprint/pprint (map (juxt :id :slug) (:images (do/images (hypervisor* :digital-ocean :token)))))



