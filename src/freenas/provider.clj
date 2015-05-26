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

(ns freenas.provider
  "Freenas jails provider"
  (:require 
    [celestial.provider :refer (wait-for-ssh mappings wait-for)]
    [freenas.remote :refer [call]]
    [freenas.validations :refer [validate-provider jail-mappings]]
    [slingshot.slingshot :refer  [throw+]]
    [celestial.persistency.systems :as s]
    [celestial.common :refer (import-logging)]
    [celestial.provider :refer (wait-for wait-for-ssh)]
    [org.httpkit.client :as client]
    [celestial.core :refer (Vm)] 
    [celestial.model :refer (hypervisor translate vconstruct)])
 )

(import-logging)

(defrecord Jail [spec]
  Vm
  (create [this] 
    (call client/post "jails/jails" spec)
    )

  (start [this]
     )

  (delete [this]
     )

  (stop [this]
     )

  (status [this] 
     ))


(defmethod translate :freenas [{:keys [machine freenas system-id] :as spec}]
    "Convert the general model into a freenas jail"
    (mappings (merge machine freenas {:system-id system-id}) jail-mappings))

(defn validate [spec] 
  (validate-provider spec) spec)

(defmethod vconstruct :freenas [spec]
  (Jail. (validate (translate spec))))
