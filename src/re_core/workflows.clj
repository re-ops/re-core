(ns re-core.workflows
  "Main workflows"
  (:require
   ; loading defmethods
   lxc.provider
   kvm.provider
   digital.provider
   aws.provider
   physical.provider
   ; cloning
   aws.model
   [re-core.repl.systems :as sys]
   [re-mote.repl :as mote]
   [es.systems :as s]
   [clojure.tools.macro :as tm]
   [metrics.timers :refer  [deftimer time!]]
   [clojure.core.strint :refer (<<)]
   [re-core.model :refer (vconstruct)]
   [taoensso.timbre :refer (refer-timbre)])
  (:import
   [re_core.repl.base Systems]))

(refer-timbre)

(defn updated-system
  "grabs system and associates system id"
  [system-id]
  {:pre [system-id]}
  (assoc (s/get! system-id) :system-id system-id))

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

(defn reload
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

(defn stop
  "Stops a vm instance"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "stopping" machine)
    (.stop vm)
    (not-running! vm)))

(defn start
  "Start a vm instance"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (not-running! vm)
    (info "starting" machine)
    (.start vm)
    (running! vm)))

(defn create
  "Sets up a clean machine from scratch"
  [{:keys [machine] :as spec}]
  (let [vm (vconstruct spec)]
    (info "setting up" machine)
    (when (.status vm)
      (throw (ex-info "can't create an existing machine" {:system spec})))
    (let [vm* (.create vm)]
      (.start vm*)
      (running! vm*)
      (info "done system setup"))))

(defn destroy
  "Deletes a system"
  [{:keys [system-id machine] :as spec}]
  (let [vm (vconstruct spec)]
    (when (.status vm)
      (when (= (.status vm) "running") (.stop vm))
      (.delete vm))
    (s/delete system-id)
    (info "system destruction done")))

(defn clone
  "Clones a system model and creates it"
  [{:keys [system-id] :as spec}]
  (when-not (s/exists? system-id)
    (throw (ex-info (<< "Could not clone missing system ~{system-id}") {:system spec})))
  (let [id (s/clone system-id spec)]
    (create (assoc (s/get id) :system-id id))
    (info "system cloned into" id)))

(defn clear
  "Clear system model (no machine destruction)"
  [{:keys [system-id] :as spec}]
  (s/delete system-id)
  (info "deleted system with id" system-id))

(defn provision
  "provision a group of systems"
  [systems t]
  (info "starting to provision hosts")
  (let [hosts (sys/into-hosts (Systems.) {:systems (map (fn [m] [(m :system-id) m]) systems)} :ip)]
    (mote/provision hosts t)
    (info "done provisioning hosts")))

(defn stage
  "create and provision"
  [type {:keys [system-id] :as spec}]
  (create spec)
  (when-not (= (.status (vconstruct (updated-system system-id))) "running"); some providers already start the vm (AWS, vCenter)
    (start (updated-system system-id)))
  (provision type (updated-system system-id)))

