(ns celestial.test.proxmox
  (:require proxmox.provider)
  (:use 
    midje.sweet
    [proxmox.provider :only (vzctl enable-features)]
    [clojure.core.strint :only (<<)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [proxmox.generators :only (ct-id)]
    [celestial.fixtures :only (redis-prox-spec with-conf)])
  (:import clojure.lang.ExceptionInfo))

(defn with-m? [m]
  (fn [actual]
    (= (get-in (.getData actual) [:object :errors]) m)))


(with-conf
  (let [{:keys [machine proxmox]} redis-prox-spec]
    (with-redefs [ct-id (fn [_] nil)]
      (fact "missing vmid"
            (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] nil)) => 
            (throws ExceptionInfo (with-m? {:vmid '("vmid must be present")} ))))
    (fact "non int vmid"
          (vconstruct (assoc-in redis-prox-spec [:proxmox :vmid] "string")) => 
          (throws ExceptionInfo (with-m? {:vmid '("vmid must be a number")})))
    (with-redefs [ct-id (fn [_] 33)]
      (let [ct (vconstruct (assoc-in redis-prox-spec [:proxmox :features] ["nfs:on"]))]
        (fact "vzctl usage"
              (enable-features ct) => '()
              (provided 
                (vzctl ct "set 33 --features \"nfs:on\" --save") => nil :times 1)))))) 

