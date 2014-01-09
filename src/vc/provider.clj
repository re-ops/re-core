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

(ns vc.provider 
  (:use 
    [celestial.persistency.systems :as s]
    [celestial.provider :only (mappings)]
    [trammel.core :only  (defconstrainedrecord)]
    [clojure.core.strint :only (<<)]
    [vc.vijava :only (clone power-on power-off status destroy guest-status)]
    [vc.guest :only (set-ip)]
    [vc.validations :only (provider-validation)]
    [celestial.core :only (Vm)]
    [celestial.common :only (import-logging)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (translate vconstruct)])
  (:require 
    [celestial.persistency :as p] 
    [celestial.provider :refer (mappings transform os->template wait-for selections)]
    [hypervisors.networking :refer (gen-ip release-ip mark)]
    )
  )

(import-logging)

(defn wait-for-guest
  "waiting for guest to boot up"
  [hostname timeout]
  (wait-for {:timeout timeout} #(= :running (guest-status hostname))
    {:type ::vc:guest-failed :hostname hostname} "Timed out on waiting for guest to start"))

(defn assign-networking
  "Generate ip only if missing"
  [{:keys [ip] :as machine}]
   (if ip 
     (do (mark ip :vcenter) machine)
     (gen-ip machine :vcenter :ip)))

(defconstrainedrecord VirtualMachine [hostname allocation machine id]
  "A vCenter Virtual machine instance"
  [(not (nil? hostname)) (provider-validation allocation machine)]
  Vm
  (create [this] 
    (let [machine* (assign-networking machine)]
      (try+ 
        (clone hostname allocation machine) 
        (.start this)  
        (set-ip hostname (select-keys machine [:user :password :sudo]) machine*) 
        (when (s/system-exists? id)
           (s/partial-system id {:machine {:ip (machine* :ip)}}))
        (.stop this)
        (->VirtualMachine hostname allocation machine* id)
      (catch Throwable e 
        (release-ip (machine* :ip) :vcenter)
        (throw e)
        ))))

  (delete [this] 
    (try 
      (destroy hostname)
     (finally (release-ip (machine :ip) :vcenter))))

  (start [this] 
      (when-not (= (.status this) "running") 
        (power-on hostname) 
        (wait-for-guest hostname [10 :minute])))

  (stop [this] 
        (when-not (= (.status this) "stopped") 
          (power-off hostname)))

  (status [this] 
     (try+ (status hostname) 
       (catch [:type :vc.vijava/missing-entity] e
         (warn "No VM found, most chances it hasn't been created yet") false))))

(def machine-ks [:template :cpus :memory :ip :netmask :gateway :search :names :user :password :sudo :domain])

(def allocation-ks [:pool :datacenter :disk-format :hostsystem])

(defmethod translate :vcenter [{:keys [machine vcenter system-id]}]
  "Convert the general model into a vc specific one"
  (-> (merge machine vcenter {:system-id system-id})
      (mappings {:os :template})
      (transform {:template (os->template :vcenter) :disk-format keyword})
      (selections :hostname allocation-ks machine-ks :system-id)
      ))

(defmethod vconstruct :vcenter [spec]
  (let [[hostname allocation machine system-id] (translate spec)]
    (->VirtualMachine hostname allocation machine system-id)))

