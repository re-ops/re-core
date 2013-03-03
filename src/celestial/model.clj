(ns celestial.model 
  "Model manipulation ns"
  )

(def hypervizors #{:proxmox :aws :vsphere :vagrant})

(defn figure-virt [spec] (first (filter hypervizors (keys spec))))

(defmulti translate
  "Converts general model to specific virtualization model" 
  (fn [spec] (figure-virt spec)))

(defmulti construct 
  "Creates a model based on require kind and given spec"
  (fn [spec] [(:kind spec) (figure-virt spec) ]))

(defn create-vm [spec] (construct (assoc spec :kind :vm)))

(defn create-prov [spec] (construct (assoc spec :kind :prov)))

; (create-vm (celestial.common/slurp-edn "fixtures/redis-system.edn")) 
