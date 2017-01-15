(ns celestial.integration.persistency.quotas
 (:import clojure.lang.ExceptionInfo)
 (:use midje.sweet)
 (:require
    [celestial.fixtures.core :refer (is-type? with-admin with-conf)]
    [celestial.fixtures.data :refer (redis-kvm-spec user-quota foo)]
    [celestial.fixtures.populate :refer (add-types add-users re-initlize)]
    [celestial.persistency.users :as u]
    [celestial.persistency.systems :as s]
    [celestial.persistency.quotas :as q]))

(defn quotas-populate []
  (re-initlize)
  (add-types)
  (add-users)
  (u/add-user foo)
  (q/add-quota (assoc-in user-quota [:quotas :dev :kvm :used :count] 0)) )

(with-conf
  (with-state-changes [(before :facts (quotas-populate))]
    (let [redis-kvm-spec' (assoc redis-kvm-spec :owner "foo")]
      (fact "basic quota usage" :integration :redis :quota
        (q/increase-use redis-kvm-spec') => "OK"
        (get-in (q/get-quota "foo") [:quotas :dev])  => (contains {:kvm {:limits {:count 2} :used {:count 1}}})
        (q/increase-use redis-kvm-spec')
        (get-in (q/get-quota "foo") [:quotas :dev])  => (contains {:kvm {:limits {:count 2} :used {:count 2}}})
        (q/with-quota redis-kvm-spec') => (throws ExceptionInfo (is-type? :celestial.persistency.quotas/quota-limit-reached))
        (q/decrease-use redis-kvm-spec')
        (get-in (q/get-quota "foo") [:quotas :dev])  => (contains {:kvm {:limits {:count 2} :used {:count 1}}})
        (q/increase-use redis-kvm-spec')
        (get-in (q/get-quota "foo") [:quotas :dev])  => (contains {:kvm {:limits {:count 2} :used {:count 2}}})
            )

    (fact "system quota interception" :integration :redis :quota
      (with-admin
        (let [id (s/add-system redis-kvm-spec')]
          (s/add-system redis-kvm-spec') => truthy
          (s/add-system redis-kvm-spec') => (throws ExceptionInfo (is-type? :celestial.persistency.quotas/quota-limit-reached))
          (s/delete-system id) => truthy
          (s/add-system redis-kvm-spec') => truthy))))

    (fact "quotasless user can run forever" :integration :redis :quota
      (let [redis-prox-admin (assoc redis-kvm-spec :owner "admin")]
        (with-admin
          (s/add-system redis-prox-admin) => truthy
          (s/add-system redis-prox-admin) => truthy
          (s/add-system redis-prox-admin) => truthy
          (s/add-system redis-prox-admin) => truthy
          ))
      )
  ))


