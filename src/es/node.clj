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

(ns es.node
  "An embedded ES node instance"
  (:import 
    [org.elasticsearch.node NodeBuilder])
  (:require 
    [celestial.common :refer (get! import-logging)]
    [clojurewerkz.elastisch.native.conversion :as cnv]
    [clojurewerkz.elastisch.native :as es]))

(import-logging)

(def ES (atom nil))

(defn build-local-node
  [settings]
    (.build 
      (doto (NodeBuilder/nodeBuilder)
      (.settings (cnv/->settings settings)) 
      (.data true)
      (.local true))))


(defn settings 
   "embedded ES settings" 
   []
   (merge (get! :elasticsearch) {
      :node.name "celestial" :cluser.name "celestial-cluster"
      :http.enabled false 
      :index.number_of_shards 1
      :index.number_of_replicas 1
    }))

(defn start
  "launch en embedded ES node" 
  []
  (info "Starting local elasticsearch node")
  (reset! ES (es/start-local-node (build-local-node (settings)))))

(defn connect []
  (es/connect-to-local-node! @ES))

(defn start-n-connect 
   "Both starts the node and connects to it if not ready" 
   []
  (when-not (and @ES (.isClosed @ES)) 
    (start)
    (connect)))

(defn stop
  "stops embedded ES node" 
  []
  (info "Stoping local elasticsearch node")
  (.close @ES) 
  (reset! ES nil))
