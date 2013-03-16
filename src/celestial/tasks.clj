(ns celestial.tasks
  "misc development tasks"
  (:use 
    [celestial.common :only (config)]
    [clojure.core.strint :only (<<)]
    [celestial.common :only (slurp-edn)]
    [taoensso.timbre :only (debug info trace)] 
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (vconstruct pconstruct)]) 
  (:require proxmox.provider aws.provider celestial.puppet_standalone); loading defmethods
  (:import 
    [celestial.puppet_standalone Standalone]
    [proxmox.provider Container]))

(defn resolve- [fqn-fn]
  ;(resolve- (first (keys (get-in config [:hooks :post-create]))))
  (let [[n f] (.split (str fqn-fn) "/")] 
    (try+
      (require (symbol n))
      (ns-resolve (find-ns (symbol n)) (symbol f)) 
     (catch java.io.FileNotFoundException e
       (throw+ {:type ::hook-missing :message (<<  "Could not locate hook ~{fqn-fn}")}))))) 

(defn post-create-hooks [machine]
  (doseq [[f args] (get-in config [:hooks :post-create])]
    (debug "running hook"  f (resolve f))
    ((resolve- f) (merge machine args))    
    ))

(defn reload [{:keys [machine] :as spec}]
  "Sets up a clean machine from scratch"
  (let [vm (vconstruct spec)]
    (info "setting up" machine)
    (when (.status vm)
      (.stop vm) 
      (.delete vm)) 
    (.create vm) 
    (.start vm)
    (assert (= (.status vm) "running")); might not match all providers
    (post-create-hooks machine)
    (info "done system setup")))

(defn puppetize [type spec]
  (info "starting to provision")
  (trace type spec) 
  (.apply- (pconstruct type spec))
  (info "done provisioning"))

(defn full-cycle
  ([{:keys [system hypervisor provision]}]
   (full-cycle system hypervisor))
  ([system provision]
   (reload system) 
   (puppetize provision)))

(defn -main [t spec & _]
  (let [{:keys [system provision]} (slurp-edn spec)]
    (case t
      "reload" (reload system)
      "puppetize" (puppetize provision))))


