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
    [openstack.validations :refer (provider-validation)]
    [celestial.core :refer (Vm)] 
    [trammel.core :refer (defconstrainedrecord)]
    [celestial.model :refer (translate vconstruct)]
    [celestial.model :refer (hypervisor)])
  (:import 
    org.openstack4j.api.Builders
    org.openstack4j.openstack.OSFactory))

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

(defn server [{:keys [machine openstack] :as spec}]
  (println (openstack :flavor))
  (-> (Builders/server) 
    (.name (machine :hostname)) 
    (.flavor (openstack :flavor)) 
    (.image (image-id machine))
    (.keypairName (openstack :keypair))
    (.networks (openstack :networks))
    (.build))
  )

(defconstrainedrecord Instance [tenant spec user]
  "An Openstack compute instance"
  [(provider-validation spec)]
  Vm
  (create [this] 
    (let [compute (servers tenant) model (server spec)]
      (.boot compute model)))

  (start [this])

  (delete [this])

  (stop [this])

  (status [this] ))

(defn openstack-spec 
   [spec]
   (-> spec 
     (update-in [:openstack :networks] network-ids)
     (update-in [:openstack :flavor] flavor-id)))

(defmethod translate :openstack [{:keys [openstack machine] :as spec}] 
  [(openstack :tenant) (openstack-spec spec) (or (machine :user) "root")])

(defmethod vconstruct :openstack [spec]
  (apply ->Instance (translate spec)))

(comment 
  (use 'celestial.fixtures.data 'celestial.fixtures.core )
  (def m (vconstruct redis-openstack)) 
  (clojure.pprint/pprint m)
  (with-conf local-conf (with-admin (.create m)))
  (with-admin (with-conf local-conf (.status m)))
  (.start m)
  )

