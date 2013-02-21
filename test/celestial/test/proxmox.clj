(ns celestial.test.proxmox
  (:use 
    [proxmox.remote :only (prox-get)]
    [proxmox.provider :only (vzctl enable-features ->Container)]
    [celestial.common :only (slurp-edn)]
    expectations.scenarios) 
  (:import 
    [proxmox.provider Container]))

(def spec (slurp-edn "fixtures/redis-system.edn"))

(let [{:keys [machine]} spec]
  (def ct (->Container (machine :hypervisor) machine)))


(let [{:keys [machine]} spec]
  (scenario 
    (expect java.lang.AssertionError 
       (->Container (machine :hypervisor) (dissoc machine :vmid)))
    (expect java.lang.AssertionError 
       (->Container (machine :hypervisor) (assoc machine :vmid "string")))
    ))

(scenario 
  (stubbing [prox-get "stopped"]
    (expect java.lang.AssertionError (vzctl ct "nfs:on"))))

(scenario 
  (enable-features ct {:vmid 1 :features ["nfs:on"]}) 
  (expect 
    (interaction (vzctl ct "set 1 --features \"nfs:on\" --save"))))

