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

(ns proxmox.provider
  (:use 
    [celestial.validations :only (hash-v str-v validate! vec-v)]
    [trammel.core :only  (defconstrainedrecord)]
    [clojure.core.memoize :only (memo-ttl)]
    [clojure.core.strint :only (<<)]
    [bouncer [core :as b] [validators :as v]]
    [celestial.core :only (Vm)]
    [supernal.sshj :only (execute)]
    [celestial.common :only (get* import-logging)]
    [proxmox.remote :only (prox-post prox-delete prox-get)]
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.set :only (difference)]
    [celestial.provider :only (str? vec?)]
    [proxmox.generators :only (ct-id gen-ip release-ip)]
    [celestial.model :only (translate vconstruct)])
  (:require 
    [celestial.persistency :as p]
    )
  (:import clojure.lang.ExceptionInfo))

(import-logging)

(defn ct-v [c]
  (b/validate c 
     :vmid [v/required v/number]
     :ostemplate [v/required str-v] 
     :cpus [v/number v/required]
     :disk [v/number v/required]
     :memory [v/number v/required]
     :ip_address [str-v]
     :password [v/required str-v]
     :hostname [v/required str-v]
     :nameserver [str-v]))

(defn ex-v [c]
  (b/validate c 
    :id [v/number]          
    :features [vec-v]))

(def node-available? 
  "Node availability check, result is cached for one minute"
  (memo-ttl  
    (fn [node] 
      (try+ 
        (prox-get (str "/nodes/" node "/status" ))
        true
        (catch [:status 500] e false))) (* 60 1000)))

(defn task-status [node upid]
  (prox-get (str "/nodes/" node  "/tasks/" upid "/status")))

(defn wait-for [node upid]
  (while (= "running" (:status (task-status node upid)))
    (Thread/sleep 500)
    (debug "Waiting for task" upid "to end")))

(defn check-task
  "Checking that a proxmox task has succeeded"
  [node upid]
  (wait-for node upid)  
  (let [{:keys [exitstatus] :as res} (task-status node upid)]
    (when (not= exitstatus "OK")
      (throw+ (assoc res :type ::task-failed)))))

(defmacro safe 
  "Making sure that the hypervisor exists and that the task succeeded"
  [f]
  `(try+ 
     (use 'proxmox.provider)
     (when-not (node-available? ~'node)
       (throw+ {:type ::missing-node :node ~'node :message "No matching proxmox hypervisor node found"}))
     (check-task ~'node ~f) 
     (catch [:status 500] e# (warn  "container does not exist"))))

(declare vzctl unmount)

(defn enable-features [{:keys [ct extended] :as this}]
  "vzctl set 170 --features \"nfs:on\" --save "
  (when-let [features (extended :features)]
    (doseq [f features] 
      (trace "enabling feature" f)
      (vzctl this (<< "set ~(ct :vmid) --features \"~{f}\" --save")))) () )

(defn key-set [h] (->> h keys (into #{})))

(defconstrainedrecord Container [node ct extended]
  "ct should match proxmox expected input"
  [(validate! (ct-v ct) ::invalid-container) (validate! (ex-v extended) ::invalid-extended) (not (nil? node))]
  Vm
  (create [this] 
          (debug "creating" (:vmid ct))
          (let [ct* (gen-ip ct) {:keys [hostname vmid ip_address]} ct* id (extended :system-id)] 
            (try+ 
              (check-task node (prox-post (str "/nodes/" node "/openvz") ct*)) 
              (enable-features this) 
              (when (p/system-exists? id)
                (p/partial-system id {:proxmox (select-keys ct* [:vmid :ip_address])})) 
              (catch [:status 500] e 
                (release-ip (ct* :ip_address))
                (warn "Container already exists" e)))))

  (delete [this]
          (try 
            (debug "deleting" (:vmid ct))
            (unmount this) 
            (safe
              (prox-delete (str "/nodes/" node "/openvz/" (:vmid ct)))) 
            (finally (release-ip (:ip_address ct)))))

  (start [this]
         (debug "starting" (:vmid ct))
         (safe
           (prox-post (str "/nodes/" node "/openvz/" (:vmid ct) "/status/start"))))

  (stop [this]
        (debug "stopping" (:vmid ct))
        (safe 
          (prox-post (str "/nodes/" node "/openvz/" (:vmid ct) "/status/stop"))))

  (status [this] 
          (try+ 
            (:status 
              (prox-get 
                (str "/nodes/" node "/openvz/" (:vmid ct) "/status/current")))
            (catch [:status 500] e false)))) 

(defn unmount [{:keys [ct node]}]
  (let [{:keys [vmid]} ct]
    (debug "unmounting" vmid) 
    (try+
      (safe 
        (prox-post (str "/nodes/" node "/openvz/" vmid "/status/umount")))
      (catch [:type :proxmox.provider/task-failed] e 
        (debug "no container to unmount")))))

(defn vzctl 
  [this action] 
  (execute  (<< "vzctl ~{action}") (get* :hypervisor :proxmox)))

(defn- key-select [v] (fn [m] (select-keys m (keys v))))

(defn mappings [res]
  "Maps raw model to proxmox model: (mappings {:ip \"1234\" :os \"ubuntu\" :cpu 1})" 
  (let [ms {:ip :ip_address :os :ostemplate}
        vs ((key-select ms) res) ]
    (merge 
      (reduce (fn [r [k v]] (dissoc r k)) res ms)
      (reduce (fn [r [k v]] (assoc r (ms k) v)) {} vs)) 
    ))

(defn os->template 
  "Converts os key to vz template" 
  [os]
  (let [ks [:hypervisor :proxmox :ostemplates os]]
    (try+ 
      (apply get* ks)
      (catch [:type :celestial.common/missing-conf] e
        (throw+ {:type :missing-template :message 
                 (<< "no matching proxmox template found for ~{os} add one to configuration under ~{ks}")})))))

(defn generate
  "apply generated values (if not present)." 
  [res]
  (reduce (fn [res [k v]] (if (res k) res (update-in res [k] v ))) res {:vmid ct-id }))

(defn transform 
  "manipulated the model making it proxmox ready "
  [res]
  (reduce (fn [res [k v]] (update-in res [k] v )) res {:ostemplate os->template}))

(def ct-ks [:vmid :ostemplate :cpus :disk :memory :ip_address :password :hostname :nameserver])

(def ex-ks [:features :node :system-id])

(def selections (juxt (fn [m] (select-keys m ct-ks)) (fn [m] (select-keys m ex-ks))))

(defmethod translate :proxmox [{:keys [machine proxmox system-id]}]
  "Convert the general model into a proxmox vz specific one"
  (-> (merge machine proxmox {:system-id system-id}) mappings transform generate selections))

(defmethod vconstruct :proxmox [{:keys [proxmox] :as spec}]
  (let [{:keys [type node]} proxmox]
    (case type
      :ct  (let [[ct ex] (translate spec)] 
             (->Container node ct ex)))))

