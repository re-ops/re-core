(ns celestial.test.proxmox
  (:require 
    [celestial.fixtures :as fix :refer [redis-prox-spec with-conf with-m?]] 
    proxmox.provider)
  (:use 
    midje.sweet
    [proxmox.provider :only (vzctl enable-features)]
    [clojure.core.strint :only (<<)]
    [celestial.common :only (curr-time)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [proxmox.generators :only (ct-id)]
    [proxmox.auth :only (fetch-headers auth-headers auth-store auth-expired?)]
    )
  (:import clojure.lang.ExceptionInfo))

(with-conf
  (let [{:keys [machine proxmox]} redis-prox-spec]
    (with-redefs [ct-id (fn [_] (fn [_] nil))]
      (fact "missing vmid"
            (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] nil)) => 
            (throws ExceptionInfo (with-m? {:machine {:vmid '("vmid must be present")}} ))))
      (fact "non int vmid"
          (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] "string")) => 
          (throws ExceptionInfo (with-m? {:machine {:vmid '("vmid must be a number")}})))
    (with-redefs [ct-id (fn [_] 101)]
      (let [ct (vconstruct (assoc-in redis-prox-spec [:proxmox :features] ["nfs:on"]))]
        (fact "vzctl usage"
              (enable-features ct) => '()
              (provided 
                (vzctl ct "set 101 --features \"nfs:on\" --save") => nil :times 1))
        (fact "bridged constructions"
          (vconstruct fix/redis-bridged-prox-spec) => 
              (contains {:network {:gateway "192.168.5.255" :netmask "255.255.255.0"}}))
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

