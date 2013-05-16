(ns celestial.integration.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:require [taoensso.carmine :as car])
  (:use midje.sweet
        proxmox.provider
        [celestial.redis :only (wcar clear-all)]
        [proxmox.generators :only (initialize-range long-to-ip)]
        [flatland.useful.map :only  (dissoc-in*)]
        [celestial.common :only (slurp-edn)]
        [celestial.model :only (vconstruct)]
        [celestial.fixtures :only (with-conf redis-prox-spec)])
  (:import [proxmox.provider Container]))


(def fake-id (update-in redis-prox-spec [:proxmox :vmid] (fn [o] 190)))

(fact "stoping and deleting missing clients" :integration :proxmox 
  (with-conf
    (let [ct (vconstruct fake-id)]
    (.stop ct)
    (.delete ct))))

; generators 

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

(with-state-changes [(before :facts (clear-all))]
  (fact "used up ips marking" :integration :redis
    (with-conf
      (do 
        (initialize-range)
        (map #(-> % Long/parseLong long-to-ip) (wcar (car/zrangebyscore "ips" 1 1))) => 
            ["192.168.5.170" "192.168.5.171" "192.168.5.173"]))))
