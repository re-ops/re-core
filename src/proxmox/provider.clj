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
    [proxmox.remote :only (prox-post prox-delete prox-get)]
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.set :only (difference)]
    [celestial.provider :only (str? vec? mappings transform os->template wait-for)]
    [proxmox.generators :only (ct-id)]
    [celestial.model :only (translate vconstruct)])
  (:require 
    [celestial.common :refer (import-logging)]
    [proxmox.model :refer (get-node)]
    [proxmox.validations :refer (validate-provider)]
    [me.raynes.fs :refer (delete-dir temp-dir)]
    [supernal.sshj :refer (copy)]
    [hypervisors.networking :refer (debian-interfaces gen-ip release-ip mark)]
    [celestial.persistency :as p])
  (:import clojure.lang.ExceptionInfo))

(import-logging)

(def node-available? 
  "Node availability check, result is cached for one minute"
  (memo-ttl  
    (fn [node] 
      (try+ 
        (prox-get (<< "/nodes/~{node}/status" ))
        true
        (catch [:status 500] e false))) (* 60 1000)))

(defn task-status [node upid]
  (prox-get (<< "/nodes/~{node}/tasks/~{upid}/status")))

(defn check-task
  "Checking that a proxmox task has succeeded"
  [node upid]
  (wait-for {:timeout [5 :minute]} 
      (fn [] 
        (debug "Waiting for task" upid "to end")
        (not= "running" (:status (task-status node upid))))      
     {:type ::proxmo:task-timeout :message "Timed out while waiting for proxmox task" :upid upid :node node})
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

(defn- append-ip [[ct network]]
  (into [ct network] [(or (ct :ip_address) (network :ip_address))]))

(defn assign-networking
  "Generate ip only if missing and not bridged"
  [ct {:keys [ip_address netif] :as network}]
  (append-ip
    (cond 
      (and ip_address netif) [(assoc ct :netif netif) (-> network (assoc :ip_address (mark ip_address "proxmox")) (dissoc :netif))]
      (and (not ip_address) netif) [(assoc ct :netif netif) (-> network (gen-ip "proxmox") (dissoc :netif))]
      (and ip_address (not netif)) [(assoc ct :ip_address (mark ip_address "proxmox")) network]
      (and (not ip_address) (not netif)) [(gen-ip ct "proxmox") network]
      )))

(defn- redhat-network
  "updates redhat bridged networking"
  [node ip_address {:keys [gateway] :as network} vmid]
  (let [ifcfg (<< "~(.getPath temp)/ifcfg-eth0") (<< "~(.getPath temp)/network")]
    (spit ifcfg (redhat-ifcfg-eth0 (merge {:ip ip_address} network)))
    (copy ifcfg (<< "/var/lib/vz/private/~{vmid}/etc/sysconfig/network-scripts/") (get-node node))))

(defn- debian-network
  "debian bridged networking update"
  [node ip_address network vmid]
  (let [temp (temp-dir "update-interfaces") output (<< "~(.getPath temp)/interfaces")]
    (try 
      (spit output (debian-interfaces (merge {:ip ip_address} network)))      
      (copy output (<< "/var/lib/vz/private/~{vmid}/etc/network/") (get-node node)) 
      (finally (delete-dir temp))))
  )

(defn update-interfaces 
  "uploads interfaces file for a ct if bridge is used" 
  [node flavor ip_address network vmid]
  (case flavor 
    :redhat (redhat-network node ip_address network vmid)
    :debian (debian-network node ip_address network vmid)
    (throw+ {:type ::missing-flavor :flavor flavor :message "No matching proxmox template flavor found"})
    )
  )

(defconstrainedrecord Container [node ct extended network]
  "ct should match proxmox expected input (see http://pve.proxmox.com/pve2-api-doc/)"
  [(validate-provider ct extended network) (not (nil? node))]
  Vm
  (create [this] 
          (debug "creating" (:vmid ct))
          (let [[ct* network* ip_address] (assign-networking ct network) {:keys [hostname vmid]} ct* id (extended :system-id)] 
            (try+ 
              (check-task node (prox-post (str "/nodes/" node "/openvz") ct*)) 
              (enable-features this) 
              (when (p/system-exists? id)
                (p/partial-system id {:proxmox {:vmid vmid} :machine {:ip ip_address}})) 
              (when (ct* :netif)
                (update-interfaces node ip_address network* vmid))
              (catch [:status 500] e 
                (release-ip ip_address "proxmox")
                (warn "Container already exists" e)))))

  (delete [this]
          (try 
            (debug "deleting" (:vmid ct))
            (unmount this) 
            (safe
              (prox-delete (str "/nodes/" node "/openvz/" (:vmid ct)))) 
            (finally 
              (when-let [id (extended :system-id)] 
                (release-ip (get-in (p/get-system id) [:machine :ip]) "proxmox")))))

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
  (execute  (<< "vzctl ~{action}") (get-node (:node this))))

(defn generate
  "apply generated values (if not present)." 
  [res]
  (reduce (fn [res [k v]] (if (res k) res (update-in res [k] v ))) res {:vmid (ct-id (:node res))}))

(def ct-ks [:vmid :ostemplate :cpus :disk :memory :password :hostname :nameserver :searchdomain])

(def net-ks [:gateway :netmask :ip_address :netif])

(def ex-ks [:features :node :system-id])

(def selections 
  (letfn [(select [k] (fn [m] (select-keys m k)))]
    (apply juxt (map select [ct-ks ex-ks net-ks]))))

(defn transformations [{:keys [bridge interface domain]}]
  (let [base {:ostemplate (os->template :proxmox) :hostname (fn [host] (<< "~{host}.~{domain}"))}]
    (if (and bridge interface) (assoc base :netif (fn [_] (<< "ifname=~{interface},bridge=~{bridge}"))) base)))

(defmethod translate :proxmox [{:keys [machine proxmox system-id] :as spec}]
  "Convert the general model into a proxmox vz specific one"
  (-> (merge machine proxmox {:system-id system-id})
      (mappings {:ip :ip_address :os :ostemplate :domain :searchdomain})
      (transform (transformations machine))
      generate selections))

(defmethod vconstruct :proxmox [{:keys [proxmox] :as spec}]
  (let [{:keys [type node]} proxmox]
    (case type
      :ct  (apply ->Container node (translate spec))
      :vm nil 
      )))


