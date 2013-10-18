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
  (:require 
    [celestial.common :refer (import-logging)]
    [subs.core :as subs :refer (validate! combine when-not-nil validation every-v every-kv)]
    [cemerick.friend.credentials :as creds]
    [cemerick.friend :as friend]
     proxmox.model aws.model)
  (:use 
    [puny.core :only (entity)]
    [celestial.roles :only (roles admin)]
    [slingshot.slingshot :only  [throw+]]
    [celestial.model :only (clone hypervizors figure-virt)] 
    [clojure.core.strint :only (<<)]))

(import-logging)

(def user-version 1)

(declare migrate-user)

(entity {:version user-version}  user :id username :intercept {:read [migrate-user]})

(defn into-v1-user [user]
  (let [upgraded (assoc user :envs [:dev])]
    (trace "migrating" user "to version 1")
    (update-user upgraded) upgraded))

(defn migrate-user
  "user migration"
  [f & args] 
  (let [res (apply f args) version (-> res meta :version)]
    (if (and (map? res) (not (empty? res)))
      (cond
        (nil? version) (into-v1-user res)
        :else res)
       res)))
 
(def user-v
  {:username #{:required :String} :password #{:required :String} 
   :roles #{:required :role*} :envs #{:required :env*}})

(validation :role (when-not-nil roles (<< "role must be either ~{roles}")))

(validation :role* (every-v #{:role}))

(validation :env* (every-v #{:Keyword}))

(defn validate-user [user]
  (validate! user user-v :error ::non-valid-user))

(entity type :id type)

(def puppet-std-v 
  {:classes #{:required :Map}
   :puppet-std {
      :args #{:Vector} 
      :module {
         :name #{:required :String}
         :src  #{:required :String}      
    }}})

(defn validate-type [{:keys [puppet-std] :as t}]
  (validate! t (combine (if puppet-std puppet-std-v {}) {:type #{:required :String}}) :error ::non-valid-type ))

(entity action :indices [operates-on])

(defn find-action-for [action-key type]
  (let [ids (get-action-index :operates-on type) 
        actions (map #(-> % Long/parseLong  get-action) ids)]
    (first (filter #(-> % :actions action-key nil? not) actions))))

(defn cap? [m] (contains? m :capistrano))

(def cap-nested {:capistrano {:args #{:required :Vector}}})

(validation :type-exists (when-not-nil type-exists? "type not found, create it first"))

(def action-base-validation
  {:src #{:required :String} :operates-on #{:required :String :type-exists}})

(defn validate-action [{:keys [actions] :as action}]
  (doseq [[k {:keys [capistrano] :as m}] actions] 
    (when capistrano (validate! m cap-nested :error ::invalid-action)))
  (validate! action action-base-validation :error ::invalid-nested-action ))

(defn reset-admin
  "Resets admin password if non is defined"
  []
  (when (empty? (get-user "admin"))
    (add-user {:username "admin" :password (creds/hash-bcrypt "changeme") :roles admin :envs [:dev]})))

(entity quota :id username)

(validation :user-exists (when-not-nil user-exists? "No matching user found"))

(validation :quota* (every-kv {:limit #{:required :Integer}}))

(def quota-v
  {:username #{:required :user-exists} :quotas #{:required :Map :quota*}})

(defn validate-quota [q]
  (validate! q quota-v :error ::non-valid-quota))

(defn curr-user []
  (:username (friend/current-authentication)))

(defn used-key [spec]
  [:quotas (figure-virt spec) :used])

(defn quota-assert
  [user spec]
  (let [hyp (figure-virt spec) {:keys [limit used]} (get-in (get-quota user) [:quotas hyp])]
    (when (= (count used) limit)
      (throw+ {:type ::quota-limit-reached} (<< "Quota limit ~{limit} on ~{hyp} for ~{user} was reached")))))

(defn quota-change [id spec f]
  (let [user (curr-user)]
    (when (quota-exists? user)
      (update-quota 
        (update-in (get-quota user) (used-key spec) f id)))))

(defn increase-use 
  "increases user quota use"
  [id spec]
  (quota-change id spec (fnil conj #{id})))

(defn decrease-use 
  "decreases usage"
  [id spec]
  (when-not (empty? (get-in (get-quota (curr-user)) (used-key spec)))
    (quota-change id spec (fn [old id*] (clojure.set/difference old #{id*})))))

(defmacro with-quota [action spec & body]
  `(do 
     (quota-assert (curr-user) ~spec)
     (let [~'id ~action]
       (increase-use  ~'id ~spec)    
       ~@body)))

