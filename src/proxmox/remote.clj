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

(ns proxmox.remote
  (:require 
    [cheshire.core :refer :all]
    [clj-http.client :as client])
  (:use [proxmox.http-common :only (root http-opts)]
        [proxmox.auth :only (auth-headers)]
        [clojure.core.strint :only (<<)]
        [celestial.common :only (get! curr-time minute import-logging)]
        [slingshot.slingshot :only  [try+]])
  )

(import-logging)

(defn call [verb api args]
  (:data (parse-string 
           (:body (verb (<< "~(root)~{api}") (merge args http-opts {:headers (auth-headers)}))) true)))


(defn prox-post 
  "A post against a proxmox instance with provided params"
  ([api] (prox-post api nil))
  ([api params] 
   (if (nil? params)
     (call client/post api {}) 
     (call client/post api {:form-params params}))))

(defn prox-delete [api] (call client/delete api {}))

(defn prox-get [api] (call client/get api {}))

