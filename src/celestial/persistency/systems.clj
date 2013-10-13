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
    [celestial.persistency :as p]
    [celestial.common :refer (import-logging)]
    [cemerick.friend :as friend]
    [proxmox.validations :as pv]
    [aws.validations :as av]
    [vc.validations :as vc]
    [subs.core :as subs :refer (validate!)]
    [puny.core :refer (entity)]
    [slingshot.slingshot :refer  [throw+]]
    [celestial.model :refer (clone hypervizors figure-virt)] 
    [clojure.core.strint :refer (<<)]  
     proxmox.model aws.model))

(import-logging)

(declare perm migrate-system validate-system)

(entity {:version 1} system :indices [type env] 
   :intercept {:create [perm] :read [perm migrate-system] :update [perm] :delete [perm]} )

(defn into-v1-system [id system]
   (trace "migrating" system "to version 1")
   ; causing re-indexing
   (update-system id system) 
   system)

(defn migrate-system
  "system migration"
  [f & args] 
  (let [res (apply f args) version (-> res meta :version)]
    (if (and (map? res) (not (empty? res)))
      (cond
        (nil? version) (into-v1-system (first args) res)
        :else res)
       res)))
 
(defn assert-access [env ident]
  (let [username (:username (friend/current-authentication)) 
        envs (into #{} (-> username p/get-user! :envs))]
    (when (and env (not (envs env))) 
      (throw+ {:type ::persmission-violation} (<< "~{username} attempted to access system ~{ident} in env ~{env}")))))

(defn perm
  "checking current user env permissions" 
  [f & args]
  (let [ident (first args)]
    (cond
      (map? ident) (assert-access (ident :env) ident) 
      :default (assert-access (robert.hooke/with-hooks-disabled get-system (get-system ident :env)) ident)) 
    (apply f args)))

(defn system-ip [id]
  (get-in (get-system id) [:machine :ip]))

(def hyp-to-v {:proxmox pv/validate-entity 
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
 
