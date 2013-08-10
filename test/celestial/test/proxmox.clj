(ns celestial.test.proxmox
  (:require 
    [celestial.fixtures :as fix :refer [redis-prox-spec redis-bridged-prox with-conf with-m?]] 
    [hypervisors.networking :as n]
    [flatland.useful.map :refer  (dissoc-in*)]
    [proxmox.validations :refer (validate-entity)]
    [proxmox.provider :as prox :refer (vzctl enable-features assign-networking)])
  (:use 
    midje.sweet
    [clojure.core.strint :only (<<)]
    [celestial.common :only (curr-time)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [proxmox.generators :only (ct-id)]
    [proxmox.auth :only (fetch-headers auth-headers auth-store auth-expired?)])
  (:import clojure.lang.ExceptionInfo))

(defn nulify [spec & ks] (assoc-in spec ks nil))

(with-conf
  (let [{:keys [machine proxmox]} redis-prox-spec]
    (with-redefs [ct-id (fn [_] (fn [_] nil))]
      (fact "missing vmid"
            (vconstruct (nulify redis-prox-spec :proxmox :vmid)) => 
            (throws ExceptionInfo (with-m? {:ct {:vmid '("must be present")}} ))))
    (fact "non int vmid"
          (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] "string")) => 
          (throws ExceptionInfo (with-m? {:ct {:vmid '("must be a number")}})))
    (with-redefs [ct-id (fn [_] 101)]
      (let [ct (vconstruct (assoc-in redis-prox-spec [:proxmox :features] ["nfs:on"]))]
        (fact "vzctl usage"
              (enable-features ct) => '()
              (provided 
                (vzctl ct "set 101 --features \"nfs:on\" --save") => nil :times 1))

        )))) 

(with-conf
  (let [headers {"Cookie" "PVEAuthCookie=" "CSRFPreventionToken" "foobar"}]
    (fact "auth initialization"
          (reset! auth-store {})
          (auth-headers) => headers
          (provided 
            (fetch-headers) => headers :times 1))
    (fact "auth expiry"
          (auth-headers) => headers
          (provided 
            (fetch-headers) => headers :times 1
            (auth-expired?) => true :times 1)) 
    (fact "modified time"
          (:modified @auth-store) => (roughly (curr-time)))))

; networking
(with-conf
  (let [{:keys [ct network] :as bridged} (vconstruct fix/redis-bridged-prox) ]
    (fact "bridged construction"
          network => (contains {:gateway "192.168.5.255" :ip_address "192.168.5.200"
                                :netif "ifname=eth0,bridge=vmbr0" :netmask "255.255.255.0"}))
    (fact "bridged with ip apply"
          (assign-networking ct network) => 
              (contains (contains {:netif "ifname=eth0,bridge=vmbr0"}) (contains {:ip_address "192.168.5.200"}))
          (provided 
            (n/mark "192.168.5.200" "proxmox") =>  "192.168.5.200")))

  (let [{:keys [ct network] :as bridged-no-ip} (vconstruct (dissoc-in* fix/redis-bridged-prox [:machine :ip])) ]
    (fact "bridged noip construction"
          network => (contains {:gateway "192.168.5.255" :netif "ifname=eth0,bridge=vmbr0" :netmask "255.255.255.0"}))

    (fact "bridged with noip apply"
          (assign-networking ct network) => 
              (contains (contains {:netif "ifname=eth0,bridge=vmbr0"}) (contains {:ip_address "192.168.5.201"}))
          (provided 
            (n/gen-ip anything "proxmox") =>  {:ip_address "192.168.5.201"})))

  (let [non-bridged-no-ip (reduce dissoc-in* fix/redis-bridged-prox [[:machine :ip] [:machine :bridge]])
        {:keys [ct network] :as non-bridged-no-ip} (vconstruct non-bridged-no-ip) ]

    (fact "non-bridged noip construction" network => (contains {:gateway "192.168.5.255" :netmask "255.255.255.0"}))

    (fact "non-bridged with noip apply"
          (assign-networking ct network) => (contains (contains {:ip_address "192.168.5.202"}))
          (provided 
            (n/gen-ip anything "proxmox") =>  {:ip_address "192.168.5.202"})))

  (let [{:keys [ct network] :as with-ip} (vconstruct (dissoc-in* fix/redis-bridged-prox [:machine :bridge])) ]
    (fact "non bridged has ip construction"
          network => (contains {:gateway "192.168.5.255" :netmask "255.255.255.0"}))

    (fact "non bridged has ip apply"
          (assign-networking ct network) => (contains (contains {:ip_address "192.168.5.200"}))
          (provided 
            (n/mark "192.168.5.200" "proxmox") =>  "192.168.5.200"))))

(fact "proxmox entity validation"
   (validate-entity redis-prox-spec) => truthy

   (validate-entity (nulify redis-prox-spec :machine :cpus)) => ; common validation 
      (throws ExceptionInfo (with-m? {:machine {:cpus '("must be present")}}))

   (validate-entity (nulify redis-prox-spec :machine :domain)) => ; non fqdn
      (throws ExceptionInfo (with-m? {:machine {:domain '("must be present")}}))

   (validate-entity (assoc-in redis-prox-spec [:machine :os] "ubutnu-12.04")) => ; entity validation 
      (throws ExceptionInfo (with-m? {:machine {:os '("must be a keyword")}}))

   (validate-entity (assoc-in redis-prox-spec [:proxmox :vmid] 33)) => 
      (throws ExceptionInfo (with-m? {:proxmox {:vmid '("must be greater then 100")}}))

   (validate-entity (nulify redis-prox-spec :proxmox :vmid)) => truthy

   (validate-entity (nulify redis-prox-spec :proxmox :password)) => ; proxmox validation
      (throws ExceptionInfo (with-m? {:proxmox {:password '("must be present")}})))


(fact "bridged entity validation"
   (validate-entity redis-bridged-prox) => truthy

   (validate-entity (nulify redis-bridged-prox :machine :netmask)) => 
      (throws ExceptionInfo (with-m? {:machine {:netmask '("must be present")}}))

   (validate-entity (assoc-in redis-bridged-prox [:machine :netmask] "123")) => 
      (throws ExceptionInfo (with-m? {:machine {:netmask '("must be a legal ip address")}})))
