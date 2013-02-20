(ns proxmox.provider
  (:use 
    [trammel.core :only  (defconstrainedrecord)]
    [clojure.core.memoize :only (memo-ttl)]
    [taoensso.timbre :only (debug info error warn)]
    [clojure.core.strint :only (<<)]
    [celestial.core :only (Vm status)]
    [celestial.ssh :only (execute)]
    [celestial.common :only (config)]
    [proxmox.remote :only (prox-post prox-delete prox-get)]
    [slingshot.slingshot :only  [throw+ try+]]
    [mississippi.core :only (required numeric validate)]
    [clojure.set :only (difference)]
    )
  (:import clojure.lang.ExceptionInfo)
  )

(def str? [string? :msg "not a string"])
(def vec? [vector? :msg "not a string"])


(def ct-validations
  {:vmid [(required) (numeric)] 
   :ostemplate [(required)]; string
   :cpus [(numeric)] :disk [(numeric)] :memory [(numeric)]
   :ip_address [(required)]
   :password [(required)]
   :hostname [(required)]
   :hypervisor [str?]
   :features [vec?]
   })

(def create-subset 
  (dissoc ct-validations [:features :hypervisor]))

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

(defn key-set [h] (->> h keys (into #{})))

(defconstrainedrecord Container [node spec]
  "spec should match proxmox expected input"
  [(empty? (:errors (validate spec ct-validations)))
   (empty? (difference (key-set spec) (key-set ct-validations)))]
  Vm
  (create [this] 
          (debug "creating" (:vmid spec))
          (try+ 
            (check-task node (prox-post (str "/nodes/" node "/openvz") 
                                        (apply dissoc spec [:features :hypervisor])))
            ; TODO enable this later
            ;(enable-features this spec)
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
            (catch [:status 500] e "missing-container")))) 

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

