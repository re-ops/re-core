(ns celestial.integration.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:require 
   [hypervisors.networking :refer (clear-range initialize-networking)]
   [supernal.sshj :refer (execute)]
   [flatland.useful.map :refer (dissoc-in*)]
   [celestial.common :refer (slurp-edn)]
   [celestial.fixtures.core :refer (with-defaults with-conf) :as f]  
   [celestial.fixtures.data :as d]
   [celestial.model :refer (vconstruct)])
  (:use midje.sweet proxmox.provider))


(def fake-id (update-in d/redis-prox-spec [:proxmox :vmid] (fn [o] 190)))

(with-defaults
 (fact "stoping and deleting missing clients" :integration :proxmox 
       (let [ct (vconstruct fake-id)]
         (.stop ct)
         (.delete ct))))

(defn running-seq [{:keys [network] :as ct}]
  (.stop ct)
  (.delete ct) 
  (let [ct* (.create ct)] 
    (.start ct*) 
    (.status ct*) => "running"
    ; making sure its ssh-able
    (execute "touch /tmp/foo" {:host (-> ct* :network :ip_address) :user "root"}) => nil 
    (.stop ct*) 
    (.delete ct*)))

(with-defaults
 (with-state-changes [(before :facts (do (clear-range :proxmox) (initialize-networking)))] 
    (fact "non generated ip" :integration :proxmox :generators
       (running-seq (vconstruct d/redis-prox-spec))) 

     (fact "ip and vmid generated" :integration :proxmox :generators
       (running-seq 
         (vconstruct (-> d/redis-prox-spec (dissoc-in* [:machine :ip]) (dissoc-in* [:proxmox :vmid]))))) 

     (fact "bridged address" :integration :proxmox :bridge
       (running-seq (vconstruct d/redis-bridged-prox))) 

     (fact "cluster" :integration :proxmox :bridge :cluster
       (with-conf d/clustered-prox
         (running-seq (vconstruct (assoc-in d/redis-bridged-prox [:proxmox :node] "proxmox-b"))))) 

     (fact "proxmox 3" :integration :proxmox-3 
       (with-conf d/proxmox-3 
         (running-seq (vconstruct (assoc-in d/redis-bridged-prox [:proxmox :node] "proxmox-3")))))

     (fact "centos bridge" :integration :proxmox :bridge :centos
       (with-conf d/clustered-prox
         (running-seq (vconstruct (assoc-in d/redis-bridged-prox [:machine :os] :centos-6)))))))



