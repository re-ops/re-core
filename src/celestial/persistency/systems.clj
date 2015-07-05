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
    [es.systems :as es]
    [celestial.roles :refer (su? system?)]
    [celestial.security :refer (current-user)]
    [robert.hooke :as h]
    [celestial.persistency 
      [users :as u] [types :as t]]
    [celestial.persistency.quotas :as q]
    [celestial.common :refer (import-logging)]
    [physical.validations :as ph]
    [freenas.validations :as fv]
    [proxmox.validations :as pv]
    [openstack.validations :as ov]
    [docker.validations :as dv]
    [aws.validations :as av]
    [vc.validations :as vc]
    [subs.core :as subs :refer (validate! validation when-not-nil)]
    [puny.core :refer (entity)]
    [slingshot.slingshot :refer  [throw+]]
    [puny.migrations :refer (Migration register)]
    [celestial.model :refer (clone hypervizors figure-virt check-validity)] 
    [clojure.core.strint :refer (<<)]  
    proxmox.model aws.model))

(import-logging)

(declare perm validate-system increase-quota decrease-quota es-put es-delete)

(entity {:version 1} system :indices [type env owner] 
        :intercept {
            :create [perm increase-quota es-put]
            :read [perm] :update [perm es-put]
            :delete [perm decrease-quota es-delete]})

(defn assert-access 
  "Validates that the current user can access the system, 
   non super users can only access systems they own.
   All users are limited to certain environments."
  [{:keys [env owner] :as system}]
  {:pre [(current-user)]}
  (let [{:keys [envs username] :as curr-user} (u/get-user! ((current-user) :username))]
    (when-not (empty? system)
      (when (and (not (su? curr-user)) (not= username owner))
        (throw+ {:type ::persmission-owner-violation} (<< "non super user ~{username} attempted to access a system owned by ~{owner}!"))
      )
     (when (and (not (system? curr-user)) env (not ((into #{} envs) env))) 
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

(defn es-put
   "runs a specified es function on system fn call" 
   [f & args]
     (if (map? (first args)) 
      (let [id (apply f args) spec (first args)]  
        (es/put (str id) spec) id)
      (apply f args)))

(defn es-delete
   "reducing usage quotas for owning user on delete" 
   [f & args]
  (let [system (first (filter map? args)) id (first (filter number? args))]
   (when-not (is-system? system) 
     (es/delete (str id) :flush? true)))
   (apply f args))


(defn system-ip [id]
  (get-in (get-system id) [:machine :ip]))

(validation :type-exists (when-not-nil t/type-exists? "type not found, create it first"))

(validation :user-exists (when-not-nil u/user-exists? "user not found"))

(def system-base {
   :owner #{:required :user-exists}
   :type #{:required :type-exists} 
   :env #{:required :Keyword}
  })

(defn validate-system
  [system]
  (validate! system system-base :error ::non-valid-system)
  (check-validity system))

(defn clone-system 
  "clones an existing system"
  [id {:keys [hostname owner] :as spec}]
  (add-system 
    (-> (get-system id) 
      (assoc :owner owner) 
      (assoc-in [:machine :hostname] hostname)
      (clone spec))))

(defn systems-for
  "grabs all the systems ids that this user can see"
  [username]
  (let [{:keys [envs username] :as user} (u/get-user username)]
    (if (su? user)
      (flatten (map #(get-system-index :env (keyword %)) envs))
      (get-system-index :owner username))))

(defn re-index 
   "Re-indexes all systems available to the current user under elasticsearch."
   [username]
   (es/re-index (map #(vector % (get-system %)) (systems-for username))))

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
  (register :systems (EnvIndices. :systems-env-indices)))

(declare validate-template)

(entity {:version 1} template :indices [type name])

(def template-base {
  :type #{:required :type-exists} :defaults #{:required :Map}
  :name #{:required :String}
})

(defn validate-template
  [template]
  (validate! template template-base :error ::non-valid-template)
  (check-validity (assoc template :as :template)))

(defn templatize
  "Create a system from a template"
   [id {:keys [env] :as provided}]
   (let [{:keys [defaults] :as t} (get-template! id)]
     (add-system (merge-with merge t (defaults env) provided))))
