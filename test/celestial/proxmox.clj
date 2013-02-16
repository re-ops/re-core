(ns celestial.proxmox
  "Integration tests assume a proxmox vm with local address make sure to configure it"
  (:use clojure.test 
        proxmox.provider
        [celestial.common :only (config)]
        )
  (:import 
    [proxmox.provider Container]))

(def spec 
  {:vmid 203 :ostemplate  "local:vztmpl/ubuntu-12.04-puppet_3-x86_64.tar.gz"
   :cpus  4 :memory  4096 :hostname  "foobar" :disk 30
   :ip_address  "192.168.1.30" :password "foobar1" :hypervisor "proxmox"})

(def local-prox 
  {:hypervisor  {:username "root" :password "foobar" :host "localhost" :ssh-port 2222}})

(def fake-id (assoc spec :vmid 190))

(alter-var-root (var config) (fn [old] local-prox))

(deftest ^:proxmox non-existing 
  (let [{:keys [hypervisor]} fake-id  ct (Container. hypervisor fake-id)]
    (.stop ct)
    (.delete ct)))

(deftest ^:proxmox full-cycle
  (let [{:keys [hypervisor]} spec  ct (Container. hypervisor spec)]
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (is (= (.status ct) "running"))))

