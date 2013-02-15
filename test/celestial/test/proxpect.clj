(ns celestial.test.proxpect
  (:use 
    [proxmox.remote :only (prox-get)]
    [proxmox.provider :only (vzctl enable-features)]
    expectations.scenarios) 
  (:import 
    [proxmox.provider Container])
  )

(def spec 
  {:vmid 203 :ostemplate  "local:vztmpl/ubuntu-12.04-puppet_3-x86_64.tar.gz"
   :cpus  4 :memory  4096 :hostname  "foobar" :disk 30
   :ip_address  "192.168.20.170" :password "foobar1" :hypervisor "takadu"})

(def ct (Container. (spec :hypervisor) spec))

(scenario 
  (stubbing [prox-get "stopped"]
    (expect java.lang.AssertionError (vzctl ct "nfs:on"))))

(scenario 
   (enable-features ct {:vmid 1 :features ["nfs:on"]}) 
   (expect 
     (interaction (vzctl ct "set 1 --features \"nfs:on\" --save"))))
 
