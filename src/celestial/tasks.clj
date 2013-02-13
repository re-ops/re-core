(ns celestial.tasks
  "misc development tasks"
  (:use proxmox.provider
        [taoensso.timbre :only (debug info error warn)]) 
  (:import 
    [celestial.puppet_standalone Standalone]
    [proxmox.provider Container]))


(defn slurp-edn [file] (read-string (slurp file)))

(defn reload 
  ([{:keys [system hypervisor]}]
   (reload system hypervisor)) 
  ([system hypervisor]
   "Sets up a clean machine from scratch"
   (let [ct (Container. hypervisor system)]
     (info "setting up" system "on" hypervisor)
     (.stop ct)
     (.delete ct) 
     (.create ct) 
     (.start ct)
     (assert (= (.status ct) "running"))
     (info "done system setup"))))

(defn puppetize [{:keys [server module] :as spec} ]
  (info "starting to provision" spec)
  (.apply- (Standalone. server module) )
  (info "done provisioning" spec))

(defn full-setup 
  ([{:keys [system hypervisor]}] (full-setup system hypervisor))
  ([system provision]
   (reload system) 
   (puppetize provision)))

(defn -main [t spec & args]
  (let [{:keys [system provision]} (slurp-edn spec)]
    (case t
      "reload" (reload system)
      "puppetize" (puppetize provision))))


