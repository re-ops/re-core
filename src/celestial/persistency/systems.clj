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
    [celestial.security :refer (current-user)]
    [celestial.roles :refer (admin?)]
    [robert.hooke :as h]
    [celestial.persistency :as p]
    [celestial.common :refer (import-logging)]
    [physical.validations :as ph]
    [proxmox.validations :as pv]
    [aws.validations :as av]
    [vc.validations :as vc]
    [subs.core :as subs :refer (validate!)]
    [puny.core :refer (entity)]
    [slingshot.slingshot :refer  [throw+]]
    [puny.migrations :refer (Migration register)]
    [celestial.model :refer (clone hypervizors figure-virt)] 
    [clojure.core.strint :refer (<<)]  
    proxmox.model aws.model))

(import-logging)

(declare perm validate-system)

(entity {:version 1} system :indices [type env] 
        :intercept {:create [perm] :read [perm] :update [perm] :delete [perm]} )

(defn assert-access [env ident]
  {:pre [(current-user)]}
  (let [username ((current-user) :username) user (p/get-user! username)
        envs (into #{} (user :envs))]
    (when (and env (not (envs env))) 
      (throw+ {:type ::persmission-violation} (<< "~{username} attempted to access system ~{ident} in env ~{env}")))))

(defn perm
  "checking current user env permissions" 
  [f & args]
  (let [ident (first args)]
    (cond
      (map? ident) (assert-access (ident :env) ident) 
      :default (assert-access (h/with-hooks-disabled get-system (get-system ident :env)) ident)) 
    (apply f args)))

(defn system-ip [id]
  (get-in (get-system id) [:machine :ip]))

(def hyp-to-v {
   :physical ph/validate-entity 
   :proxmox pv/validate-entity 
   :aws av/validate-entity 
   :vcenter vc/validate-entity})

(defn validate-system
  [system]
  (validate! system {:type #{:required :type-exists} :env #{:required :Keyword}} :error ::non-valid-machine-type )
  ((hyp-to-v (figure-virt system)) system))

(defn clone-system 
  "clones an existing system"
  [id hostname]
  (add-system (clone (assoc-in (get-system id) [:machine :hostname] hostname))))
 
(defrecord EnvIndices [identifier]
   Migration
   (apply- [this]
     (doseq [id (all-systems)]  
       (update-system id (update-in (get-system id) [:env] keyword))))  
   (rollback [this])) 

(defn register-migrations []
  (register :systems (EnvIndices. :systems-env-indices)))

(defn systems-for
  "grabs all the systems ids that this user can see"
   [username]
  (let [envs ((p/get-user username) :envs)]
   (flatten (map #(get-system-index :env (keyword %)) envs))))


