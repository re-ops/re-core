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
    [taoensso.carmine :as car]
    [cemerick.friend :as friend]
    [celestial.validations :as cv]
    proxmox.model aws.model)
  (:use 
    [puny.core :only (entity)]
    [celestial.roles :only (roles admin)]
    [cemerick.friend.credentials :as creds]
    [bouncer [core :as b] [validators :as v :only (defvalidatorset defvalidator)]]
    [celestial.validations :only (validate! validate!! validate-nest)]
    [clojure.string :only (split join)]
    [celestial.redis :only (wcar hsetall*)]
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.model :only (clone hypervizors figure-virt)] 
    [clojure.core.strint :only (<<)]))

(entity user :id username)

(defn validate-user [user]
  (validate! 
    (b/validate user
       [:username] [v/required cv/str?]
       [:password] [v/required cv/str?]
       [:roles] [v/required (v/every #(roles %) :message (<< "role must be either ~{roles}"))]) 
       ::non-valid-user))

(entity type :id type)

(defn type-base-v [v]
  (b/validate v [:type] [v/required]))

(defn puppet-std-v [t]
  (validate-nest t [:puppet-std]
                 [:args]         [(cv/vec? :pre (comp not nil?))]
                 [:module :name] [v/required cv/str?]
                 [:module :src]  [v/required cv/str?]))

(defn classes-v [t]
  (validate t 
      [:classes] [v/required cv/hash?]))

(defn validate-type [t]
  (validate! 
    (cond-> (-> t type-base-v second)
      (t :puppet-std) (-> classes-v second puppet-std-v)) ::non-valid-type))

(entity action :indices [operates-on])

(defn find-action-for [action-key type]
  (let [ids (get-action-index :operates-on type) 
        actions (map #(-> % Long/parseLong  get-action) ids)]
    (first (filter #(-> % :actions action-key nil? not) actions))))

(defn cap? [m] (contains? m :capistrano))

(defvalidatorset nested-action-validation
  [:capistrano :args] [(v/required :pre cap?) cv/vec?])

(defvalidatorset action-base-validation
  :src [v/required cv/str?]
  :operates-on [v/required cv/str?]
  :operates-on [(v/custom type-exists? :message (<< "Given actions target type ~(action :operates-on) not found, create it first"))]
  )

(defn validate-action [{:keys [actions] :as action}]
  (doseq [[k m] actions] 
    (cv/validate!! ::invalid-action m nested-action-validation))
  (cv/validate!! ::invalid-nested-action action action-base-validation))
 
(entity system :indices [type])

(defvalidatorset system-type
  :type [(v/custom type-exists? :message (<< "Given system type ~(system :type) not found, create it first"))]
  )

(def hyp-to-v 
  {:proxmox pv/validate-entity
   :aws    av/validate-entity 
   })

(defn validate-system
  [system]
  (validate!! ::non-valid-machine-type system system-type)
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

(defvalidator hypervisor-ks
  {:default-message-format (<<  "quotas keys must be one of ~{hypervizors}")}
  [qs]
  (empty? (remove hypervizors (keys qs))))

(defvalidator int-limits
  {:default-message-format (<<  "quotas limits must be integers ")}
  [qs]
  (empty? (remove (fn [[k v]] (integer? (v :limit))) qs)))

(defn validate-quota [q]
  (validate! 
    (b/validate q
                [:username] [v/required (v/custom user-exists? :message "No matching user found")]
                [:quotas]  [v/required cv/hash? hypervisor-ks int-limits])
    ::non-valid-quota))

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

