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

(ns celestial.api.actions
  "Actions api"
  (:require 
    [celestial.persistency.actions :as a]
    [celestial.common :refer (import-logging wrap-errors success)]
    [swag.model :refer (defmodel)]
    [swag.core :refer (GET- POST- PUT- DELETE- defroutes-)]))

(defmodel object)

(defmodel capistrano :args {:type "List" :items {"$ref" "String"}})

(defmodel ccontainer :capistrano {:type "Capistrano"} )

(defmodel actions :action-a {:type "Ccontainer"})

(defmodel action :operates-on :string :src :string :actions {:type "Actions"})

(defmodel arguments :args {:type "Hash" :description "key value pairs {'foo':1 , 'bar':2 , ...}" })
 
(defroutes- actions-ro {:path "/actions" :description "Read only actions api"}
  (GET- "/actions/type/:type" [^:string type] {:nickname "getActionsByTargetType" :summary "Gets actions that operate on a target type"}
        (let [ids (a/get-action-index :operates-on type)]
           (success (apply merge {} (map #(hash-map % (a/get-action %)) ids)))))

  (GET- "/actions/:id" [^:int id] {:nickname "getAction" :summary "Get action descriptor"}
        (success (a/get-action id))))

(defroutes- actions {:path "/actions" :description "Adhoc action managment"}

  (POST- "/actions" [& ^:action action] {:nickname "addAction" :summary "Adds an actions set"}
    (wrap-errors (success {:message "added action" :id (a/add-action action)})))

  (PUT- "/actions/:id" [^:int id & ^:action action] {:nickname "updateAction" :summary "Update an actions set"}
        (wrap-errors
          (a/update-action id action)
           (success {:message "updated action" :id id})))


  (DELETE- "/actions/:id" [^:int id] {:nickname "deleteAction" :summary "Deletes an action set"}
           (a/delete-action id)
           (success {:message "Deleted action" :id id})))
