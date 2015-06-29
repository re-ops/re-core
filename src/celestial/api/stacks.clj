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

(ns celestial.api.stacks
  "Stacks api"
   (:require 
    [taoensso.timbre :as timbre]
    [celestial.persistency.stacks :as s]
    [celestial.common :refer (import-logging wrap-errors success)]
    [swag.model :refer (defmodel)]
    [swag.core :refer (GET- POST- PUT- DELETE- defroutes-)])
 )

(timbre/refer-timbre)

(defmodel stack
  :shared {
    :type "Hash" :description "Shared peroperties include: :owner :env :machine :openstack/:aws etc"
  }
  :systems {
    :type "List" :description "count per template {:count 1 :template :foo}"
  }
)
