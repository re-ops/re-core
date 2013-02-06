(ns com.narkisr.proxmox.provider
  (:require 
    [closchema.core :as schema])
  (:use 
    [taoensso.timbre :only (debug info error)]
    clojure.core.strint
    [com.narkisr.celestial.core]
    [com.narkisr.proxmox.remote :only (prox-post prox-delete prox-get)]
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

(defn task-status [upid]
  (prox-get (str "/nodes/proxmox/tasks/" upid "/status")))

(defn wait-for [upid]
  (while (= "running" (:status (task-status upid)))
    (Thread/sleep 200)
    (debug "Waiting for task" upid "to end")) )

(defn check-task
  "Checking that a proxmox task has succeeded"
  [upid]
  (wait-for upid)  
  (let [{:keys [exitstatus] :as res} (task-status upid)]
    (when (not= exitstatus "OK")
      (throw (ExceptionInfo. "Task failed" res)))))

(defn use-ns []
  (use 'com.narkisr.proxmox.remote);TODO this is ugly http://tinyurl.com/avotufx fix with macro
  (use 'com.narkisr.proxmox.provider))

(defmacro safe [f]
  `(try+ (check-task ~f)
         (catch [:status 500] e# (warn  "Container does not exist"))))

(deftype Container [node spec]
  Vm
  (create [this] 
    (use-ns)
    (let [errors (validate spec)]
      (when-not (empty? errors) 
        (throw (ExceptionInfo. "Failed to validate spec" errors)))
      (try+ 
        (check-task (prox-post (str "/nodes/" node "/openvz") spec))
        (catch [:status 500] e 
          (throw (ExceptionInfo. "Machine already exists" e)))
        )
      ))

  (delete [this]
    (use-ns)
    (safe
      (prox-delete (str "/nodes/" node "/openvz/" (:vmid spec)))))

  (start [this]
    (use-ns)
    (safe
      (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/start"))))

  (stop [this]
    (use-ns)
    (safe 
      (prox-post (str "/nodes/" node "/openvz/" (:vmid spec) "/status/stop"))))

  (status [this] 
    (use-ns)
    (try+ 
      (:status 
        (prox-get 
          (str "/nodes/" node "/openvz/" (:vmid spec) "/status/current")))
      (catch [:status 500] e "missing")))) 

