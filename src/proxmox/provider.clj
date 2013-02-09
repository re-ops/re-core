(ns proxmox.provider
  (:require 
    [closchema.core :as schema])
  (:use 
    [taoensso.timbre :only (debug info error warn)]
    clojure.core.strint
    celestial.core
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
  (schema/report-errors (schema/validate ct-schema spec)))

(defn task-status [node upid]
  (prox-get (str "/nodes/" node  "/tasks/" upid "/status")))

(defn wait-for [node upid]
  (while (= "running" (:status (task-status node upid)))
    (Thread/sleep 200)
    (debug "Waiting for task" upid "to end")) )

(defn check-task
  "Checking that a proxmox task has succeeded"
  [node upid]
  (wait-for node upid)  
  (let [{:keys [exitstatus] :as res} (task-status node upid)]
    (when (not= exitstatus "OK")
      (throw+ (assoc res :type ::task-failed)))))

(defn use-ns []
  (use 'proxmox.remote);TODO this is ugly http://tinyurl.com/avotufx fix with macro
  (use 'proxmox.provider))

(defmacro safe [f]
  `(try+ 
       (check-task ~'node ~f) 
         (catch [:status 500] e# (warn  "Container does not exist"))))

(defprotocol Openvz (unmount [this]))

(deftype Container [node spec]
  Vm
  (create [this] 
    (use-ns)
    (debug "creating" (:vmid spec))
    (let [errors (validate spec)]
      (when-not (empty? errors) 
        (throw (ExceptionInfo. "Failed to validate spec" errors)))
      (try+ 
        (check-task node (prox-post (str "/nodes/" node "/openvz") spec))
        (catch [:status 500] e 
          (warn "Container already exists" e))
        )))

  (delete [this]
    (use-ns)
    (debug "deleting" (:vmid spec))
    (.unmount this)
    (safe
      (prox-delete (str "/nodes/" node "/openvz/" (:vmid spec)))))

  (start [this]
    (use-ns)
    (debug "starting" (:vmid spec))
    (safe
      (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/start"))))

  (stop [this]
    (use-ns)
    (debug "stopping" (:vmid spec))
    (safe 
      (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/stop"))))

  (status [this] 
    (use-ns)
    (try+ 
      (:status 
        (prox-get 
          (str "/nodes/" node "/openvz/" (:vmid spec) "/status/current")))
      (catch [:status 500] e "missing-container")))
  
  Openvz
  (unmount [this]
    (use-ns)
    (debug "unmounting" (:vmid spec))
    (try+
      (safe 
        (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/umount")))
      (catch [:type :proxmox.provider/task-failed] e 
        (debug "no container to unmount")))) 

  ) 

