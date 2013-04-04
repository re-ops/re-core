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

(ns celestial.persistency
  (:refer-clojure :exclude [type])
  (:use 
    [clojure.string :only (split join)]
    [celestial.redis :only (wcar hsetall*)]
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.core.strint :only (<<)]) 
  (:require 
    [taoensso.carmine :as car]))

(defn tk [id] (<< "type:~{id}"))

(defn hk [id] (<< "host:~{id}"))

(defn type-of [t]
  "Reading a type"
  (if-let [res (wcar (car/get (tk t)))] res 
    (throw+ {:type ::missing-type :t t})))

(defn new-type [t spec]
  "An application type and its spec see fixtures/redis-type.edn"
  (wcar (car/set (tk t) spec)))

(defn register-host [{:keys [type machine] :as props}]
  {:pre [(type-of type)]}
  "Mapping host to a given type and its machine"
  (hsetall* (hk (machine :hostname)) props))

(defn host-exists?
  "checks if host exists"
  [h] (not= 0 (wcar (car/exists (hk h)))))

(defn host 
  "Reads host data"
  [h]
  (let [data (wcar (car/hgetall* (hk h)))]
    (if-not (empty? data)
      data 
      (throw+ {:type ::missing-host :host h}))))

(defn update-host 
  "Updates a given host"
  [h m]
  (hsetall* (hk h) (merge-with merge m (host h))))

(defn delete-host 
  "Deletes a given host"
  [host]
  (wcar (car/del (hk host))))

(defn fuzzy-host [h]
  "Searches after a host in a fuzzy manner, first fqn then tried prefixes"
  (let [ks (reverse (reductions (fn [r v] (str r "." v)) (split h #"\.")))]
    (when-let [k (first (filter #(= 1 (wcar (car/exists (hk %)))) ks))]
      (host k))))
(comment 
  (new-type "z" {}) 
  (register-host "foo" "z" {:foo 1}) 
  (host "foo")) 
