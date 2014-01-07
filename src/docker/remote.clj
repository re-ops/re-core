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

(ns docker.remote
  "Docker http remote API client"
  (:require 
    [celestial.model :refer (clone hypervisor)] 
    [cheshire.core :refer :all]
    [clojure.core.strint :refer (<<)]
    [celestial.common :refer (curr-time minute import-logging)]
    [slingshot.slingshot :refer [try+]]  
    [clj-http.client :as client]))

(import-logging)

(defn root [node] 
  (<< "https://~(hypervisor :docker :nodes node :host):~(hypervisor :docker :nodes node :port)/"))

(defn call [verb api node]
  (parse-string (:body (verb (<< "~(root node)~{api}") {:insecure? true})) true))

(defn docker-post 
  "A post against a docker instance with provided params"
  ([node api] (docker-post api nil node))
  ([node api params] 
   (if (nil? params)
     (call client/post api {} node) 
     (call client/post api {:form-params params} node))))

(defn docker-delete [node api] (call client/delete api node))

(defn docker-get [node api] (call client/get api node))


(celestial.model/set-env :dev 
 (docker-get :local "/images/json?all=0")) 
  
