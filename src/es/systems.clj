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

(ns es.systems
  "Systems indexing/searching"
  (:refer-clojure :exclude [get])
  (:require 
    [es.node :as node]
    [puny.migrations :refer (Migration register)]
    [celestial.persistency :as p]
    [celestial.common :refer (envs)]
    [celestial.persistency.systems :as s]
    [celestial.roles :refer (su?)]
    [clojurewerkz.elastisch.native :as es]
    [clojurewerkz.elastisch.query :as q]
    [clojurewerkz.elastisch.native.index :as idx]
    [clojurewerkz.elastisch.native.document :as doc]))

(def ^:const index "celestial-systems")

(def ^:const system-types
  {:system 
   {:properties {
      :owner {:type "string" }
      :env {:type "string" :index "not_analyzed"}
      :type {:type "string"}
     }
    }
   }
  )

(defn initialize 
  "Creates systems index and type" 
  []
  (node/start-n-connect)
  (when-not (idx/exists? index)
    (idx/create index :mappings system-types)))

(defn put
   "Add/Update a system into ES"
   [id system]
  (doc/put index "system" id system))

(defn get 
   "Grabs a system by an id"
   [id]
  (doc/get index "system" id))

(defn query 
   "basic query string" 
   [query & {:keys [from size] :or {size 100 from 0}}]
  (doc/search index "system" :from from :size size :query query :fields ["owner" "env"]))

(defn- query-for [username q]
  (let [{:keys [envs username] :as user} (p/get-user! username)]
    (if (su? user)
      (let [ts (mapv #(hash-map :term {"env" (str %)}) envs)] 
        (-> q (update-in [:bool :should] (fn [v] (into v ts))) (assoc-in [:bool :minimum_should_match] 1)))
      (update-in q [:bool :must] (fn [v] (into v {:term {"owner" username}}))))))

(defn systems-for
  "grabs all the systems ids that this user can manipulate from ES"
  [username q from size]
  (query (query-for username q) :from from :size size))

; indexing all systems
(defrecord ElasticSystems [identifier]
  Migration
  (apply- [this]
    (initialize)
    (doseq [id (flatten (map #(s/get-system-index :env (keyword %)) (envs)))]  
      (put id (s/get-system id))))  

  (rollback [this]))

(defn register-migrations []
  (register :systems-es (ElasticSystems. :systems-es-indexing)))
