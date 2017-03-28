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

(ns re-core.persistency.audits
  "Audits persistency"
  (:refer-clojure :exclude  [name type])
  (:require
    [puny.migrations :refer (Migration register)]
    [clojure.string :refer (join escape)]
    [re-core.model :refer (figure-rem)]
    [re-core.persistency.common :as c]
    [slingshot.slingshot :refer  [throw+]]
    [subs.core :as subs :refer (validate! validation when-not-nil)]
    [clojure.core.strint :refer (<<)]
    [puny.core :refer (entity)]))

(declare with-args)

(entity audit :id name :intercept  {:create [with-args] :update [with-args]})

(defn add-args [{:keys [query] :as audit}]
  "appends audit expected arguments derived from args strings"
   (assoc audit :args (into [] (c/args-of query))))

(def with-args (partial c/with-transform add-args))
 
(def audit-types #{:kibana3 :kibana4})

(validation :audit-type
  (when-not-nil audit-types (<< "Audit type must be either ~{audit-types}")))

(def audit-validation
  {:name #{:required :String} :query #{:required :String} 
   :args #{:Vector} :type #{:required :audit-type}
   })

(defn validate-audit [{:keys [name type] :as audit}]
  (validate! audit audit-validation :error ::invalid-audit))
