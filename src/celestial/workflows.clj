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
    [celestial.common :only (get! import-logging)]
    [clojure.core.strint :only (<<)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (vconstruct pconstruct rconstruct)]) 
  (:require ; loading defmethods
    proxmox.provider aws.provider vc.provider
    celestial.puppet_standalone 
    remote.capistrano remote.ruby
    [celestial.persistency :as p]
    [celestial.persistency.systems :as s]
    [clojure.tools.macro :as tm]
   )
  (:import 
    [celestial.puppet_standalone Standalone]
    [proxmox.provider Container]))

(import-logging)

(defn resolve- [fqn-fn]
  ;(resolve- (first (keys (get-in config [:hooks :post-create]))))
  (let [[n f] (.split (str fqn-fn) "/")] 
    (try+
      (require (symbol n))
      (ns-resolve (find-ns (symbol n)) (symbol f)) 
     (catch java.io.FileNotFoundException e
       (throw+ {:type ::hook-missing :message (<<  "Could not locate hook ~{fqn-fn}")}))))) 

(defn run-hooks 
  "Runs hooks"
  [args workflow event]
  (doseq [[f conf] (get! :hooks)]
    (debug "running hook"  f (resolve f))
    (try 
      ((resolve- f) (merge args conf {:workflow workflow :event event}))
      (catch Throwable t (error t)) 
      )))

(defmacro deflow
  "Defines a basic flow functions with post-success and post-error hooks"
  [fname & args]
  (let [[name* attrs] (tm/name-with-attributes fname args)
        meta-map (meta name*) 
        hook-args (or (some-> (meta-map :hook-args) name symbol) 'spec)]
    `(defn ~name* ~@(when (seq meta-map) [meta-map]) ~(first attrs)
       (try ~@(next attrs)
         (run-hooks ~hook-args ~(keyword name*) :success)
         (catch Throwable t#
           (run-hooks ~hook-args ~(keyword name*) :error) 
           (throw t#))))))

(deflow reload 
  "Reloads a machine if one already exists, will distroy the old one"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "setting up" machine)
    (when (.status vm)
      (info "clearing prevsious" machine)
      (.stop vm) 
      (.delete vm)) 
    (let [vm* (.create vm)]  
      (.start vm*) 
      (assert (= (.status vm*) "running")) ; might not match all providers
      (info "done system setup"))))

(deflow stop
  "Stops a vm instance"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "stopping" machine)
    (.stop vm) 
    (assert (not (= (.status vm) "running"))) ; might not match all providers
    ))

(deflow start 
  "Start a vm instance"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "starting" machine)
    (.start vm) 
    (assert (= (.status vm) "running")) ; might not match all providers
    ))

(deflow create
  "Sets up a clean machine from scratch"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "setting up" machine)
    (when (.status vm)
       (println (.status vm))
       (throw+ {:type ::machine-exists :msg "can't create an existing machine"})) 
    (let [vm* (.create vm)]  
      (.start vm*) 
      (assert (= (.status vm*) "running")) ; might not match all providers
      (info "done system setup"))))

(deflow destroy 
  "Deletes a system"
  [{:keys [system-id machine] :as spec}]
  (let [vm (vconstruct spec)]
    (when (.status vm)
      (.stop vm) 
      (.delete vm)) 
    (s/delete-system system-id)
    (info "system destruction done")))

(deflow puppetize 
  "Provisions an instance"
  [type {:keys [machine] :as spec}]
    (info "starting to provision") 
    (trace type spec) 
    (assert (= (.status (vconstruct spec)) "running")) ; might not match all providers
    (.apply- (pconstruct type spec)) 
    (info "done provisioning"))

(defn stage
  "create and provision"
  [type {:keys [system-id] :as spec}] 
  (create spec) 
  (start spec)
  (puppetize type (assoc (s/get-system system-id) :system-id system-id)))

(deflow ^{:hook-args :run-info} run-action
  "Runs an action"
  [actions run-info]
  (let [remote (rconstruct actions run-info) {:keys [action]} run-info]
    (info (<< "setting up task ~{action}"))
    (.setup remote)
    (info (<< "running up task ~{action}"))
    (.run remote)
    (info (<< "cleanning up task ~{action}"))
    (.cleanup remote)))

