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

(ns vc.vijava
  (:use 
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.core.strint :only  (<<)]
    [celestial.common :only (get! import-logging)])
  (:import 
    java.net.URL
    com.vmware.vim25.mo.InventoryNavigator
    com.vmware.vim25.VirtualMachineRelocateTransformation
    com.vmware.vim25.mo.ServiceInstance
    com.vmware.vim25.VirtualMachineCloneSpec
    com.vmware.vim25.VirtualMachineConfigSpec
    com.vmware.vim25.VirtualMachineRelocateSpec
    com.vmware.vim25.mo.Folder
    com.vmware.vim25.mo.GuestOperationsManager
    com.vmware.vim25.VirtualMachinePowerState
    com.vmware.vim25.mo.InventoryNavigator
    com.vmware.vim25.mo.Task
    com.vmware.vim25.mo.VirtualMachine 
    java.lang.Throwable 
    ))

(import-logging)

(def ^:dynamic service)

(defn connect [{:keys [url username password]}]
  (ServiceInstance. (URL. url) username password true))

(def services 
  (memoize 
    (fn []  
      (into [] 
            (repeatedly (get! :hypervisor :vcenter :session-count) #(agent (connect (get! :hypervisor :vcenter))))))))

(defn session-expired? [instance]
  (nil? (:currentSession (bean (.getSessionManager (deref (first (services))))))))

(defn renew-session [s]
  (if (session-expired? s)
    (do 
      (debug "Session expired renewing")
      (connect (get! :hypervisor :vcenter)))
    s))

(defmacro execute [body p]
  `(fn [s#]
     (binding [service (renew-session s#)]
       (try 
         (deliver ~p ~body) service) 
       (catch Throwable e# (deliver ~p e#) service))))

(defmacro with-service 
  "Uses recycled service instances see http://bit.ly/YRsiNo, 
  We try to keep all agents busy still rand isn't fair (cycle would work better)."
  [body]
  `(let [a# ((services) (rand-int (count (services)))) p# (promise)]
     (send a# (execute ~body p#)) 
     (let [res# @p#]
       (when (instance? Throwable res#)
         (throw res#))
       res#)
     ))

(defn navigator 
  ([] (navigator (.getRootFolder service)))
  ([root] (InventoryNavigator. root)))

(defn find-all
  "Find all entities of type"
  ([type] (find-all type (navigator)))
  ([type within] (.searchManagedEntities within type)))

(defn find*  
  "Find entity by type and name"
  ([type name*] (find* type name* (navigator)))
  ([type name* within] 
   (if-let [entity (.searchManagedEntity within type name*)]
     entity 
     (throw+ {:type ::missing-entity :message (<< "No matching entity named '~{name*}' of type ~{type} found")}))))

(defn find-vm 
  "locates a vm or a template" 
  [name*]
  (find* "VirtualMachine" name*))

(defn resource-pools [dcname]
  (find-all "ResourcePool" (navigator (find* "Datacenter" dcname))))

(def disk-format-types
  {:sparse VirtualMachineRelocateTransformation/sparse 
   :flat VirtualMachineRelocateTransformation/flat})

(defn relocation-spec [{:keys [datacenter disk-format hostsystem pool]}]
  (doto (VirtualMachineRelocateSpec.) 
    (.setTransform (disk-format-types disk-format))
    (.setPool (.getMOR (if pool (find* "ResourcePool" pool) (first (resource-pools datacenter)))))
    (.setHost (.getMOR (find* "HostSystem" hostsystem))) 
    ))

(defn config-spec [{:keys [cpus memory]}]
  {:pre [(pos? cpus) (pos? memory)]}
  (doto (VirtualMachineConfigSpec.)
    (.setNumCPUs ^:Integer (int cpus)) 
    (.setMemoryMB (long memory))))

(defn clone-spec [allocation machine]
  (doto (VirtualMachineCloneSpec.)
    (.setLocation (relocation-spec allocation))
    (.setConfig (config-spec machine))
    (.setPowerOn false)
    (.setTemplate false)))

(defmacro wait-for [task]
  `(let [status# (.waitForTask ~task 1000 1000)]
     (when-not (= status# "success")
       (throw+ {:type ::task-fail :message (str "Vmware task failed with status:" status#)}))))

(defn clone [hostname {:keys [datacenter] :as allocation} {:keys [template] :as machine}]
  (with-service
    (let [vm (find-vm template)]
      (wait-for (.cloneVM_Task vm (.getParent vm) hostname (clone-spec allocation machine))))))

(def power-to-s
  {VirtualMachinePowerState/poweredOn "running"
   VirtualMachinePowerState/poweredOff "stopped" 
   VirtualMachinePowerState/suspended "suspended"})

(defn status 
  "Get VM status"
  [hostname]
  (with-service
    (-> hostname find-vm bean :summary bean :runtime bean :powerState power-to-s )))

(defn guest-info 
   "Guest info map" 
   [hostname]
    (with-service 
      (-> (find-vm hostname) (.getGuest) bean)))

(defn tools-installed?
   "checks if vmware tools are installed on the host" 
   [hostname]
   (not (nil? (:toolsVersion (guest-info hostname)))))

(defn guest-status 
   "Get guest os status (requires vmware tools to be installed)" 
   [hostname]
  {:pre [(tools-installed? hostname)]}
    (some-> (guest-info hostname) :guestState keyword))

(defn power-on 
  "Power on VM"
  [hostname]
  (with-service
    (wait-for (.powerOnVM_Task (find-vm hostname) nil))))

(defn power-off 
  "Power off VM"
  [hostname]
  (with-service
    (wait-for (.powerOffVM_Task (find-vm hostname)))))

(defn destroy 
  "Destroy a vm, requires vm to be stopped"
  [hostname]
  {:pre [(= (status hostname) "stopped")]}
  (with-service (wait-for (.destroy_Task (find-vm hostname)))))
