(ns celestial.test.proxmox
  (:use 
    [proxmox.remote :only (prox-get)]
    [proxmox.provider :only (vzctl enable-features ->Container)]
    [celestial.common :only (config)]
    [celestial.model :only (create-vm)]
    [celestial.fixtures :only (spec local-prox)]
    expectations.scenarios) 
  (:import 
    [proxmox.provider Container]))

(let [{:keys [machine proxmox]} spec]
  (scenario 
    (expect java.lang.AssertionError 
       (create-vm (assoc-in spec [:proxmox :vmid] nil)))
    (expect java.lang.AssertionError 
       (create-vm (assoc-in spec [:proxmox :vmid] "string") ))
    ))


(with-redefs [config local-prox]
  (def ct 
    (create-vm (assoc-in spec [:proxmox :features] ["nfs:on"]))))

(scenario 
  (enable-features ct) 
  (expect 
    (interaction (vzctl ct "set 33 --features \"nfs:on\" --save")) :once))

