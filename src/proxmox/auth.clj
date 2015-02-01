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

(ns proxmox.auth
  (:require 
    [cheshire.core :refer :all]
    [proxmox.model :refer (proxmox-master)]
    [org.httpkit.client :as client])
  (:use 
    [proxmox.http-common :only (root http-opts)]
    [clojure.core.strint :only (<<)]
    [celestial.common :only (import-logging curr-time minute)]
    [slingshot.slingshot :only  [try+]])
  (:import clojure.lang.ExceptionInfo))

(import-logging)

(defn call- 
  "Calling without auth headers"
  [verb api args]
  (:data (parse-string (:body @(verb (<< "~(root)~{api}") (merge args http-opts))) true)))

(defn login-creds []
  (select-keys 
    (assoc (proxmox-master) :realm "pam") [:username :password :realm]))

(defn login []
  {:post [(not (nil? (% :CSRFPreventionToken))) (not (nil? (% :ticket)))]}
  (try+
    (let [res (call- client/post "/access/ticket" {:form-params (login-creds)})]
      (select-keys res [:CSRFPreventionToken :ticket]))
    (catch #(#{401 500} (:status %)) e
      (debug e)
      (throw (ExceptionInfo. "Failed to login" (proxmox-master))))
    (catch #(#{400} (:status %)) e
      (throw (ExceptionInfo. "Illegal request, check query params" (proxmox-master))))))

(defn fetch-headers []
  (trace "Refetching auth headers")
  (let [{:keys [CSRFPreventionToken ticket]} (login)]
    {"Cookie" (str "PVEAuthCookie=" ticket) "CSRFPreventionToken" CSRFPreventionToken}))

(def auth-store (atom {}))

(defn auth-expired? []
  (> (- (curr-time) (:modified @auth-store)) (* 5 minute)))

(defn auth-headers []
  (when (or (not (contains? @auth-store :headers)) (auth-expired?))
    (reset! auth-store {:headers (fetch-headers) :modified (curr-time)}))
  (@auth-store :headers))

