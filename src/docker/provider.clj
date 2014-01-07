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
    [docker.remote :as r]
    [trammel.core :refer (defconstrainedrecord)]
    [clojure.core.strint :refer (<<)]
    [celestial.model :refer (translate vconstruct)]
    [celestial.common :refer (import-logging)]
    [celestial.core :refer (Vm)] 
    [celestial.persistency :as p]
    [celestial.persistency.systems :as s]))


(import-logging)

(defconstrainedrecord Instance [docker node]
  "A docker container instance"
  []
  Vm
  (create [this] 
    )

  (start [this]
    )

  (delete [this]
    )

  (stop [this]
    )

  (status [this] 
    ))

(defmethod translate :proxmox [{:keys [machine docker system-id] :as spec}]
  "Convert the general model into a docker specific one"
  )

(defmethod vconstruct :docker [{:keys [docker] :as spec}]
  (let [{:keys [type node]} docker]
    ))
