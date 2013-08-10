(ns celestial.test.config
  (:use 
    midje.sweet
    [celestial.fixtures :only (local-prox)]
    [flatland.useful.map :only (dissoc-in*)]
    [celestial.config :only (validate-conf proxmox-v)]))

(defn validate-missing [& ks]
  (validate-conf (dissoc-in* local-prox ks)))

(fact "legal configuration"
      (:bouncer.core/errors (validate-conf local-prox))  => nil)

(fact "missing celestial options detected"
      (validate-missing :celestial :https-port) =>  {:celestial {:https-port '("must be present")}}  
      (validate-missing :celestial :port) => {:celestial {:port '("must be present")}})

(fact "missing proxmox options"
    (validate-missing :hypervisor :proxmox :nodes :proxmox-a :password) =>  
      '{:hypervisor {:proxmox {:nodes (({:proxmox-a {:password ("must be present")}}))}}} )


(fact "missing aws options"
    (validate-conf (assoc-in local-prox [:hypervisor :aws] {})) => 
      {:hypervisor {:aws {:access-key '("must be present") :secret-key '("must be present")}}})

(fact "vcenter validations" 
   (validate-missing :hypervisor :vcenter :password) => {:hypervisor {:vcenter {:password '("must be present")}}}
   (validate-missing :hypervisor :vcenter :url) => {:hypervisor {:vcenter {:url '("must be present")}}}
   (validate-conf (assoc-in local-prox [:hypervisor :vcenter :ostemplates] [])) => {:hypervisor {:vcenter {:ostemplates '("must be a map")}}}
  )
