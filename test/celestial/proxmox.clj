(ns celestial.proxmox
  "Integration tests assume a proxmox vm with local address"
  (:use
    clojure.test 
    proxmox.provider)
  (:import 
    [proxmox.provider Container]))

(def spec 
  {:vmid 203 :ostemplate  "local:vztmpl/ubuntu-12.04-puppet_3-x86_64.tar.gz"
   :cpus  4 :memory  4096 :hostname  "foobar" :disk 30
   :ip_address  "192.168.5.203" :password "foobar1"})

(deftest ^:proxmox non-existing 
  (let [ct (Container. "proxmox" (update-in spec [:vmid] (fn [v] 204)))]
    (.stop ct)
    (.delete ct)))

(deftest ^:proxmox full-cycle
  (let [ct (Container. "proxmox" spec)]
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (is (= (.status ct) "running"))))

(deftest feature-enable 
  (let [action (atom "")
        prox (reify Openvz
               (vzctl [this a] (reset! action a)) 
               (unmount [this]))]
    (enable-features prox {:vmid 1 :features ["nfs:on"]})
    (is (= @action "set 1 --features \"nfs:on\" --save"))))
