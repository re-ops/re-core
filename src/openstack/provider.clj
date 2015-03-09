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
    [celestial.persistency.systems :as s]
    [celestial.common :refer (import-logging)]
    [openstack.networking :refer (first-ip update-ip)]
    [clojure.java.data :refer [from-java]]
    [celestial.provider :refer (wait-for wait-for-ssh)]
    [openstack.validations :refer (provider-validation)]
    [celestial.core :refer (Vm)] 
    [trammel.core :refer (defconstrainedrecord)]
    [celestial.model :refer (translate vconstruct)]
    [celestial.model :refer (hypervisor)])
  (:import 
    org.openstack4j.model.compute.Server$Status
    org.openstack4j.model.compute.Action
    org.openstack4j.api.Builders
    org.openstack4j.openstack.OSFactory))

(import-logging)

(defn image-id [machine]
  (hypervisor :openstack :ostemplates (machine :os) :id))

(defn flavor-id 
   [f]
  (hypervisor :openstack :flavors f))

(defn network-ids
   [nets]
  (mapv #(hypervisor :openstack :networks %) nets))

(defn servers [tenant]
  (let [{:keys [username password endpoint]} (hypervisor :openstack)]
    (-> (OSFactory/builder) 
        (.endpoint endpoint)
        (.credentials username password)
        (.tenantName tenant)
        (.authenticate)
        (.compute)
        (.servers))))

(defn model [{:keys [machine openstack] :as spec}]
  (-> (Builders/server) 
    (.name (machine :hostname)) 
    (.flavor (openstack :flavor)) 
    (.image (image-id machine))
    (.keypairName "ronen")
    (.networks (openstack :network-ids))
    (.build)))

(defn wait-for-ip  [compute id network timeout]
  "Wait for an ip to be avilable"
  (wait-for {:timeout timeout} #(not (nil? (first-ip (.get compute id) network)))
    {:type ::openstack:status-failed :timeout timeout} 
      "Timed out on waiting for ip to be available"))

(defn system-val
  "grabbing instance id of spec"
   [spec ks]
  (get-in (s/get-system (spec :system-id)) ks))

(defn instance-id [spec] (system-val spec [:openstack :instance-id]))

(defn update-id [spec id]
  "update instance id"
  (when (s/system-exists? (spec :system-id))
     (s/partial-system (spec :system-id) {:openstack {:instance-id id}})))

(defconstrainedrecord Instance [tenant spec user]
  "An Openstack compute instance"
  [(provider-validation spec)]
  Vm
  (create [this] 
    (let [compute (servers tenant)  id (:id (from-java (.boot compute (model spec)))) 
          network (first (get-in spec [:openstack :networks]))]
       (update-id spec id)
       (debug "waiting for" id "to get an ip on" network)
       (wait-for-ip compute id network [5 :minute])
       (let [ip (first-ip (.get compute id) network)]
         (update-ip spec ip)
         (debug "waiting for ssh to be available at" ip)
         (wait-for-ssh ip (get-in spec [:machine :user]) [5 :minute])))
       this)

  (start [this]
     (debug "starting" (instance-id spec))
     (.action (servers tenant) (instance-id spec) Action/START)
     (wait-for-ssh 
       (system-val spec [:machine :ip]) (get-in spec [:machine :user]) [5 :minute]))

  (delete [this]
     (debug "deleting" (instance-id spec))
     (.delete (servers tenant) (instance-id spec)))

  (stop [this]
     (debug "stopping" (instance-id spec))
     (.action (servers tenant) (instance-id spec) Action/STOP))

  (status [this] 
     (if-let [instance-id (instance-id spec)]
       (let [server (.get (servers tenant) instance-id)
             value (.toLowerCase (str (.getStatus server)))]
         (if (= value "active") "running" value))
       (do (debug "no instance id found, instance not created") false) 
       )))

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

(comment 
  (use 'celestial.fixtures.data 'celestial.fixtures.core )
  (def m (vconstruct redis-openstack)) 
  (with-conf local-conf (with-admin (.create m)))
  (with-admin (with-conf local-conf (.status m)))
  (clojure.pprint/pprint 
    (ip (from-java (.get (servers "skywind") "9a7e1ceb-cccd-44b9-b1bc-148a39e913c4")) "playtech-int-2"))
  (.start m)
  )

