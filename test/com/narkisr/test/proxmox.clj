(ns com.narkisr.test.proxmox
  (:use clojure.test com.narkisr.proxmox.provider)
  (:import 
    [com.narkisr.proxmox.provider Container]))

(def spec 
  {:vmid 203 :ostemplate  "local:vztmpl/ubuntu-12.04-puppet_3-x86_64.tar.gz"
   :cpus  4 :memory  4096 :hostname  "foobar" :disk 30
   :ip_address  "192.168.5.203" :password "foobar1"})

(deftest ^:integration non-existing 
 (let [ct (Container. "proxmox" (update-in spec [:vmid] (fn [v] 204)))]
    (.stop ct)
    (.delete ct)))

(deftest ^:integration full-cycle
  (let [ct (Container. "proxmox" spec)]
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (is (= (.status ct) "running"))))

