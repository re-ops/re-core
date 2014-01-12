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

(ns docker.provider
  "Provides Docker support in Celestial "
  (:require 
    [celestial.provider :refer [selections]]
    [slingshot.slingshot :refer  [throw+ try+]]
    [docker.client :as c]
    [trammel.core :refer (defconstrainedrecord)]
    [clojure.core.strint :refer (<<)]
    [celestial.model :refer (translate vconstruct)]
    [celestial.common :refer (import-logging)]
    [celestial.core :refer (Vm)] 
    [docker.validations :refer (validate-provider)]
    [celestial.persistency :as p]
    [celestial.persistency.systems :as s]))

(import-logging)

(defn container-id 
   "grab container id"
   [system-id]
  (get-in (s/get-system system-id) [:docker :container-id]))

(defconstrainedrecord Container [node system-id create-spec start-spec]
  "A docker container instance"
  [(validate-provider create-spec start-spec) (not (nil? node))]
  Vm
  (create [this] 
    (let [{:keys [id]} (c/create node create-spec)]
      (when (s/system-exists? id)
         (s/partial-system id {:docker {:container-id id}})))
       this)

  (start [this]
     (c/start node (container-id system-id) start-spec) 
    )

  (delete [this]
    (c/delete node (container-id system-id)) 
    )

  (stop [this]
    (c/stop node (container-id system-id))
    )

  (status [this] 
    (try+ 
      (let [{:keys [running]} (:state (c/inspect :local (container-id system-id)))]
        (if running "running" "stopped")) 
      (catch [:status 404] e false)))
  ) 

(def starts-ks [:port-bindings :binds])

(def create-ks [:image :exposed-ports :volumes])

(defmethod translate :docker [{:keys [machine docker system-id] :as spec}]
  "Convert the general model into a docker specific one"
   (let [{:keys [node]} docker]
     (into [node system-id] (selections (merge machine docker) [create-ks starts-ks]))))

(defmethod vconstruct :docker [{:keys [docker] :as spec}]
    (apply ->Container (translate spec)))
