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

(ns celestial.api.audits
  "Auditing api" 
  (:refer-clojure :exclude  [name type])
  (:require 
    [clojure.data.json :as json]
    [celestial.persistency.audits :as a]
    [celestial.common :refer (import-logging success wrap-errors conflict bad-req link)]
    [swag.model :refer (defmodel defc)]
    [ring.util.codec :refer (base64-decode)]
    [swag.core :refer (GET- POST- PUT- DELETE- defroutes- errors)]))

(defc "/audits" [:type] (keyword v))

(defmodel audit :name :string :query :string :type :string :type :string
                 :args {:type "List" :items {"$ref" "String"}})

(defmodel link-input :name :string :args {:type "Hash" :description "args to value"})

(defroutes- audits-ro {:path "/audits" :description "Read only audits api"}
  (GET- "/audits" [] {:nickname "getAudits" :summary "Get all audits"}
        (success {:audits (map a/get-audit (a/all-audits))}))

  (GET- "/audits/:name" [^:string name] {:nickname "getAudit" :summary "Get audit by name"}
        (success (a/get-audit name)))
 
  (GET- "/audits/:name/:args" [^:string name ^:string args] {:nickname "linkFor" :summary "Get audit link"}
    (let [{:keys [query]} (a/get-audit name)]
       (success {:link (link query (json/read-str (String. (base64-decode args)) :key-fn keyword))})))) 

(defroutes- audits {:path "/audit" :description "Operations on audits"}
  (POST- "/audits" [& ^:audit audit] {:nickname "addAudit" :summary "Add audit"}
     (wrap-errors 
       (a/add-audit audit)
       (success {:message "new audit saved"})))

  (PUT- "/audits" [& ^:audit audit] {:nickname "updateAudit" :summary "Update audit"}
        (wrap-errors
          (if-not (a/audit-exists? (audit :name))
            (conflict {:message "Audit does not exists, use POST /audit first"}) 
            (do (a/update-audit audit) 
                (success {:message "Audit updated"})))))

  (DELETE- "/audits/:audit" [^:string audit] {:nickname "deleteAudit" :summary "Delete audit" 
                                           :errorResponses (errors {:bad-req "audit does not exist"})}
           (if (a/audit-exists? audit)
             (do (a/delete-audit audit) 
                 (success {:message "Audit deleted"}))
             (bad-req {:message "Audit does not exist"}))))
