(ns vmware.vijava
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
  `(binding [service (connect (get* :hypervisor :vmware))]
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

(defn relocation-spec [dcname]
  (doto (VirtualMachineRelocateSpec.) 
    (.setTransform (VirtualMachineRelocateTransformation/sparse))
    (.setPool (.getMOR (first (resource-pools dcname))))))

(defn clone-spec [dcname]
  (doto (VirtualMachineCloneSpec.)
    (.setLocation (relocation-spec dcname))
    (.setPowerOn false)
    (.setTemplate false)))

(defmacro wait-for [task]
  `(let [status# (.waitForMe ~task)]
     (when-not (= status# "success")
       (throw+ {:type ::task-fail :message (str "Vmware task failed with status:" status#)}))))

(defn clone [template vmname dcname]
  (with-service
    (let [vm (find* "VirtualMachine" template)]
      (wait-for (.cloneVM_Task vm (.getParent vm) vmname (clone-spec dcname))))))

(comment
  (clone "ubuntu-13.04_puppet-3.1" "foo" "playground")
  )


