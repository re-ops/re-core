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
    [es.common :refer (flush- index map-env-terms clear initialize)]
    [clojure.set :refer (subset?)]
    [clojure.core.strint :refer (<<)] 
    [slingshot.slingshot :refer  [throw+]] 
    [celestial.persistency.users :as u]
    [celestial.common :refer (envs import-logging)]
    [celestial.roles :refer (su?)]
    [clojurewerkz.elastisch.query :as q]
    [clojurewerkz.elastisch.native.document :as doc]))

(import-logging)

(defn put
   "Add/Update a system into ES"
   [id system & {:keys [flush?]}]
  (doc/put index "system" id system)
  (when flush? (flush-)))

(defn delete
   "delete a system from ES"
   [id & {:keys [flush?]}]
  (doc/delete index "system" id)
  (when flush? (flush-)))

(defn get 
   "Grabs a system by an id"
   [id]
  (doc/get index "system" id))

(defn query 
   "basic query string" 
   [query & {:keys [from size] :or {size 100 from 0}}]
  (doc/search index "system" :from from :size size :query query :fields ["owner" "env"]))

(defn query-envs [q]
 (into #{} 
   (filter identity 
      (map #(keyword (get-in % [:term :env])) (get-in q [:bool :should])))))

(defn envs-set 
  "Set should envs on query" 
  [q {:keys [envs username]}]
  (let [es (query-envs q)] 
    (if-not (empty? es) 
      (if (subset? es (into #{} envs)) 
        (map-env-terms q)
        (throw+ {:type ::non-legal-env :message (<< "~{username} tried to query in ~{es} he has access only to ~{envs}")}))
      (update-in q [:bool :should] 
        (fn [v] (into v (mapv #(hash-map :term {:env (str %)}) envs)))))))

(defn- query-for [username q]
  (let [{:keys [envs username] :as user} (u/get-user! username)]
    (if (su? user)
      (-> q (envs-set user) 
          (assoc-in [:bool :minimum_should_match] 1))
      (update-in q [:bool :must] (fn [v] (into v {:term {"owner" username}}))))))

(defn systems-for
  "grabs all the systems ids that this user can manipulate from ES"
  [username q from size]
  (info "query for" (query-for username q))
  (query (query-for username q) :from from :size size))

(defn re-index  
   "Re-indexes a bulk of systems"
   [systems]
  (clear)
  (initialize)
  (doseq [[id s] systems] (put id s))
  (let [[id s] (first systems)] 
     (put id s :flush? true)))
