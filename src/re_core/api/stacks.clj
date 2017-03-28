(comment 
   re-core, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns re-core.api.stacks
  "Stacks api"
   (:require 
    [slingshot.slingshot :refer  [throw+ try+]] 
    [re-core.persistency.stacks :as s]
    [re-core.common :refer (success wrap-errors conflict bad-req)]
    [taoensso.timbre :as timbre]
    [swag.model :refer (defmodel)]
    [swag.core :refer (GET- POST- PUT- DELETE- defroutes- errors)])
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

(defroutes- stacks {:path "/stacks" :description "Operations on stacks"}
  (POST- "/stacks" [& ^:stack stack] {:nickname "addStack" :summary "Add stack"}
    (wrap-errors (s/add-stack stack) (success {:message "new stack saved"})))

  (DELETE- "/stacks/:id" [^:int id] {:nickname "deleteStack" :summary "Delete Stack" 
                                          :errorResponses (errors {:bad-req "Stack does not exist"})}
         (try+ 
             (let [spec (s/get-stack! id) int-id (Integer/valueOf id)]               
               (s/delete-stack! int-id) 
               (success {:message "Stack deleted"})) 
             (catch [:type :re-core.persistency/missing-stack] e 
               (bad-req {:message "Stack does not exist"}))))

  (PUT- "/stacks/:id" [^:int id & ^:stack stack] {:nickname "updateStack" :summary "Update stack"}
     (if-not (s/stack-exists? id)
          (conflict {:message "Stack does not exists, use POST /stack first"}) 
          (wrap-errors
            (s/update-stack (Integer/valueOf id) stack) 
            (success {:message "stack updated" :id id})))))

(defroutes- stacks-ro {:path "/stacks" :description "Read only stacks api"}
  (GET- "/stacks/:id" [^:int id] {:nickname "getStackById" :summary "Get stack by its id"}
     (success (s/get-stack id))))
