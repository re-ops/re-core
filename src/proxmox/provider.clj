(ns proxmox.provider
  (:use 
    [trammel.core :only  (defconstrainedrecord)]
    [clojure.core.memoize :only (memo-ttl)]
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Vm)]
    [celestial.ssh :only (execute)]
    [celestial.common :only (config import-logging)]
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

(defconstrainedrecord Container [node ct extended]
  "ct should match proxmox expected input"
  [(empty? (:errors (validate ct ct-valid))) (not (nil? node))]
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
            (catch [:status 500] e "missing-container")))) 

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
  (execute (config :hypervisor) [(<< "vzctl ~{action}")]))

(defn- key-select [v] (fn [m] (select-keys m (keys v))))

(defn mappings [res]
 "(mappings {:ip \"1234\" :os \"ubuntu\" :cpu 1})" 
  (let [ms {:ip :ip_address :os :ostemplate}
        vs ((key-select ms) res) ]
     (merge 
       (reduce (fn [r [k v]] (dissoc r k)) res ms)
       (reduce (fn [r [k v]] (assoc r (ms k) v)) {} vs)) 
    ))


(defn transform [res]
  (first (map (fn [[k v]] (update-in res [k] v ))
              {:ostemplate (fn [os] (get-in config [:hypervisor :proxmox :ostemplates os]))})))

(def selections (juxt (key-select ct-valid) (key-select extra-valid)))

(defmethod translate :proxmox [{:keys [machine proxmox]}]
  "Convert the general model into a proxmox vz specific one"
  (-> (merge machine proxmox) mappings transform selections)) 

(defmethod vconstruct :proxmox [{:keys [proxmox] :as spec}]
  (let [{:keys [type node]} proxmox]
    (case type
      :ct  (let [[ct ex] (translate spec)] 
             (->Container node ct ex)))))

