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

(ns celestial.api.types
  "Celestial types api"
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.persistency :as p]
    [celestial.model :refer (sanitized-envs)]
    [clojure.core.strint :refer (<<)]
    [swag.model :refer (defmodel)]
    [celestial.common :refer (import-logging success wrap-errors conflict bad-req)]
    [swag.core :refer (GET- POST- PUT- DELETE- defroutes- errors)]))

(import-logging)

(defmodel type :type :string :puppet-std {:type "Puppetstd"} :classes {:type "Object"})

(defmodel puppetstd :module {:type "Module"} :args {:type "List"})

(defmodel module :name :string :src :string)

(defroutes- types {:path "/type" :description "Operations on types"}

  (GET- "/types" [] {:nickname "getTypes" :summary "Get all types"}
        (success {:types (map p/get-type (p/all-types))}))

  (GET- "/types/:type" [^:string type] {:nickname "getType" :summary "Get type"}
        (success (p/get-type type)))

  (POST- "/types" [& ^:type props] {:nickname "addType" :summary "Add type"}
         (wrap-errors 
           (p/add-type props)
           (success {:msg "new type saved"})))

  (PUT- "/types" [& ^:type props] {:nickname "updateType" :summary "Update type"}
        (wrap-errors
          (if-not (p/type-exists? (props :type))
            (conflict {:msg "Type does not exists, use POST /type first"}) 
            (do (p/update-type props) 
                (success {:msg "type updated"})))))

  (DELETE- "/types/:type" [^:string type] {:nickname "deleteType" :summary "Delete type" 
                                           :errorResponses (errors {:bad-req "Type does not exist"})}
           (if (p/type-exists? type)
             (do (p/delete-type type) 
                 (success {:msg "Type deleted"}))
             (bad-req {:msg "Type does not exist"}))))
 
