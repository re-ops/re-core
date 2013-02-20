(ns celestial.integration.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:use clojure.test 
        proxmox.provider
        [celestial.common :only (config slurp-edn)]
        )
  (:import 
    [proxmox.provider Container]))

(def spec (slurp-edn "fixtures/redis-system.edn"))

(def local-prox 
  {:hypervisor {:username "root" :password "foobar" :host "localhost" :ssh-port 22222}})

(def fake-id (update-in spec [:machine :vmid] (fn [o] 190)))

(alter-var-root (var config) (fn [old] local-prox))

(deftest ^:proxmox non-existing 
  (let [{:keys [machine]} fake-id  ct (Container. (machine :hypervisor) machine)]
    (.stop ct)
    (.delete ct)))

(deftest ^:proxmox full-cycle
  (let [{:keys [machine]} spec ct (Container. (machine :hypervisor) machine)]
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (is (= (.status ct) "running"))))

