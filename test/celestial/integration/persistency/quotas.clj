(ns celestial.integration.persistency.quotas
 (:import clojure.lang.ExceptionInfo)
 (:use midje.sweet)
 (:require 
    [celestial.redis :refer (clear-all)]  
    [celestial.fixtures.core :refer (is-type? with-admin)]
    [celestial.fixtures.data :refer (redis-prox-spec user-quota)]
    [celestial.fixtures.populate :refer (add-types)]
    [celestial.persistency :as p]
    [celestial.persistency.systems :as s]
    [celestial.persistency.quotas :as q]))

(defn quotas-populate []
  (clear-all)
  (add-types)
  (p/add-user {:username "foo" :password "bla" :roles {} :envs []})
  (q/add-quota (assoc-in user-quota [:quotas :proxmox :used] nil)) )

(with-state-changes [(before :facts (quotas-populate))]
  (let [redis-prox-spec' (assoc redis-prox-spec :owner "foo")]
    (fact "basic quota usage" :integration :redis :quota
       (q/increase-use 1 redis-prox-spec') => "OK"
       (:quotas (q/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1}}}) 
       (q/increase-use 2  redis-prox-spec') 
       (:quotas (q/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1 2}}}) 
       (q/with-quota (fn [] 1) redis-prox-spec') => (throws ExceptionInfo (is-type? :celestial.persistency.quotas/quota-limit-reached)) 
       (q/decrease-use 2 redis-prox-spec') 
       (:quotas (q/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1}}})  
       (q/increase-use 3  redis-prox-spec') 
       (:quotas (q/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1 3}}}))
   
  (fact "system quota interception" :integration :redis :quota
    (with-admin 
      (let [id (s/add-system redis-prox-spec')]
        (s/add-system redis-prox-spec') => truthy
        (s/add-system redis-prox-spec') => (throws ExceptionInfo (is-type? :celestial.persistency.quotas/quota-limit-reached))
        (s/delete-system id) => truthy
        (s/add-system redis-prox-spec') => truthy)))
    )
  )


