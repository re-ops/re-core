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

(ns freenas.remote
  (:require 
    [celestial.common :refer (import-logging)]
    [cheshire.core :refer :all]
    [celestial.model :refer (hypervisor)]
    [clojure.core.strint :refer (<<)]
    [org.httpkit.client :as client]))

(defn freenas [ks])

(defn root 
   []
   (<< "https://~(hypervisor :freenas :host)/api/v1.0/"))

(defn auth  
   []
  [(hypervisor :freenas :user) (hypervisor :freenas :password)]
  )

(defn call [verb api args]
   @(verb (<< "~(root)~{api}") (merge args {:basic-auth (auth) :insecure? true})))


;; (clojure.pprint/pprint (parse-string (:body (call client/get  "jails/jails/?format=json" {})) true))
;; (clojure.pprint/pprint (parse-string (:body (call client/get  "jails/templates/?format=json" {})) true))

