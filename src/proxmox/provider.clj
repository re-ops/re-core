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
    [trammel.core :only  (defconstrainedrecord)]
    [clojure.core.memoize :only (memo-ttl)]
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Vm)]
    [supernal.sshj :only (execute)]
    [celestial.common :only (get* import-logging)]
    [proxmox.remote :only (prox-post prox-delete prox-get)]
    [slingshot.slingshot :only  [throw+ try+]]
    [mississippi.core :only (required numeric validate)]
    [clojure.set :only (difference)]
    [celestial.provider :only (str? vec?)]
    [celestial.model :only (translate vconstruct)])
  (:import clojure.lang.ExceptionInfo))

(import-logging)

(def ct-valid
  {
   :vmid [(required) (numeric)] 
   :ostemplate [str? (required)]
   :cpus [(numeric)] :disk [(numeric)] :memory [(numeric)]
   :ip_address [str? (required)]
   :password [str? (required)]
   :hostname [str? (required)]
   :nameserver [str?]  
   }
  )

(def extra-valid
  {
   :hypervisor [str? (required)]
   :features [vec?]
   :host [str?]
   :node [str?]
   } 
  )



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

(defn validate-debug 
  [ct]
  (let [es (:errors (validate ct ct-valid))]
    (debug es)
    (empty? es)))

(defconstrainedrecord Container [node ct extended]
  "ct should match proxmox expected input"
  [(validate-debug ct) (not (nil? node))]
  Vm
  (create [this] 
          (debug "creating" (:vmid ct))
          (try+ 
            (check-task node (prox-post (str "/nodes/" node "/openvz") ct)) 
            (enable-features this)
            (catch [:status 500] e 
              (warn "Container already exists" e))))

  (delete [this]
          (debug "deleting" (:vmid ct))
          (unmount this)
          (safe
            (prox-delete (str "/nodes/" node "/openvz/" (:vmid ct)))))

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
  "(mappings {:ip \"1234\" :os \"ubuntu\" :cpu 1})" 
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
          (<< "no matching proxmox template found for ~{os}, add one to configuration under ~{ks} ")}) 
        ))))

(defn transform 
  "manipulated the model making it proxmox ready "
  [res]
  (first (map (fn [[k v]] (update-in res [k] v )) {:ostemplate os->template})))

(def selections (juxt (key-select ct-valid) (key-select extra-valid)))

(defmethod translate :proxmox [{:keys [machine proxmox]}]
  "Convert the general model into a proxmox vz specific one"
  (-> (merge machine proxmox) mappings transform selections)) 

(defmethod vconstruct :proxmox [{:keys [proxmox] :as spec}]
  (let [{:keys [type node]} proxmox]
    (case type
      :ct  (let [[ct ex] (translate spec)] 
             (->Container node ct ex)))))

