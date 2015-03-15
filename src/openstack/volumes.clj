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

(ns openstack.volumes
  "Volumes creation and manipulation"
  (:require 
    [clojure.java.data :refer [from-java]]
    [celestial.provider :refer (wait-for)]
    [celestial.persistency.systems :as s]
    [openstack.common :refer (openstack servers)])
  (:import org.openstack4j.api.Builders))

(defn model 
   "build a volume" 
   [{:keys [size desc name]}]
   (-> (Builders/volume)
     (.name name)
     (.description desc )
     (.size size)
     (.build)))

(defn create-volume [tenant model]
  (-> (openstack tenant) (.blockStorage) (.volumes) (.create model)))


(defn update-vol
   [id {:keys [device]} {:keys [system-id openstack]}]
    (s/partial-system system-id {
        :openstack {
          :volumes (mapv #(if (= (% :device) device) (assoc % :id id) %) (openstack :volumes)) 
        }
      }))

(defn create 
  "create volume returning its id"
  [spec vol tenant]
  (let [{:keys [id]} (from-java (create-volume tenant (model vol)))]
    (update-vol id vol spec) id))

(defn wait-for-active [blocks id timeout]
  "Wait for an ip to be avilable"
  (wait-for {:timeout timeout} #(= "AVAILABLE" (-> blocks (.get id) (.getStatus) str))
    {:type ::openstack:start-failed :timeout timeout} 
      "Timed out on waiting for ip to be available"))

(defn attach 
   "attach volume to instance" 
   [instance-id id device tenant] 
   (-> (servers tenant) (.attachVolume instance-id id device)))
