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

(ns es.jobs
  "Jobs ES persistency"
  (:refer-clojure :exclude [get])
  (:require
    [es.node :as node :refer (ES)]
    [es.common :refer (flush- index)]
    [clojurewerkz.elastisch.native.document :as doc]
    [taoensso.timbre :refer (refer-timbre)]
    [re-core.common :refer (envs)]))

(refer-timbre)

(defn put
   "Add/Update a jobs into ES"
   [{:keys [tid queue status] :as job} ttl & {:keys [flush?]}]
  (doc/put @ES index "jobs" tid (merge job {:queue (name queue) :status (name status)}) {:ttl ttl})
  (when flush? (flush-)))

(defn delete
   "delete a system from ES"
   [id]
  (doc/delete @ES index "jobs" id))

(defn get
   "Grabs a system by an id"
   [id]
  (doc/get @ES index "jobs" id))

(defn query-envs
   "maps envs to query form terms"
   [envs]
   (map (fn [e] {:term {:env (name e)}}) envs))

(defn paginate
   "basic query string"
   [from size envs]
  (let [q {:bool {:minimum_should_match 1 :should (query-envs envs)}}]
    (:hits
      (doc/search @ES index "jobs" {
          :from from :size size :query q :sort {:end "desc"}
        }))))
