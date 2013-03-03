(ns celestial.integration.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:use clojure.test 
        proxmox.provider
        [celestial.common :only (config slurp-edn)]
        [celestial.model :only (construct)]
        [celestial.fixtures :only (spec local-prox)]
        )
  (:import 
    [proxmox.provider Container]))


(def fake-id (update-in spec [:proxmox :vmid] (fn [o] 190)))

(deftest ^:proxmox non-existing 
  (with-redefs [config local-prox]
    (let [ct (construct fake-id) ]
    (.stop ct)
    (.delete ct))))

(deftest ^:proxmox full-cycle
  (with-redefs [config local-prox]
   (let [ct (construct spec )]
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (is (= (.status ct) "running")))))

