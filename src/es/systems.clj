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

(ns es.systems
  "Systems indexing/searching"
  (:refer-clojure :exclude [get])
  (:require
    [es.common :refer (flush- index map-env-terms clear initialize)]
    [es.node :as node :refer (ES)]
    [clojure.set :refer (subset?)]
    [clojure.core.strint :refer (<<)]
    [slingshot.slingshot :refer  [throw+]]
    [re-core.common :refer (envs import-logging)]
    [clojurewerkz.elastisch.query :as q]
    [clojurewerkz.elastisch.native.document :as doc]))

(import-logging)

(def ^:dynamic flush? false)

(defmacro set-flush
   [flush* & body]
  `(binding [flush? ~flush*] ~@body))

(defn put
   "Add/Update a system into ES"
   [id system]
  (doc/put @ES index "system" id system)
  (when flush? (flush-)))

(defn delete
   "delete a system from ES"
   [id & {:keys [flush?]}]
  (doc/delete @ES index "system" id)
  (when flush? (flush-)))

(defn get
   "Grabs a system by an id"
   [id]
  (doc/get @ES index "system" id))

(defn query
   "basic query string"
   [query & {:keys [from size] :or {size 100 from 0}}]
  (doc/search @ES index "system" {
      :from from :size size :query query :fields ["owner" "env"]
   }))

(defn query-envs [q]
 (into #{}
   (filter identity
      (map #(keyword (get-in % [:term :env])) (get-in q [:bool :should])))))



