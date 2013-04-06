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
    [bouncer [core :as b] [validators :as v]]
    [celestial.validations :only (str-v validate!)]
    [clojure.string :only (split join)]
    [celestial.redis :only (wcar hsetall*)]
    [slingshot.slingshot :only  [throw+ try+]]
    [clojure.core.strint :only (<<)]) 
  (:require 
    [taoensso.carmine :as car]))



(defn tk 
  "type key"
  [id] (<< "type:~{id}"))

(defn hk 
  "host key"
  [id] (<< "host:~{id}"))

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

(defmacro <<< 
  "String interpulation into a symbol"
  [s] `(symbol (<< ~s)))

(defmacro <<k 
  "String interpulation into a keyword"
  [s] `(keyword (<< ~s)))

(defmacro entity
  "Generates all the persistency (add/delete/exists etc..) functions for given entity"
  [name*]
  (let [id-fn (<<< "~{name*}-id") gen-fn (<<< "gen-~{name*}-id")
        add-fn (<<< "add-~{name*}") delete-fn (<<< "delete-~{name*}")
        update-fn (<<< "update-~{name*}") get-fn (<<< "get-~{name*}")
        exists-fn (<<< "~{name*}-exists?") missing (<<k ":~{*ns*}/missing-~{name*}") 
        validate-fn (<<< "validate-~{name*}")]
    `(do 
       (declare ~validate-fn)

       (defn ~id-fn [~'id] (str '~name* ":" ~'id))

       (defn ~gen-fn []
         (wcar (~id-fn (car/incr ~(<< "~{name*}:ids")))))

       (defn ~add-fn [~'v]
         (~validate-fn ~'v)
         (let [id# (~gen-fn)]
           (wcar (hsetall* (~id-fn id#) ~'v)) id#)) 

       (defn ~get-fn [~'id] (wcar (car/hgetall* (~id-fn ~'id))))
        
       (defn ~delete-fn [~'id] (wcar (car/del (~id-fn ~'id))))

       (defn ~exists-fn [~'id] (not= 0 (wcar (car/exists (~id-fn ~'id)))))
       
       (defn ~update-fn [{:keys [~'id] :as ~'v}]
         (~validate-fn ~'v)
         (when-not (~exists-fn ~'id)
            (throw+ {:type ~missing ~(keyword name*) ~'v }))
         (wcar (hsetall* (~id-fn ~'id) (merge (wcar (car/hgetall* (~id-fn ~'id))) ~'v)))
         ))))

(entity user)

(defn validate-user [user]
  (validate! ::non-valid-user
    (b/validate user
      [:username] [v/required str-v]
      [:password] [v/required str-v]
      [:roles] [v/required])))

