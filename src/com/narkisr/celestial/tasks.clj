(ns com.narkisr.celestial.tasks
  "misc development tasks"
  (:use com.narkisr.proxmox.provider)
  (:import 
    [com.narkisr.celestial.puppet_standalone Standalone ]
    [com.narkisr.proxmox.provider Container]))

(def spec 
  {:vmid 203 :ostemplate  "local:vztmpl/ubuntu-12.04-puppet_3-x86_64.tar.gz"
   :cpus  4 :memory  4096 :hostname  "foobar" :disk 30
   :ip_address  "192.168.5.203" :password "foobar1"})

(defn slurp-edn [file] (read-string (slurp file)))

(defn reload [system hypervisor]
  "Sets up a clean machine from scratch"
  (let [ct (Container. hypervisor system)]
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (assert (= (.status ct) "running"))))

(defn puppetize [{:keys [server module]}]
  (.apply- (Standalone. server module)))

(defn -main [t spec & args]
  (let [{:keys [system provision]} (slurp-edn spec)]
    (case t
      "reload" (reload system)
      "puppetize" (puppetize provision (first args)))))


