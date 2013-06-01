(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns celestial.workflows
  "Main workflows"
  (:use 
    [celestial.common :only (get!)]
    [clojure.core.strint :only (<<)]
    [celestial.common :only (slurp-edn)]
    [taoensso.timbre :only (debug info trace)] 
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (vconstruct pconstruct rconstruct)]) 
  (:require ; loading defmethods
    proxmox.provider aws.provider vsphere.provider
    celestial.puppet_standalone capistrano.remoter
    [celestial.persistency :as p])
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

(defn post-create-hooks 
  "Runs post creation hooks"
  [machine]
  (doseq [[f args] (get! :hooks :post-create)]
    (debug "running hook"  f (resolve f))
    ((resolve- f) (merge machine args))))

(defn reload 
  "Sets up a clean machine from scratch"
  [{:keys [machine] :as spec}]
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

(defn destroy 
  "Deletes a system"
  [id {:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (when (.status vm)
      (.stop vm) 
      (.delete vm)) 
    (p/delete-system id)
    (info "system destruction done")))

(defn puppetize 
  "Provisions an instance"
  [type spec]
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

(defn run-action
  "Runs an action"
  [actions run-info]
 (let [remote (rconstruct actions run-info) {:keys [action]} run-info]
   (info (<< "setting up task ~{action}"))
   (.setup remote)
   (info (<< "running up task ~{action}"))
   (.run remote)
   (info (<< "cleanning up task ~{action}"))
   (.cleanup remote)))

