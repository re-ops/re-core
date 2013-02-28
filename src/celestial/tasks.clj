(ns celestial.tasks
  "misc development tasks"
  (:use 
    [celestial.common :only (config)]
    [celestial.common :only (slurp-edn)]
    [taoensso.timbre :only (debug info trace)]) 
  (:import 
    [celestial.puppet_standalone Standalone]
    [proxmox.provider Container]))

(defn resolve- [fqn-fn]
  ;(resolve- (first (keys (get-in config [:hooks :post-create]))))
  (let [[n f] (.split (str fqn-fn) "/")] 
    (require (symbol n))
    (ns-resolve (find-ns (symbol n)) (symbol f)) 
    )) 

(defn post-create-hooks [machine]
  (doseq [[f args] (get-in config [:hooks :post-create])]
    (debug "running hook"  f (resolve f))
    ((resolve- f) (merge machine args))    
    ))

(defn reload [{:keys [machine]}]
  "Sets up a clean machine from scratch"
  (let [{:keys [hypervisor]} machine ct (Container. hypervisor machine)]
    (info "setting up" machine "on" hypervisor)
    (.stop ct)
    (.delete ct) 
    (.create ct) 
    (.start ct)
    (assert (= (.status ct) "running"))
    (post-create-hooks machine)
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


