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
    [celestial.validations :as cv]
    proxmox.model aws.model)
  (:use 
    [puny.core :only (entity)]
    [celestial.roles :only (roles admin)]
    [cemerick.friend.credentials :as creds]
    [bouncer [core :as b] [validators :as v]]
    [celestial.validations :only (validate! validate-nest)]
    [clojure.string :only (split join)]
    [celestial.redis :only (wcar hsetall*)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (clone hypervizors)] 
    [clojure.core.strint :only (<<)]) 
  (:require 
    [taoensso.carmine :as car]))

(entity user :id username)

(defn validate-user [user]
  (validate! 
    (b/validate user
       [:username] [v/required cv/str?]
       [:password] [v/required cv/str?]
       [:roles] [v/required (v/every #(roles %) :message (<< "role must be either ~{roles}"))]) 
       ::non-valid-user))

(entity task)

(defn cap-v
  "Validates a capistrano task"
  [cap-task]
  (validate-nest cap-task [:capistrano]
                 [:src] [v/required cv/str?]
                 [:args] [v/required cv/str?]
                 [:name] [v/required cv/str?]))

(defn validate-task 
  "Validates task model"
  [task]
  (validate! 
    (cond-> task
      (task :capistrano) cap-v) ::non-valid-task))

(entity type :id type)

(defn type-base-v [v]
  (b/validate v [:type] [v/required]))

(defn puppet-std-v [t]
  (validate-nest t [:puppet-std]
    [:module :name] [v/required cv/str?]
    [:module :src] [v/required cv/str?]))

(defn classes-v [t]
   (validate t [:classes] [v/required cv/hash?]))

(defn validate-type [t]
  (validate! 
    (cond-> (-> t type-base-v second)
     (t :puppet-std) (-> classes-v second puppet-std-v) ) ::non-valid-type))

(entity system :indices [type])

(defn validate-system
  [system]
  (validate! 
    (b/validate system
       [:type] [(v/custom type-exists? :message "missing system type")]
       [:machine :hostname]  [v/required cv/str?])
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


(entity quota)

(defvalidator hypervisor-ks
  {:default-message-format (<<  "quotas keys must be one of ~{hypervizors}")}
  [qs]
  (empty? (remove hypervizors (keys qs))))

(defn validate-quota [q]
  (validate! 
    (b/validate q
       [:user] [v/required (v/custom user-exists? :message "No matching user found")]
       [:quotas]  [v/required cv/hash? hypervisor-ks ])
    ::non-valid-quota)
  )
