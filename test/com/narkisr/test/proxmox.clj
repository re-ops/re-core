(ns com.narkisr.test.proxmox
  (:use 
    [slingshot.slingshot :only  [throw+ try+]]
    clojure.test com.narkisr.proxmox.provider)
  (:import 
    [com.narkisr.proxmox.provider Container]))

(def spec 
  {:vmid 203 :ostemplate  "local:vztmpl/ubuntu-12.04-x86_64.tar.gz"
   :cpus  4 :memory  4096 :hostname  "tk-storage-3" :disk 30
   :ip_address  "192.168.20.203" :password "foobar1"})

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
    (is (= (.status ct) "running"))
    ))

