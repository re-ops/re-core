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
    [celestial.common :only (get! import-logging resolve-)]
    [clojure.core.strint :only (<<)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (vconstruct pconstruct rconstruct)]) 
  (:require ; loading defmethods
    proxmox.provider 
    freenas.provider 
    aws.provider 
    vc.provider 
    physical.provider 
    docker.provider
    openstack.provider
    celestial.puppet_standalone 
    remote.capistrano remote.ruby
    ;cloning
    docker.model 
    aws.model 
    proxmox.model
    openstack.model
    [celestial.persistency.systems :as s]
    [clojure.tools.macro :as tm]
    [metrics.timers :refer  [deftimer time!]]
   )
  (:import 
    [celestial.puppet_standalone Standalone]
    ))

(import-logging)

(defn run-hooks 
  "Runs hooks"
  [args workflow event]
  (doseq [[f conf] (get! :hooks)]
    (debug "running hook" f (resolve- f))
    (try 
      ((resolve- f) (merge args conf {:workflow workflow :event event}))
      (catch Throwable t (error t)) 
      )))

(defmacro deflow
  "Defines a basic flow functions with post-success and post-error hooks"
  [fname & args]
  (let [[name* attrs] (tm/name-with-attributes fname args)
        timer (symbol (str name* "-time"))
        meta-map (meta name*) 
        hook-args (or (some-> (meta-map :hook-args) name symbol) 'spec)]
    `(do
       (deftimer ~timer)
       (defn ~name* ~@(when (seq meta-map) [meta-map]) ~(first attrs)
         (time! ~timer
          (try ~@(next attrs)
           (run-hooks ~hook-args ~(keyword name*) :success)
           (catch Throwable t#
             (run-hooks ~hook-args ~(keyword name*) :error) 
             (throw t#))))))))

(defn updated-system 
   "grabs system and associates system id" 
   [system-id]
   {:pre [system-id]}
   (assoc (s/get-system! system-id) :system-id system-id))

(defn running! 
  "Asserts that a VM is running"
  [vm]
  (assert (= (.status vm) "running")) ; might not match all providers
  )
 
(defn not-running!
  "Assert that a vm is not running"
   [vm]
  (assert (not (= (.status vm) "running"))) ; might not match all providers
  )

(deflow reload 
  "Reloads a machine if one already exists, will distroy the old one"
  [{:keys [machine system-id] :as spec}]
  (let [vm (vconstruct spec)]
    (info "setting up" machine)
    (when (.status vm)
      (info "clearing previous" machine)
      (.stop vm) 
      (.delete vm)) 
    (let [vm* (.create (vconstruct (updated-system system-id)))]  
      (.start vm*) 
      (running! vm*)
      (info "done system setup"))))

(deflow stop
  "Stops a vm instance"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "stopping" machine)
    (.stop vm) 
    (not-running! vm)
    ))

(deflow start 
  "Start a vm instance"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (not-running! vm)
    (info "starting" machine)
    (.start vm) 
    (running! vm)
    ))

(deflow create
  "Sets up a clean machine from scratch"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "setting up" machine)
    (when (.status vm)
       (throw+ {:type ::machine-exists} "can't create an existing machine")) 
    (let [vm* (.create vm)]  
      (.start vm*) 
      (running! vm*)
      (info "done system setup"))))

(deflow destroy 
  "Deletes a system"
  [{:keys [system-id machine] :as spec}]
  (let [vm (vconstruct spec)]
    (when (.status vm)
      (when (= (.status vm) "running") (.stop vm)) 
      (.delete vm)) 
    (s/delete-system system-id)
    (info "system destruction done")))

(deflow ^{:hook-args :spec} clone
  "Clones a system model and creates it"
  [{:keys [system-id] :as spec}]
   (when-not (s/system-exists? system-id)
      (throw+ {:type ::system-missing} (<< "Could not clone missing system ~{system-id}")))
    (let [id (s/clone-system system-id spec)]
      (create (assoc (s/get-system id) :system-id id)) 
      (info "system cloned into" id)))

(deflow clear
  "Clear system model (no machine destruction)"
  [{:keys [system-id] :as spec}]
    (s/delete-system system-id)
    (info "system deleted"))


(deflow provision
  "Provisions an instance"
  [type {:keys [machine] :as spec}]
     (info "starting to provision") 
     (trace type spec) 
     (running! (vconstruct spec))
     (.apply- (pconstruct type spec)) 
     (info "done provisioning"))

(defn stage
  "create and provision"
  [type {:keys [system-id] :as spec}] 
  (create spec) 
  (when-not (= (.status (vconstruct (updated-system system-id))) "running"); some providers already start the vm (AWS, vCenter)
    (start (updated-system system-id))) 
  (provision type (updated-system system-id)))

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

