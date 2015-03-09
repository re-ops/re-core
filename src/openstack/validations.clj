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
    [clojure.core.strint :refer (<<)]
    [subs.core :as subs :refer (validate! combine every-v every-kv validation when-not-nil)])
 )

(def machine-entity
  {:machine {
     :hostname #{:required :String} :domain #{:required :String} 
     :user #{:required :String} :os #{:required :Keyword} 
  }})

(validation :group* (every-v #{:String}))
(validation :network* (every-v #{:String}))

(def openstack-common
  {:openstack
   {:flavor #{:required :String} :tenant #{:required :String} 
    :security-groups #{:Vector :group*} :networks #{:Vector :network*}}}
  )

(defn validate-entity 
  "openstack based systems entity validation " 
  [openstack]
  (validate! openstack (combine machine-entity openstack-common) :error ::invalid-system))

(defn provider-validation [spec]
  (validate! spec (combine machine-entity openstack-common) :error ::invalid-openstack))
