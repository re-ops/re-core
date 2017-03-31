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

(ns es.node
  "An embedded ES node instance"
  (:import
    [org.elasticsearch.node NodeBuilder])
  (:require
    [taoensso.timbre :refer (refer-timbre)]
    [safely.core :refer [safely]]
    [re-core.common :refer (get!)]
    [clojurewerkz.elastisch.native.conversion :as cnv]
    [clojurewerkz.elastisch.native :as es]))

(refer-timbre)

(def ES (atom nil))

(defn connect-
   "Connecting to Elasticsearch"
   []
  (let [{:keys [host port cluster]} (get! :elasticsearch)]
    (info "Connecting to elasticsearch")
    (reset! ES (es/connect  [[host port]] {"cluster.name" cluster}))))

(defn connect
  "Connecting to Elasticsearch with retry support"
  []
  (let [{:keys [host port cluster]} (get! :elasticsearch)]
    (safely (connect-)
       :on-error
       :max-retry 5
       :message "Error while trying to connect to Elasticsearch"
       :log-errors true
       :retry-delay [:random-range :min 2000 :max 5000])))

(defn stop
  "stops embedded ES node"
  []
  (info "Stoping local elasticsearch node")
  (.close @ES)
  (reset! ES nil))

(defn health [indices]
  (.name (.getStatus @ES)))

