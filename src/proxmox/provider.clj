(ns proxmox.provider
  (:require 
    [closchema.core :as schema])
  (:use 
    [clojure.core.memoize :only (memo-ttl)]
    [taoensso.timbre :only (debug info error warn)]
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Vm status)]
    [celestial.ssh :only (execute)]
    [celestial.common :only (config)]
    [proxmox.remote :only (prox-post prox-delete prox-get)]
    [slingshot.slingshot :only  [throw+ try+]]
    )
  (:import clojure.lang.ExceptionInfo)
  )

(def ct-schema
  {:type "object"
   :properties  {
     :vmid  {:description  "vm unique id number" :type  "integer" :required  true}
     :ostemplate  {:description  "template used " :type  "string" :required  true}
     :cpus  {:description "number of cpus" :type  "integer" :required false} 
     :disk  {:description "disk size" :type  "integer" :required false}
     :memory  {:description "RAM size" :type  "integer" :required false} 
     :ip_address  {:description "the container static ip" :type  "string" :required false}
     :password  {:description "machines password" :type  "string" :required false}
     :hostname  {:description "container hostname" :type  "string" :required false}
    }})

(defn validate [spec]
  (let [errors (schema/report-errors (schema/validate ct-schema spec))]
    (when-not (empty? errors) 
      (throw (ExceptionInfo. "Failed to validate spec" errors)))))

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
    (Thread/sleep 200)
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
     (when-not (node-available? ~'node)
       (throw+ {:type ::missing-node :node ~'node :message "No matching proxmox hypervisor node found"}))
     (check-task ~'node ~f) 
     (catch [:status 500] e# (warn  "container does not exist"))))



(declare vzctl unmount)

(defn enable-features [this {:keys [vmid] :as spec}]
  (when-let [features (:features spec)] 
    ;vzctl set 170 --features "nfs:on" --save 
    (doseq [f features] 
      (vzctl this (<< "set ~{vmid} --features \"~{f}\" --save")))) )


(defrecord Container [node spec]
  Vm
  (create [this] 
    (debug "creating" (:vmid spec))
    (validate spec)
    (try+ 
      (check-task node (prox-post (str "/nodes/" node "/openvz") 
                            (apply dissoc spec [:features :hypervisor])))
      (enable-features this spec)
      (catch [:status 500] e 
        (warn "Container already exists" e))))

  (delete [this]
    (debug "deleting" (:vmid spec))
    (unmount this)
    (safe
      (prox-delete (str "/nodes/" node "/openvz/" (:vmid spec)))))

  (start [this]
    (debug "starting" (:vmid spec))
    (safe
      (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/start"))))

  (stop [this]
    (debug "stopping" (:vmid spec))
    (safe 
      (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/stop"))))

  (status [this] 
    (try+ 
      (:status 
        (prox-get 
          (str "/nodes/" node "/openvz/" (:vmid spec) "/status/current")))
      (catch [:status 500] e "missing-container")))
  ) 

(defn unmount [{:keys [spec node]}]
  (let [{:keys [vmid]} spec]
    (debug "unmounting" vmid) 
    (try+
      (safe 
        (prox-post (str "/nodes/" node "/openvz/" vmid "/status/umount")))
      (catch [:type :proxmox.provider/task-failed] e 
        (debug "no container to unmount")))))

(defn vzctl [this action] 
    {:pre [(= (status this) "running")]}
    (execute (config :hypervisor) [(<< "vzctl ~{action}")]))
 
