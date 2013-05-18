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
    com.vmware.vim25.mo.InventoryNavigator
    com.vmware.vim25.mo.ServiceInstance
    com.vmware.vim25.mo.Task
    com.vmware.vim25.mo.VirtualMachine )
 )

(def ^:dynamic service)

(defn connect [{:keys [url username password]}]
  (ServiceInstance. (URL. url) username password true))

(defmacro with-service [body]
  `(binding [service (connect (get* :hypervisor :vsphere))]
     ~body 
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

(defn status [hostname]
  (with-service
    (let [vm (find* "VirtualMachine" hostname)]
     (-> (bean vm) :guest bean :guestState )
      )
    )
  )

(comment
  (clone {:datacenter "playground"} 
         {:template "ubuntu-13.04_puppet-3.1" :hostname "foo" :disk-format :sparse :cpus 1 :memory 512})
  (status "foo")
  )

