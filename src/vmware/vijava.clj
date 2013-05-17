(ns vmware.vijava
  (:use 
    [celestial.common :only (get* import-logging)])
  (:import 
    java.net.URL
    com.vmware.vim25.mo.InventoryNavigator
    com.vmware.vim25.mo.ServiceInstance
    com.vmware.vim25.VirtualMachineCloneSpec
    com.vmware.vim25.VirtualMachineRelocateSpec
    com.vmware.vim25.mo.Folder
    com.vmware.vim25.mo.InventoryNavigator
    com.vmware.vim25.mo.ServiceInstance
    com.vmware.vim25.mo.Task
    com.vmware.vim25.mo.VirtualMachine )
 )

(defn connect [{:keys [url username password]}]
  (ServiceInstance. (URL. url) username password true))

(defn find-all
  "Find all entities of type"
  ([service type nav-root] 
   (.searchManagedEntities (InventoryNavigator. (.getRootFolder service)) type)) )

(defn find-by  
  [service type name]
  (.searchManagedEntity (InventoryNavigator. (.getRootFolder service)) type name) )

(defn resource-pool [root dcname]
  (let [dc  (.searchManagedEntity (InventoryNavigator. root) "Datacenter" dcname)]
    (first (.searchManagedEntities (InventoryNavigator. dc) "ResourcePool"))))

(defn clone-spec []
  (doto 
    (VirtualMachineCloneSpec.)
    (.setLocation 
      (doto 
        (VirtualMachineRelocateSpec.)
        (.setPool (.getMOR (resource-pool (.getRootFolder (connect (get* :hypervisor :vmware))) "playground")))))
    (.setPowerOn false)
    (.setTemplate false)))

(defn clone [template vmname]
  (let [service (connect (get* :hypervisor :vmware)) vm (search- service template) 
        task (.cloneVM_Task vm (.getParent vm) vmname (clone-spec)) status (.waitForMe task)]
    (println status) 
    ))

(comment
  (bean (.getGuest (first (list-vms (connect "https://192.168.5.23/sdk" "Administrator" "Oox1kai7")))))
  (clone "ubuntu-13.04_puppet-3.1" "foo")
  )


