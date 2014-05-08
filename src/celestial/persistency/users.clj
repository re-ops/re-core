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

(ns celestial.persistency.users
  (:require 
    [celestial.model :as m]
    [cemerick.friend.credentials :as creds]
    [celestial.roles :refer (roles admin)]
    [celestial.common :refer (import-logging)]
    [cemerick.friend :as friend]
    [subs.core :as subs :refer (validate! combine when-not-nil validation every-v every-kv)]
    [puny.core :refer (entity)]
    [clojure.core.strint :refer (<<)]
    ))

(import-logging)

(def user-version 2)

(declare migrate-user)

(entity {:version user-version}  user :id username :intercept {:read [migrate-user]})

(defn into-v1-user [user]
  (let [upgraded (assoc user :envs [:dev])]
    (trace "migrating" user "to version 1")
    (update-user upgraded) upgraded))

(defn into-v2-user [user]
  (let [upgraded (assoc user :operations m/operations)]
    (trace "migrating" user "to version 2")
    (update-user upgraded) upgraded))

(defn migrate-user
  "user migration"
  [f & args] 
  (let [res (apply f args) version (-> res meta :version)]
    (if (and (map? res) (not (empty? res)))
      (cond
        (nil? version) (into-v1-user res)
        (= version 1) (into-v2-user res)
        :else res)
       res)))
 
(def user-v
  {:username #{:required :String!} :password #{:required :String!} 
   :roles #{:required :role*} :envs #{:required :env*} :operations #{:required :operation*}})

(validation :role (when-not-nil roles (<< "role must be either ~{roles}")))

(validation :role* (every-v #{:role}))

(validation :env* (every-v #{:Keyword}))

(validation :operation (when-not-nil m/operations (<< "operation must be either ~{m/operations}")))

(validation :operation* (every-v #{:operation}))

(defn validate-user [user]
  (validate! user user-v :error ::non-valid-user))
 
(defn reset-admin
  "Resets admin password if non is defined"
  []
  (when (empty? (get-user "admin"))
    (info "Reseting admin password")
    (add-user {
      :username "admin" :password (creds/hash-bcrypt "changeme")
      :roles admin :envs [:dev] :operations m/operations
     })))

(defn curr-user []
  (:username (friend/current-authentication)))

(defn op-allowed?
   "Checks if user can perform an operation" 
   [op {:keys [operations] :as user}]
    ((into #{} operations) (keyword op)))
