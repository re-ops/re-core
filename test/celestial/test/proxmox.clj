(ns celestial.test.proxmox
  (:use 
    [proxmox.remote :only (prox-get)]
    [proxmox.provider :only (vzctl enable-features ->Container)]
    [celestial.common :only (config)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (vconstruct)]
    [celestial.fixtures :only (redis-prox-spec local-prox)]
    expectations) 
  (:import 
    [proxmox.provider Container]))

(let [{:keys [machine proxmox]} redis-prox-spec]
  (expect java.lang.AssertionError 
       (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] nil)))
    (expect java.lang.AssertionError 
       (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] "string")))
    )


; not sure why but with-redefs dose not work
(alter-var-root (var prox-get) (fn [_] (fn [_] (throw+ {:status 500}))))

(with-redefs [config local-prox]
  (let [ct (vconstruct (assoc-in redis-prox-spec [:proxmox :features] ["nfs:on"]))]
    (expect 
      (interaction (vzctl ct "set 33 --features \"nfs:on\" --save"))
      (enable-features ct)) 

    (expect false (.status ct)))) 
