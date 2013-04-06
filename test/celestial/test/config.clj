(ns celestial.test.config
  (:use 
    midje.sweet
    [celestial.fixtures :only (local-prox)]
    [flatland.useful.map :only (dissoc-in*)]
    [celestial.config :only (validate-conf proxmox-v)]))

(defn validate-missing [& ks]
  (-> (validate-conf (dissoc-in* local-prox ks)) :bouncer.core/errors vals))

(fact "legal configuration"
      (:bouncer.core/errors (validate-conf local-prox))  => nil)

(fact "missing celestial options detected"
      (validate-missing :celestial :https-port) => (contains {:https-port '("https-port must be present")})  
      (validate-missing :celestial :port) => (contains {:port '("port must be present")}))

(fact "missing proxmox options"
      (validate-missing :hypervisor :proxmox :password) =>
      (contains {:proxmox {:password '("password must be present")}} ))


(fact "missing aws options"
      (get-in 
        (validate-conf 
          (assoc-in local-prox [:hypervisor :aws] {})) [:bouncer.core/errors :hypervisor :aws]) =>
      (contains {:access-key '("access-key must be present")}))
