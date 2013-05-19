(ns vsphere.vijava
  (:use 
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.core.strint :only  (<<)]
    [celestial.common :only (get* import-logging)])
  (:import 
    java.net.URL
    com.vmware.vim25.mo.InventoryNavigator
    com.vmware.vim25.VirtualMachineRelocateTransformation
    com.vmware.vim25.mo.ServiceInstance
    com.vmware.vim25.VirtualMachineCloneSpec
    com.vmware.vim25.VirtualMachineConfigSpec
    com.vmware.vim25.VirtualMachineRelocateSpec
    com.vmware.vim25.mo.Folder
    com.vmware.vim25.VirtualMachinePowerState
    com.vmware.vim25.mo.InventoryNavigator
    com.vmware.vim25.mo.ServiceInstance
    com.vmware.vim25.mo.Task
    com.vmware.vim25.mo.VirtualMachine 
    java.lang.Throwable 
    )
  )

(import-logging)

(def ^:dynamic service)

(defn connect [{:keys [url username password]}]
  (ServiceInstance. (URL. url) username password true))

(def services 
  (memoize (fn []  (into [] (repeatedly 2 #(agent (connect (get* :hypervisor :vsphere))))))))

(defmacro with-service 
  "Uses recycled service instances see http://bit.ly/YRsiNo, 
  We try to keep all agents busy still rand isn't fair (cycle would work better)."
  [body]
  `(let [a# ((services) (rand-int (count (services)))) p# (promise)]
     (send a# (fn [s#]  
                (try 
                  (binding [service s#] (debug (.hashCode service)) (deliver p# ~body) s#) 
                  (catch Throwable e# (deliver p# e#) s#)))) 
     (let [res# @p#]
       (when (instance? Throwable res#)
         (throw res#))
       res# )
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
  ([type name] (find* type name (navigator)))
  ([type name within] (.searchManagedEntity within type name)))

(defn resource-pools [dcname]
  (find-all "ResourcePool" (navigator (find* "Datacenter" dcname))))

(def disk-format-types
  {:sparse VirtualMachineRelocateTransformation/sparse 
   :flat VirtualMachineRelocateTransformation/flat})

(defn relocation-spec [{:keys [datacenter]} {:keys [disk-format]}]
  (doto (VirtualMachineRelocateSpec.) 
    (.setTransform (disk-format-types disk-format))
    (.setPool (.getMOR (first (resource-pools datacenter))))))

(defn config-spec [{:keys [cpus memory]}]
  {:pre [(pos? cpus) (pos? memory)]}
  (doto (VirtualMachineConfigSpec.)
    (.setNumCPUs (int cpus)) 
    (.setMemoryMB memory)))

(defn clone-spec [allocation machine]
  (doto (VirtualMachineCloneSpec.)
    (.setLocation (relocation-spec allocation machine))
    (.setConfig (config-spec machine))
    (.setPowerOn false)
    (.setTemplate false)))

(defmacro wait-for [task]
  `(let [status# (.waitForMe ~task)]
     (when-not (= status# "success")
       (throw+ {:type ::task-fail :message (str "Vmware task failed with status:" status#)}))))

(defn clone [{:keys [datacenter] :as allocation} {:keys [template hostname] :as machine}]
  (with-service
    (let [vm (find* "VirtualMachine" template)]
      (wait-for (.cloneVM_Task vm (.getParent vm) hostname (clone-spec allocation machine))))))

(def power-to-s
  {VirtualMachinePowerState/poweredOn :running 
  VirtualMachinePowerState/poweredOff :stopped 
  VirtualMachinePowerState/suspended :suspended})

(defn status [hostname]
  (with-service
    (let [vm (find* "VirtualMachine" hostname)]
      (-> (bean vm) :summary bean :runtime bean :powerState power-to-s ))))

(defn power-on [hostname]
  (with-service
    (let [vm (find* "VirtualMachine" hostname)]
      (wait-for (.powerOnVM_Task vm nil)))))

(defn power-off [hostname]
  (with-service
    (let [vm (find* "VirtualMachine" hostname)]
      (wait-for (.powerOffVM_Task vm)))))

(defn destroy [hostname]
  (with-service
    (let [vm (find* "VirtualMachine" hostname)]
      (wait-for (.destroy_Task vm)))))

(comment
  (clone {:datacenter "playground"} 
         {:template "ubuntu-13.04_puppet-3.1" :hostname "foo" :disk-format :sparse :cpus 1 :memory 512})
  (map deref (repeatedly 40 (fn []  (future (status "foo") ))))
  (status "foo")
  (power-on "foo")
  (power-off "foo")
  (destroy "foo") 
  )






