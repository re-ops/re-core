(ns com.narkisr.test.proxmox
  (:use clojure.test com.narkisr.proxmox.provider)
  (:import 
    [com.narkisr.proxmox.provider Container]))

(def spec 
  {:vmid 203 :ostemplate  "local:vztmpl/ubuntu-12.04-x86_64.tar.gz"
   :cpus  4 :memory  4096 :hostname  "tk-storage-3" :disk 30
   :ip_address  "192.168.20.203" :password "foobar1"})

(deftest ^:integration creation 
  (let [ct (Container. "proxmox" spec)]
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    ))

