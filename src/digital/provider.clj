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
     [slingshot.slingshot :refer (throw+)]
     [clojure.tools.trace :as tr]
     [digital.validations :refer (provider-validation)]
     [clojure.core.strint :refer (<<)]
     [celestial.model :refer (translate vconstruct hypervisor*)]
     [celestial.provider :refer (mappings transform selections os->template wait-for wait-for-ssh)]
     [celestial.persistency.systems :as s]
     [celestial.common :refer (import-logging get*)]
     [celestial.core :refer (Vm)]
     [digitalocean.v2.core :as do]))

(import-logging)


(defn run-action [type* id]
  (let [post-action (do/generic :post (<< "droplets/~{id}/actions"))]
    (post-action (hypervisor* :digital-ocean :token) nil  {:type (name type*)})))

(defn ip [droplet]
  (get-in droplet [:networks :v4 :ip_address]) 
  )

(defrecord Droplet [token spec ext]
  Vm
  (create [this]
     (let [{:keys [droplet message] :as result} (do/create-droplet token nil spec) {:keys [id]} droplet]
       (when-not droplet 
         (throw+ {:type ::droplet-fail} message))
       (s/partial-system (ext :system-id) {:digital-ocean {:id id}})
       this 
       ))

  (delete [this])

  (start [this]
    (run-action "power_on" (get-in spec [:digital-ocean :id]))
    #_(wait-for-ssh (ip-address this) (:user extended) [5 :minute]))

  (stop [this]
     (run-action "power_off" (get-in spec [:digital-ocean :id]))
    )

  (status [this]
     (let [status-map {"active" "running" "off" "stop"}
           result (:status (do/get-droplet (get-in spec [:digital-ocean :id])))]
       (or (status-map result) result))
    ))

(defn machine-ts 
  "Construcuting machine transformations"
  [{:keys [domain]}]
   {:name (fn [host] (<< "~{host}.~{domain}")) :image (fn [os] (:image ((os->template :digital-ocean) os)))})

(def drop-ks [:name :region :size :image :ssh_keys])

(defmethod translate :digital-ocean [{:keys [machine digital-ocean system-id] :as spec}] 
   (-> (merge machine digital-ocean {:system-id system-id})
     (mappings {:os :image :hostname :name})
     (transform (machine-ts machine))
     (assoc :ssh_keys [(hypervisor* :digital-ocean :ssh-key)])
     (selections [drop-ks [:system-id]] )
     )
  )

(defmethod vconstruct :digital-ocean [{:keys [digital-ocean machine] :as spec}]
  (let [[translated ext] (translate spec)]
     (provider-validation translated)
     (->Droplet (hypervisor* :digital-ocean :token) translated ext)
   )
  )


#_(clojure.pprint/pprint (map (juxt :id :slug) (:images (do/images (hypervisor* :digital-ocean :token)))))



; (clojure.pprint/pprint (do/regions (get* :hypervisor :dev :digital-ocean :token)))
