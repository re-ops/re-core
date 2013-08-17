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
    [proxmox.validations :as pv]
    [aws.validations :as av]
    [vc.validations :as vc]
    [subs.core :as subs :refer (validate! combine when-not-nil validation every-v every-kv validation)]
    [taoensso.carmine :as car]
    [cemerick.friend :as friend]
    proxmox.model aws.model)
  (:use 
    [puny.core :only (entity)]
    [celestial.roles :only (roles admin)]
    [cemerick.friend.credentials :as creds]
    [clojure.string :only (split join)]
    [celestial.redis :only (wcar)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (clone hypervizors figure-virt)] 
    [clojure.core.strint :only (<<)]))

(entity user :id username)

(def user-v
 {:username #{:required :String} :password #{:required :String} :roles #{:required :role*}})

(validation :role (when-not-nil roles (<< "role must be either ~{roles}")))

(validation :role* (every-v #{:role}))

(defn validate-user [user]
  (validate! user user-v :error ::non-valid-user))

(entity type :id type)

(def puppet-std-v 
  {
   :classes #{:required :Map}
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
 
(entity system :indices [type])

(defn system-ip [id]
  (get-in (get-system id) [:machine :ip]))


(def hyp-to-v 
  {:proxmox pv/validate-entity 
   :aws av/validate-entity 
   :vcenter vc/validate-entity})

(defn validate-system
  [system]
  (validate! system {:type #{:type-exists}} :error ::non-valid-machine-type )
  ((hyp-to-v (figure-virt system)) system))

(defn clone-system 
  "clones an existing system"
  [id hostname]
  (add-system (clone (assoc-in (get-system id) [:machine :hostname] hostname))))

(defn reset-admin
  "Resets admin password if non is defined"
  []
  (when (empty? (get-user "admin"))
    (add-user {:username "admin" :password (creds/hash-bcrypt "changeme") :roles admin})))

(entity quota :id username)

#_(defvalidator hypervisor-ks
  {:default-message-format (<<  "quotas keys must be one of ~{hypervizors}")}
  [qs]
  (empty? (remove hypervizors (keys qs))))

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

