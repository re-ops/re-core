(ns celestial.tasks
  "misc development tasks"
  (:use 
    [celestial.common :only (slurp-edn)]
    [taoensso.timbre :only (debug info error warn)]) 
  (:import 
    [celestial.puppet_standalone Standalone]
    [proxmox.provider Container]))

(defn reload [{:keys [system]}]
  "Sets up a clean machine from scratch"
  (let [{:keys [hypervisor]} system ct (Container. hypervisor system)]
    (info "setting up" system "on" hypervisor)
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (assert (= (.status ct) "running"))
    (info "done system setup")))

(defn puppetize [{:keys [server module] :as provision} ]
  (info "starting to provision" provision)
  (.apply- (Standalone. server module) )
  (info "done provisioning" provision))

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


