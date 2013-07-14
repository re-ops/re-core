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
    [trammel.core :only  (defconstrainedrecord)]
    [clojure.core.strint :only (<<)]
    [vsphere.vijava :only (clone power-on power-off status destroy guest-status)]
    [vsphere.validations :only (provider-validation)]
    [celestial.core :only (Vm)]
    [celestial.common :only (import-logging)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.provider :only (str? vec? mappings transform os->template wait-for)]
    [celestial.model :only (translate vconstruct)])
  )

(import-logging)

(defn wait-for-guest
  "waiting for guest to boot up"
  [hostname timeout]
  (wait-for {:timeout timeout} #(= :running (guest-status hostname))
    {:type ::vsphere:guest-failed :message "Timed out on waiting for guest to start" :hostname hostname}))


(defconstrainedrecord VirtualMachine [allocation machine guest]
  "A vCenter Virtual machine instance"
  [(not (nil? allocation)) (provider-validation allocation machine guest)]
  Vm
  (create [this] 
    (clone allocation machine))

  (delete [this] (destroy (machine :hostname)))

  (start [this] 
    (let [{:keys [hostname]} machine]
      (power-on hostname) 
      (wait-for-guest hostname [10 :minute])))

  (stop [this] (power-off (machine :hostname)))

  (status [this] (status (machine :hostname)) ))

(def machine-ks [:template :cpus :memory :hostname])

(def allocation-ks [:pool :datacenter :disk-format])

(defn select-from [ks] (fn[m] (select-keys m ks)))

(def selections (juxt (select-from allocation-ks) (select-from machine-ks) :guest))

(defmethod translate :vsphere [{:keys [machine vsphere system-id]}]
  "Convert the general model into a vsphere specific one"
  (-> (merge machine vsphere {:system-id system-id})
      (mappings {:os :template})
      (transform {:template (os->template :vsphere)})
      selections
      ))

(defmethod vconstruct :vsphere [spec]
  (let [[allocation machine guest] (translate spec)]
    (->VirtualMachine allocation machine guest)))

