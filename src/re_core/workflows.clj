(ns re-core.workflows
  "Main workflows"
  (:require
    ; loading defmethods
   kvm.provider
   digital.provider
   aws.provider
   physical.provider
    ;cloning
   aws.model
   [re-core.persistency.systems :as s]
   [clojure.tools.macro :as tm]
   [metrics.timers :refer  [deftimer time!]]
   [re-core.common :refer (get! resolve-)]
   [clojure.core.strint :refer (<<)]
   [slingshot.slingshot :refer  [throw+ try+]]
   [re-core.model :refer (vconstruct pconstruct rconstruct)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defmacro deflow
  "Defines a basic flow functions"
  [fname & args]
  (let [[name* attrs] (tm/name-with-attributes fname args)
        timer (symbol (str name* "-time"))
        meta-map (meta name*)]
    `(do
       (deftimer ~timer)
       (defn ~name* ~@(when (seq meta-map) [meta-map]) ~(first attrs)
         (time! ~timer ~@(next attrs))))))

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
      (debug "clearing previous" machine)
      (.stop vm)
      (.delete vm))
    (let [vm* (.create (vconstruct (updated-system system-id)))]
      (.start vm*)
      (running! vm*)
      (debug "done system setup"))))

(deflow stop
  "Stops a vm instance"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "stopping" machine)
    (.stop vm)
    (not-running! vm)))

(deflow start
  "Start a vm instance"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (not-running! vm)
    (info "starting" machine)
    (.start vm)
    (running! vm)))

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

(deflow clone
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
  (let [vm (vconstruct spec)
        provisioner (pconstruct type (assoc-in spec [:machine :ip] (.ip vm)))]
    (running! vm)
    (.apply- provisioner))
  (info "done provisioning"))

(defn stage
  "create and provision"
  [type {:keys [system-id] :as spec}]
  (create spec)
  (when-not (= (.status (vconstruct (updated-system system-id))) "running"); some providers already start the vm (AWS, vCenter)
    (start (updated-system system-id)))
  (provision type (updated-system system-id)))

