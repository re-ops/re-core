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

(ns openstack.provider
  (:require 
    [slingshot.slingshot :refer  [throw+]]
    [celestial.persistency.systems :as s]
    [celestial.common :refer (import-logging)]
    [openstack.networking :refer (first-ip update-ip assoc-floating dissoc-floating)]
    [clojure.java.data :refer [from-java]]
    [celestial.provider :refer (wait-for wait-for-ssh)]
    [openstack.validations :refer (provider-validation)]
    [celestial.core :refer (Vm)] 
    [trammel.core :refer (defconstrainedrecord)]
    [openstack.volumes :as v]
    [openstack.common :refer (openstack servers compute)]
    [celestial.model :refer (hypervisor translate vconstruct)])
  (:import 
    org.openstack4j.model.compute.Server$Status
    org.openstack4j.model.compute.Action
    org.openstack4j.api.Builders))

(import-logging)

(defn image-id [machine]
  (hypervisor :openstack :ostemplates (machine :os) :id))

(defn flavor-id 
   [f]
  (hypervisor :openstack :flavors f))

(defn network-ids
   [nets]
  (mapv #(hypervisor :openstack :networks %) nets))

(defn floating-ips [tenant] (-> (compute tenant) (.floatingIps)))

(defn model [{:keys [machine openstack] :as spec}]
  (-> (Builders/server) 
    (.name (machine :hostname)) 
    (.flavor (openstack :flavor)) 
    (.image (image-id machine))
    (.keypairName "ronen")
    (.networks (openstack :network-ids))
    (.build)))

(defn wait-for-ip  [servers' id network timeout]
  "Wait for an ip to be avilable"
  (wait-for {:timeout timeout} #(not (nil? (first-ip (.get servers' id) network)))
    {:type ::openstack:status-failed :timeout timeout} 
      "Timed out on waiting for ip to be available"))

(defn running? [this] (= (.status this) "running"))

(defn wait-for-start [this timeout]
  "Wait for an ip to be avilable"
  (wait-for {:timeout timeout} #(running? this)
    {:type ::openstack:start-failed :timeout timeout} 
      "Timed out on waiting for ip to be available"))

(defn wait-for-stop [this timeout]
  "Wait for an ip to be avilable"
  (wait-for {:timeout timeout} #(not (running? this))
    {:type ::openstack:stop-failed :timeout timeout} 
      "Timed out on waiting for ip to be available"))

(defn system-val
  "grabbing instance id of spec"
   [spec ks]
  (get-in (s/get-system (spec :system-id)) ks))

(defn instance-id* [spec] (system-val spec [:openstack :instance-id]))

(defmacro with-instance-id [& body]
 `(if-let [~'instance-id (instance-id* ~'spec)]
    (do ~@body) 
    (throw+ {:type ::openstack:missing-id} "Instance id not found"))) 

(defn update-id [spec id]
  "update instance id"
  (when (s/system-exists? (spec :system-id))
     (s/partial-system (spec :system-id) {:openstack {:instance-id id}})))

(defconstrainedrecord Instance [tenant spec user]
  "An Openstack compute instance"
  [(provider-validation spec)]
  Vm
  (create [this] 
    (let [servers' (servers tenant) server (.boot servers' (model spec)) 
          instance-id (:id (from-java server)) network (first (get-in spec [:openstack :networks]))]
       (update-id spec instance-id)
       (debug "waiting for" instance-id "to get an ip on" network)
       (wait-for-ip servers' instance-id network [5 :minute])
       (let [ip (first-ip (.get servers' instance-id) network)]
         (update-ip spec ip)
         (debug "waiting for ssh to be available at" ip)
         (wait-for-ssh ip (get-in spec [:machine :user]) [5 :minute]))
       (when-let [ip (get-in spec [:openstack :floating-ip])]
         (assoc-floating (floating-ips tenant) server ip))
       (when-let [volumes (get-in spec [:openstack :volumes])]
         (doseq [{:keys [device] :as v} volumes :let [vid (v/create spec v tenant)]]
           (v/attach instance-id vid device tenant)))  
       this))

  (start [this]
     (with-instance-id
       (when-not (running? this)
         (debug "starting" instance-id )
         (.action (servers tenant) instance-id Action/START)
         (wait-for-start this [5 :minute])
         (wait-for-ssh 
           (system-val spec [:machine :ip]) (get-in spec [:machine :user]) [5 :minute]))))

  (delete [this]
     (with-instance-id 
       (debug "deleting" instance-id)
       (when-let [volumes (get-in spec [:openstack :volumes])]
         (doseq [v volumes]
           (v/destroy instance-id v tenant)))
       (let [servers' (servers tenant)]
         (when-let [ip (get-in spec [:openstack :floating-ip])]
           (dissoc-floating (floating-ips tenant) (.get servers' instance-id) ip))
         (.delete servers' instance-id))))

  (stop [this]
     (with-instance-id 
       (debug "stopping" instance-id)
       (.action (servers tenant) instance-id Action/STOP)
       (wait-for-stop this [5 :minute])))

  (status [this] 
     (if-let [instance-id (instance-id* spec)]
       (let [server (.get (servers tenant) instance-id)
             value (.toLowerCase (str (.getStatus server)))]
          (if (= value "active") "running" value))
       (do (debug "no instance id found, instance not created") false))))

(defn openstack-spec 
   [spec]
   (-> spec 
     (assoc-in [:openstack :network-ids] 
        (network-ids (get-in spec [:openstack :networks])))
     (update-in [:openstack :flavor] flavor-id)))

(defmethod translate :openstack [{:keys [openstack machine] :as spec}] 
  [(openstack :tenant) (openstack-spec spec) (or (machine :user) "root")])

(defmethod vconstruct :openstack [spec]
  (apply ->Instance (translate spec)))



