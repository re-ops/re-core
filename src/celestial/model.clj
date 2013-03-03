(ns celestial.model 
  "Model manipulation ns"
  )

(def hypervizors #{:proxmox :aws :vsphere :vagrant})

(defn figure-virt [spec] (first (filter hypervizors (keys spec))))

(defmulti translate
  "Converts general model to specific virtualization model" 
  (fn [spec] (figure-virt spec)))

(defmulti vconstruct 
  "Creates a Virtualized instance model from input spec" 
  (fn [spec] (figure-virt spec)))

(def provisioners #{:chef :puppet :puppet-std})

(defmulti pconstruct
  "Creates a Provisioner instance model from input spec" 
   (fn [type spec] (first (filter provisioners (keys type)))))

