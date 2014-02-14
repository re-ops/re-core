(ns celestial.integration.persistency.quotas
 (:import clojure.lang.ExceptionInfo)
 (:use midje.sweet)
 (:require 
    [celestial.redis :refer (clear-all)]  
    [celestial.fixtures.core :refer (is-type?)]
    [celestial.fixtures.data :refer (redis-prox-spec user-quota)]
    [celestial.persistency :as p]
    [celestial.persistency.quotas :as q]
   
   ))

(with-state-changes [(before :facts (clear-all))]
  (fact "basic quota usage" :integration :redis :quota
        (let [redis-prox-spec' (assoc redis-prox-spec :owner "foo") ]
          (p/add-user {:username "foo" :password "bla" :roles {} :envs []})
          (q/add-quota (assoc-in user-quota [:quotas :proxmox :used] nil)) 
          (q/increase-use 1 redis-prox-spec') => "OK"
          (:quotas (q/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1}}}) 
          (q/increase-use 2  redis-prox-spec') 
          (:quotas (q/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1 2}}}) 
          (q/with-quota (fn [] 1) redis-prox-spec') => (throws ExceptionInfo (is-type? :celestial.persistency.quotas/quota-limit-reached)) 
          (q/decrease-use 2 redis-prox-spec') 
          (:quotas (q/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1}}})  
          (q/increase-use 3  redis-prox-spec') 
          (:quotas (q/get-quota "foo"))  => (contains {:proxmox {:limit 2 :used #{1 3}}})) 
        ))
