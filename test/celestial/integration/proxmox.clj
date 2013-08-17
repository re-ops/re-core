(ns celestial.integration.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:require 
     [supernal.sshj :refer (execute)]
     [flatland.useful.map :refer (dissoc-in*)]
     [celestial.common :refer (slurp-edn)]
     [celestial.fixtures :refer (local-prox with-conf redis-prox-spec 
                                 redis-bridged-prox clustered-prox proxmox-3)]  
     [celestial.model :refer (vconstruct)])
  (:use midje.sweet proxmox.provider))


(def fake-id (update-in redis-prox-spec [:proxmox :vmid] (fn [o] 190)))

(fact "stoping and deleting missing clients" :integration :proxmox 
  (with-conf
    (let [ct (vconstruct fake-id)]
    (.stop ct)
    (.delete ct))))

; generators 

(defn running-seq [{:keys [network] :as ct}]
  (.stop ct)
  (.delete ct) 
  (.create ct) 
  (.start ct)
  (.status ct) => "running"
  ; making sure its ssh-able
  (execute "touch /tmp/foo" {:host (:ip_address network) :user "root"}) => nil
  (.stop ct)
  (.delete ct))

(fact "non generated" :integration :proxmox 
  (with-conf
    (running-seq (vconstruct redis-prox-spec))))

(fact "ip and vmid generated" :integration :proxmox
   (with-conf
    (running-seq 
      (vconstruct (-> redis-prox-spec (dissoc-in* [:machine :ip]) (dissoc-in* [:proxmox :vmid]))))))

(fact "bridged" :integration :proxmox :bridge
   (with-conf
      (running-seq (vconstruct redis-bridged-prox))))

(fact "cluster" :integration :proxmox :bridge :cluster
   (with-conf clustered-prox
      (running-seq (vconstruct (assoc-in redis-bridged-prox [:proxmox :node] "proxmox-b")))))

#_(fact "proxmox 3" :integration :proxmox-3 
   (with-conf proxmox-3 
     (running-seq (vconstruct (assoc-in redis-bridged-prox [:proxmox :node] "proxmox-3")))))

(fact "centos bridge" :integration :proxmox :bridge :centos
    (with-conf clustered-prox
      (running-seq (vconstruct (assoc-in redis-bridged-prox [:machine :os] :centos-6)))))

