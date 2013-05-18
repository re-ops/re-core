(ns celestial.integration.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:use midje.sweet
        proxmox.provider
        [flatland.useful.map :only (dissoc-in*)]
        [celestial.model :only (vconstruct)]
        [celestial.fixtures :only (with-conf redis-prox-spec)]))


(def fake-id (update-in redis-prox-spec [:proxmox :vmid] (fn [o] 190)))

(fact "stoping and deleting missing clients" :integration :proxmox 
  (with-conf
    (let [ct (vconstruct fake-id)]
    (.stop ct)
    (.delete ct))))

(fact "Full proxmox cycle" :integration :proxmox 
  (with-conf
    (let [ct (vconstruct redis-prox-spec)]
     (.stop ct)
     (.delete ct) 
     (.create ct) 
     (.start ct)
     (.status ct) => "running"
     (.stop ct)
     (.delete ct))))

(fact "ip and vmid generation" :integration :proxmox
   (with-conf
    (let [ct (vconstruct (-> redis-prox-spec (dissoc-in* [:machine :ip]) (dissoc-in* [:machine :vmid])))]
     (.stop ct)
     (.delete ct) 
     (.create ct) 
     (.start ct)
     (.status ct) => "running"
     (.stop ct)
     (.delete ct))))
