(ns celestial.tasks
  "misc development tasks"
  (:use 
    [celestial.common :only (slurp-edn)]
    [taoensso.timbre :only (debug info trace)]) 
  (:import 
    [celestial.puppet_standalone Standalone]
    [proxmox.provider Container]))

(defn reload [{:keys [machine]}]
  "Sets up a clean machine from scratch"
  (let [{:keys [hypervisor]} machine ct (Container. hypervisor machine)]
    (info "setting up" machine "on" hypervisor)
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (assert (= (.status ct) "running"))
    (info "done system setup")))

(defn puppetize [{:keys [module machine] :as provision} ]
  (info "starting to provision" )
  (trace provision) 
  (.apply- (Standalone. machine module))
  (info "done provisioning"))

(defn full-cycle
  ([{:keys [system hypervisor provision]}] (full-cycle system hypervisor))
  ([system provision]
   (reload system) 
   (puppetize provision)))

(defn -main [t spec & _]
  (let [{:keys [system provision]} (slurp-edn spec)]
    (case t
      "reload" (reload system)
      "puppetize" (puppetize provision))))


