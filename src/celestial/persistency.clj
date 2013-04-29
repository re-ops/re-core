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
  (:require proxmox.model aws.model)
  (:use 
    [puny.core :only (entity)]
    [celestial.roles :only (roles admin)]
    [cemerick.friend.credentials :as creds]
    [bouncer [core :as b] [validators :as v]]
    [celestial.validations :only (str-v set-v validate! validate-nest)]
    [clojure.string :only (split join)]
    [celestial.redis :only (wcar hsetall*)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (clone)] 
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

(entity user :id username)

(defn validate-user [user]
  (validate! 
    (b/validate user
       [:username] [v/required str-v]
       [:password] [v/required str-v]
       [:roles] [v/required (v/every #(roles %) :message (<< "role must be either ~{roles}"))]) 
       ::non-valid-user))

(entity task)

(defn cap-v
  "Validates a capistrano task"
  [cap-task]
  (validate-nest cap-task [:capistrano]
                 [:src] [v/required str-v]
                 [:args] [v/required str-v]
                 [:name] [v/required str-v]))

(defn validate-task 
  "Validates task model"
  [task]
  (validate! 
    (cond-> task
      (task :capistrano) cap-v) ::non-valid-task))


(entity system)

(defn validate-system
  [system]
 (validate! 
    (b/validate system
      [:machine :hostname]  [v/required str-v])
     ::non-valid-machine))

(defn clone-system 
  "clones an existing system"
  [id]
  (add-system (clone (get-system id))))

(defn reset-admin
  "Resets admin password if non is defined"
  []
  (when (empty? (get-user "admin"))
    (add-user {:username "admin" :password (creds/hash-bcrypt "changeme") :roles admin})))


