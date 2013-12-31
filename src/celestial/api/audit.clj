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

(ns celestial.api.audit
  "Auditing api" 
  (:require 
    [celestial.persistency.audit :as a]
    [celestial.common :refer (import-logging wrap-errors success)]
    [swag.model :refer (defmodel)]
    [swag.core :refer (GET- POST- PUT- DELETE- defroutes-)]))

(defmodel audit :name :string :query :string :type :string :type :string
                 :args {:type "List" :items {"$ref" "String"}})

(defroutes- audits-ro {:path "/audits" :description "Read only audits api"}
  (GET- "/audits/:id" [^:string name] {:nickname "getAudit" :summary "Get audit by name"}
        (success (a/get-audit id))))
