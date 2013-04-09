(ns celestial.test.proxmox
  (:use 
    midje.sweet
    [proxmox.provider :only (vzctl enable-features)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [celestial.fixtures :only (redis-prox-spec with-conf)])
  )

(with-conf
  (let [{:keys [machine proxmox]} redis-prox-spec]
    (fact "missing vmid"
          (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] nil)) => (throws java.lang.AssertionError))
    (fact "non int vmid"
          (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] "string")) => (throws java.lang.AssertionError))
    ))


(with-conf 
  (let [ct (vconstruct (assoc-in redis-prox-spec [:proxmox :features] ["nfs:on"]))]
    (fact "vzctl usage"
          (enable-features ct) => '()
          (provided 
            (vzctl ct "set 33 --features \"nfs:on\" --save") => nil :times 1)))) 
