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

(ns vmware.provider 
  (:use 
    [trammel.core :only  (defconstrainedrecord)]
    [clojure.core.strint :only (<<)]
    [bouncer [core :as b] [validators :as v]]
    [celestial.core :only (Vm)]
    [celestial.common :only (get* import-logging)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.provider :only (str? vec?)]
    [celestial.model :only (translate vconstruct)])
  (:import 
    java.net.URL
    com.vmware.vim25.mo.InventoryNavigator
    com.vmware.vim25.mo.ServiceInstance))


(defn connect [^String url ^String user ^String pwd]
  (ServiceInstance. (URL. url) user pwd true))

(defconstrainedrecord Template []
  "ct should match proxmox expected input"
  []
  Vm
  (create [this])

  (delete [this])

  (start [this])

  (stop [this])

  (status [this] ))

#_(defn list-vms [service] 
    (.searchManagedEntities (InventoryNavigator. (.getRootFolder service)) "VirtualMachine"))

#_(bean (.getGuest (first (list-vms (connect "https://192.168.5.23/sdk" "Administrator" "Oox1kai7")))))
