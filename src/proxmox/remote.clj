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
  (:use [clojure.core.strint :only (<<)]
        [celestial.common :only (get*)]
        [slingshot.slingshot :only  [try+]]
        [taoensso.timbre :only (debug info error) :as timbre])
  (:import clojure.lang.ExceptionInfo)) 


(defn root [] (<< "https://~(get* :hypervisor :proxmox :host):8006/api2/json"))

(defn retry
  "A retry for http calls against proxmox, some operations lock machine even after completion"
  [ex try-count _]
  (debug "re-trying due to" ex "attempt" try-count)
  (Thread/sleep 1000) 
  (if (> try-count 1) false true))

(def http-opts 
  {:insecure? true :retry-handler retry })

(declare auth-headers)

(defn call [verb api args]
  (:data (parse-string 
    (:body (verb (<< "~(root)~{api}") (merge args http-opts {:headers (auth-headers)}))) true)))

(defn call- 
  "Calling without auth headers"
  [verb api args]
  (:data (parse-string (:body (verb (<< "~(root)~{api}") (merge args http-opts))) true)))

(defn proxmox-conf [] (get* :hypervisor :proxmox))

(defn login-creds []
  (select-keys 
    (assoc (proxmox-conf) :realm "pam") [:username :password :realm]))

(defn login []
  {:post [(not (nil? (% :CSRFPreventionToken))) (not (nil? (% :ticket)))]}
  (try+
    (let [res (call- client/post "/access/ticket" {:form-params (login-creds)})]
      (select-keys res [:CSRFPreventionToken :ticket]))
    (catch #(#{401 500} (:status %)) e
      (debug e)
      (throw (ExceptionInfo. "Failed to login" (proxmox-conf))))
    (catch #(#{400} (:status %)) e
      (throw (ExceptionInfo. "Illegal request, check query params" (proxmox-conf))))))

(def auth-headers
  (memoize 
    (fn []
      (let [{:keys [CSRFPreventionToken ticket] :as headers} (login)]
        (assoc 
          (assoc {} "Cookie" (str "PVEAuthCookie=" ticket)) "CSRFPreventionToken" CSRFPreventionToken)))))

(defn prox-post 
  "A post against a proxmox instance with provided params"
  ([api] (prox-post api nil))
  ([api params] 
   (if (nil? params)
     (call client/post api {:headers auth-headers}) 
     (call client/post api {:form-params params :headers auth-headers}))))

(defn prox-delete [api] (call client/delete api {:headers auth-headers}))

(defn prox-get [api] (call client/get api {:headers auth-headers}))

