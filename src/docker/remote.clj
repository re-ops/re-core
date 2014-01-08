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
    [flatland.useful.map :refer [map-keys]]
    [camel-snake-kebab :as k]
    [celestial.model :refer (clone hypervisor)] 
    [cheshire.core :refer :all]
    [clojure.core.strint :refer (<<)]
    [celestial.common :refer (curr-time minute import-logging)]
    [slingshot.slingshot :refer [try+]]  
    [clj-http.client :as client]))

(import-logging)

(defn root [node] 
  (<< "https://~(hypervisor :docker :nodes node :host):~(hypervisor :docker :nodes node :port)/"))

(defn uncamel [m]
 (map-keys m k/->kebab-case)) 

(defn uncamelize
   "uncammel responses" 
   [m]
 (if (sequential? m)
   (map uncamel m) 
   (uncamel m)))


(defn call [verb api args node]
  (-> (verb (<< "~(root node)~{api}") (merge args {:insecure? true}))
    :body (parse-string true) uncamelize))

(defn docker-post 
  "A post against a docker instance with provided params"
  ([node api] (docker-post api nil node))
  ([node api params] 
   (if (nil? params)
     (call client/post api {} node) 
     (call client/post api 
        {:form-params (map-keys params k/->CamelCase) :content-type :json} node))))

(defn docker-delete [node api] (call client/delete api {} node))

(defn docker-get [node api] 
  (call client/get api {} node))

(comment 
  (celestial.model/set-env :dev (docker-get :local "/images/json?all=0"))) 

