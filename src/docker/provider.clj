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
    [clojure.string :refer [split]]
    [celestial.provider :refer [selections mappings transform wait-for]]
    [slingshot.slingshot :refer  [throw+ try+]]
    [docker.client :as c]
    [clojure.core.strint :refer (<<)]
    [celestial.model :refer (translate vconstruct)]
    [celestial.common :refer (import-logging)]
    [celestial.core :refer (Vm)] 
    [docker.validations :refer (validate-provider)]
    [celestial.persistency.systems :as s]))

(import-logging)

(defn container-id 
   "grab container id"
   [system-id]
  (get-in (s/get-system system-id) [:docker :container-id]))

(defrecord Container [node system-id create-spec start-spec]
  Vm
  (create [this] 
    (let [{:keys [id]} (c/create node create-spec)]
      (when (s/system-exists? system-id)
         (s/partial-system system-id {:docker {:container-id id}})
        ))
       this)

  (start [this]
    (c/start node (container-id system-id) start-spec))

  (delete [this]
    (c/delete node (container-id system-id)))

  (stop [this]
    (c/stop node (container-id system-id)))

  (status [this] 
    (try+ 
      (if-let [id (container-id system-id)]
        (if (get-in (c/inspect :local id) [:state :running])
          "running" "stopped")
        false) 
      (catch [:status 404] e false)))
  ) 

(def starts-ks [:port-bindings :binds])

(def create-ks [:image :cpu-shares :memory :exposed-ports :volumes])

(defn to-binds
  "Trasnforms 22/tcp:2222 to {22/tcp [{:host-ip 0.0.0.0 :host-port 2222}]}" 
  {:test 
    #(assert (= (to-binds ["22/tcp:2222/0.0.0.0"])
         {"22/tcp" [{:host-ip "0.0.0.0" :host-port "2222"}]}))}
  [bs]
  (apply merge 
    (map 
      #(let [[c h] (split % #":") [hpo hip] (split h #"\/")]
        {c [{:host-ip hip :host-port hpo}]}) bs)))

(test #'to-binds)

(defn empty-hashes-map
   "convert a vector [:a] to empty hash map {:a {}}" 
   [v]
  (zipmap v (repeat {})))

(defmethod translate :docker [{:keys [machine docker system-id] :as spec}]
  "Convert the general model into a docker specific one"
  (let [{:keys [node]} docker]
    (into [(keyword node) system-id]  
          (-> (merge machine docker)
              (mappings {:mount-bindings :binds :cpus :cpu-shares})
              (transform {:port-bindings to-binds :exposed-ports empty-hashes-map 
                          :volumes empty-hashes-map :memory #(* % 1024 1024)})
              (selections [create-ks starts-ks])
              ))))

(defn validate [[node system-id create-spec start-spec :as args]]
  (validate-provider create-spec start-spec) 
  (assert (not (nil? node)))
  args)

(defmethod vconstruct :docker [{:keys [docker] :as spec}]
  (apply ->Container (validate (translate spec))))
