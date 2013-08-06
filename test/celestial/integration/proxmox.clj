(ns celestial.integration.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:require 
     [flatland.useful.map :refer (dissoc-in*)]
     [celestial.common :refer (slurp-edn)]
     [celestial.fixtures :refer (with-conf redis-prox-spec redis-bridged-prox-spec)]  
     [celestial.model :refer (vconstruct)])
  (:use midje.sweet proxmox.provider))


(def fake-id (update-in redis-prox-spec [:proxmox :vmid] (fn [o] 190)))

(fact "stoping and deleting missing clients" :integration :proxmox 
  (with-conf
    (let [ct (vconstruct fake-id)]
    (.stop ct)
    (.delete ct))))

; generators 

(defn running-seq [ct]
  (.stop ct)
  (.delete ct) 
  (.create ct) 
  (.start ct)
  (.status ct) => "running"
  (.stop ct)
  (.delete ct))

(fact "non generated" :integration :proxmox 
  (with-conf
    (running-seq (vconstruct redis-prox-spec))))

(fact "ip and vmid generated" :integration :proxmox
   (with-conf
    (running-seq 
      (vconstruct (-> redis-prox-spec (dissoc-in* [:machine :ip]) (dissoc-in* [:proxmox :vmid]))))))

(fact "bridged" :integration :proxmox
   (with-conf
      (running-seq (vconstruct redis-bridged-prox-spec))))
