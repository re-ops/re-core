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

(ns celestial.persistency.actions
  "Actions persistency"
  (:refer-clojure :exclude  [name type])
  (:require
    [puny.migrations :refer (Migration register)]
    [clojure.string :refer (join escape)]
    [celestial.persistency.common :as c]
    [celestial.model :refer (figure-rem)]
    [slingshot.slingshot :refer  [throw+]]
    [subs.core :as subs :refer (validate! validation every-kv combine)]
    [clojure.core.strint :refer (<<)]
    [puny.core :refer (entity)]))

(declare unique-name with-provided)

(entity action :indices [operates-on] :intercept {:create [unique-name with-provided] :update [with-provided]})

(defn remoter [action]
  (get action (figure-rem action)))

(defn add-provided [action]
  "appends action expected arguments derived from args strings"
   (if (:args (remoter action))
     action ; not migrated yet
     (reduce 
      (fn [m e] 
        (assoc-in m [(figure-rem m) e :provided] 
          (remove #{"target" "hostname"} 
            (c/args-of (join " " ((remoter m) e :args)))))) action (keys (remoter action))))) 

(def with-provided (partial c/with-transform add-provided))

(defn find-action-for [name type]
  (let [ids (get-action-index :operates-on type) 
        actions (map #(-> % Long/parseLong  get-action) ids)]
    (first (filter #(= (-> % :name) name) actions))))

(defn unique-name [f & [{:keys [name operates-on]} & r :as args]]
  (when (and name operates-on (find-action-for name operates-on))
    (throw+ {:type ::duplicated-action } (<< "action for ~{operates-on} named ~{name} already exists")))
  (apply f args))

(validation :git-based*
  (every-kv {
    :args #{:required :Vector}
    :timeout #{:required :Integer}
   }))

(def action-validation
  {:operates-on #{:required :String :type-exists}
   :name #{:required :String} :src #{:required :String}})

(defn validate-action [action]
   (let [remoter-validation {(figure-rem action) #{:required :git-based*}}]
     (validate! action (combine action-validation remoter-validation) :error ::invalid-action)))

(defrecord Timeout [identifier]
  Migration
  (apply- [this]
    (doseq [id (all-actions)]  
      (when-not ((get-action id) :timeout)
        (update-action id (assoc (get-action id) :timeout (* 1000 60 15))))))  
  (rollback [this]))

(defn register-migrations []
  (register :actions (Timeout. :default-timeout)))


