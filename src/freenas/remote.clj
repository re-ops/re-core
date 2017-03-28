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

(ns freenas.remote
  (:require 
    [slingshot.slingshot :refer  [throw+]]
    [taoensso.timbre :as timbre]
    [re-core.common :refer (import-logging)]
    [cheshire.core :refer :all]
    [re-core.model :refer (hypervisor)]
    [clojure.core.strint :refer (<<)]
    [org.httpkit.client :as client]))

(timbre/refer-timbre)

(defn freenas [ks])

(defn root 
   []
   (<< "https://~(hypervisor :freenas :host)/api/v1.0/"))

(defn auth  
   []
  [(hypervisor :freenas :username) (hypervisor :freenas :password)]
  )

(defn defaults []
  {:basic-auth (auth) :insecure? true :headers {"Content-Type" "application/json"}})

(defn call 
  ([verb api] (call verb api nil))
  ([verb api params]
   (let [args* (merge (defaults) {:body (generate-string params)} {})
         {:keys [body status] :as res} @(verb (<< "~(root)~{api}") args*)]
     (when-not (#{200 201 202 204} status) 
       (info status)
       (throw+ (assoc res :type ::call-failed)))
     (-> body (parse-string true)))))

