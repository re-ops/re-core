(ns celestial.integration.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:use clojure.test 
        proxmox.provider
        [celestial.common :only (config slurp-edn)]
        [celestial.model :only (construct)]
        )
  (:import 
    [proxmox.provider Container]))

(def spec (slurp-edn "fixtures/redis-system.edn"))

(def local-prox 
  {:hypervisor {:username "root" :password "foobar" :host "localhost" :ssh-port 22222}})

(def fake-id (update-in spec [:proxmox :vmid] (fn [o] 190)))

(alter-var-root (var config) (fn [old] local-prox))

(deftest ^:proxmox non-existing 
  (let [ct (construct fake-id) ]
    (.stop ct)
    (.delete ct)))

(deftest ^:proxmox full-cycle
  (let [{:keys [machine]} spec 
        ct (construct (dissoc machine :features))]
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (is (= (.status ct) "running"))))

