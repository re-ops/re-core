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

(ns celestial.persistency.systems
  "systems persistency layer"
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.roles :refer (su?)]
    [celestial.security :refer (current-user)]
    [robert.hooke :as h]
    [celestial.persistency :as p]
    [celestial.persistency.quotas :as q]
    [celestial.common :refer (import-logging)]
    [physical.validations :as ph]
    [proxmox.validations :as pv]
    [docker.validations :as dv]
    [aws.validations :as av]
    [vc.validations :as vc]
    [subs.core :as subs :refer (validate! validation when-not-nil)]
    [puny.core :refer (entity)]
    [slingshot.slingshot :refer  [throw+]]
    [puny.migrations :refer (Migration register)]
    [celestial.model :refer (clone hypervizors figure-virt)] 
    [clojure.core.strint :refer (<<)]  
    proxmox.model aws.model))

(import-logging)

(declare perm validate-system increase-quota decrease-quota)

(entity {:version 1} system :indices [type env owner] 
        :intercept {
            :create [perm increase-quota]
            :read [perm] :update [perm]
            :delete [perm decrease-quota]})

(defn assert-access 
  "Validates that the current user can access the system, 
   non super users can only access systems they own.
   All users are limited to certain environments."
  [{:keys [env owner] :as system}]
  {:pre [(current-user)]}
  (let [{:keys [envs username] :as curr-user} (p/get-user! ((current-user) :username))]
    (when-not (empty? system)
      (when (and (not (su? curr-user)) (not= username owner))
        (throw+ {:type ::persmission-owner-violation} (<< "non super user ~{username} attempted to access a system owned by ~{owner}!"))
      )
     (when (and env (not ((into #{} envs) env))) 
      (throw+ {:type ::persmission-env-violation} (<< "~{username} attempted to access system ~{system} in env ~{env}"))))))

(defn is-system? [s]
  (and (map? s) (s :owner) (s :env)))

(defn perm
  "A permission interceptor on systems access, we check both env and owner persmissions.
  due to the way robert.hooke works we analyse args and not fn to decide what to verify on.
  If we have a map we assume its a system if we have a number we assume its an id." 
  [f & args]
  (let [system (first (filter map? args)) 
        id (first (filter #(or (number? %) (and (string? %) (re-find #"\d+" %))) args))
        skip (first (filter #{:skip-assert} args))]
    (when-not skip
      (trace "perm checking" f args)
      (if (is-system? system) 
        (assert-access system)
        (assert-access (get-system id :skip-assert)))) 
    (if skip (apply f (butlast args)) (apply f args))))

(defn decrease-quota 
   "reducing usage quotas for owning user on delete" 
   [f & args]
  (let [system (first (filter map? args))]
   (when (is-system? system) (q/decrease-use system)))
   (apply f args))

(defn increase-quota 
   "reducing usage quotas for owning user on delete" 
   [f & args]
   (if (map? (first args)) 
     (let [id (apply f args) spec (first args)]  
       (q/quota-assert spec)
       (q/increase-use spec) id)
     (apply f args)))

(defn system-ip [id]
  (get-in (get-system id) [:machine :ip]))

(def hyp-to-v {
               :physical ph/validate-entity 
               :proxmox pv/validate-entity 
               :docker dv/validate-entity 
               :aws av/validate-entity 
               :vcenter vc/validate-entity})

(validation :type-exists (when-not-nil p/type-exists? "type not found, create it first"))

(validation :user-exists (when-not-nil p/user-exists? "user not found"))

(def system-base {
   :owner #{:required :user-exists}
   :type #{:required :type-exists} 
   :env #{:required :Keyword}
  })

(defn validate-system
  [system]
  (validate! system system-base :error ::non-valid-machine-type)
  ((hyp-to-v (figure-virt system)) system))

(defn clone-system 
  "clones an existing system"
  [id {:keys [hostname owner] :as clone-spec}]
  (add-system 
    (-> (get-system id) 
      (assoc :owner owner) 
      (assoc-in [:machine :hostname] hostname)
      (clone clone-spec))))

(defn systems-for
  "grabs all the systems ids that this user can see"
  [username]
  (let [{:keys [envs username] :as user} (p/get-user username)]
    (if (su? user)
      (flatten (map #(get-system-index :env (keyword %)) envs))
      (get-system-index :owner username))))

; triggering env indexing and converting to keyword
(defrecord EnvIndices [identifier]
  Migration
  (apply- [this]
    (doseq [id (systems-for "admin")]  
      (update-system id (update-in (get-system id) [:env] keyword))))  
  (rollback [this])) 

; triggering owner indexing and setting default to admin
(defrecord OwnerIndices [identifier]
  Migration
  (apply- [this]
    (doseq [id (systems-for "admin")]  
      (when-not ((get-system id) :owner)
        (update-system id (assoc (get-system id) :owner "admin")))))  
  (rollback [this]))

; index all existing systems into ES

(defn register-migrations []
  (register :systems (OwnerIndices. :systems-owner-indices))
  (register :systems (EnvIndices. :systems-env-indices))
  )




