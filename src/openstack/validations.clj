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

(ns openstack.validations
  "Openstack validations"
  (:require 
    [openstack.model :refer (identifiers)]
    [celestial.model :refer (check-validity)]
    [clojure.core.strint :refer (<<)]
    [subs.core :as subs :refer (validate! combine every-v every-kv validation when-not-nil subtract)])
 )

(def machine-entity
  {:machine {
     :hostname #{:required :String} :domain #{:required :String} 
     :user #{:required :String} :os #{:required :Keyword} 
  }})

(validation ::volume {
    :device #{:required :device} 
    :size #{:required :Integer}
    :clear #{:required :Boolean}})

(validation :device 
  #(when-not (re-find (re-matcher #"\/dev\/\w+" %)) "device should match /dev/{id} format"))

(validation ::group* (every-v #{:String}))

(validation ::network* (every-v #{:String}))

(validation ::volume* (every-v #{::volume}))

(validation ::hint* (every-v #{:Vector}))

(def openstack-common
  {:openstack
   {:flavor #{:required :String} :tenant #{:required :String} 
    :security-groups #{:Vector ::group*} :networks #{:Vector ::network*}
    :key-name #{:required :String} :floating-ip #{:ip :String}
    :hints #{::hint*} :volumes #{::volume*}}
   })

(defn provider-validation [spec]
  (validate! spec (combine machine-entity openstack-common) :error ::invalid-openstack))

(defmethod check-validity [:openstack :entity] [m]
  (validate! m (combine machine-entity openstack-common) :error ::invalid-system))

(defmethod check-validity [:openstack :template] [m]
  (validate! m (subtract (combine machine-entity openstack-common) identifiers) :error ::invalid-template))
