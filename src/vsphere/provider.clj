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

(ns vsphere.provider 
  (:use 
    [celestial.provider :only (str? vec? mappings)]
    [celestial.common :only (get* import-logging)]
    [trammel.core :only  (defconstrainedrecord)]
    [clojure.core.strint :only (<<)]
    [vsphere.vijava :only (clone power-on power-off status destroy)]
    [bouncer [core :as b] [validators :as v]]
    [celestial.core :only (Vm)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.provider :only (str? vec? mappings transform os->template)]
    [celestial.model :only (translate vconstruct)])
  )


(defconstrainedrecord VirtualMachine [allocation machine]
  ""
  []
  Vm
  (create [this] (clone allocation machine))

  (delete [this] (destroy (machine :hostname)))

  (start [this] (power-on (machine :hostname)))

  (stop [this] (power-off (machine :hostname)))

  (status [this] (status (machine :hostname)) ))

(def machine-ks [:template :cpus :disk :memory :hostname])

(def allocation-ks [:pool :datacenter])

(def selections (juxt (fn [m] (select-keys m allocation-ks)) (fn [m] (select-keys m machine-ks))))

(defmethod translate :vsphere [{:keys [machine vsphere system-id]}]
  "Convert the general model into a vsphere specific one"
  (-> (merge machine vsphere {:system-id system-id})
      (mappings {:os :template})
      (transform {:template (os->template :vsphere)})
        selections
      ))

(defmethod vconstruct :vsphere [spec]
   (let [[allocation machine] (translate spec)] (->VirtualMachine allocation machine)))

