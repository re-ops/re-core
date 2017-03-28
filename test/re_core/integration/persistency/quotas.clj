(ns re-core.integration.persistency.quotas
 (:import clojure.lang.ExceptionInfo)
 (:use midje.sweet)
 (:require
    [re-core.fixtures.core :refer (is-type? with-admin with-conf)]
    [re-core.fixtures.data :refer (redis-kvm-spec user-quota foo)]
    [re-core.fixtures.populate :refer (add-types add-users re-initlize)]
    [re-core.persistency.users :as u]
    [re-core.persistency.systems :as s]
    [re-core.persistency.quotas :as q]))

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
        (q/with-quota redis-kvm-spec') => (throws ExceptionInfo (is-type? :re-core.persistency.quotas/quota-limit-reached))
        (q/decrease-use redis-kvm-spec')
        (get-in (q/get-quota "foo") [:quotas :dev])  => (contains {:kvm {:limits {:count 2} :used {:count 1}}})
        (q/increase-use redis-kvm-spec')
        (get-in (q/get-quota "foo") [:quotas :dev])  => (contains {:kvm {:limits {:count 2} :used {:count 2}}})
            )

    (fact "system quota interception" :integration :redis :quota
      (with-admin
        (let [id (s/add-system redis-kvm-spec')]
          (s/add-system redis-kvm-spec') => truthy
          (s/add-system redis-kvm-spec') => (throws ExceptionInfo (is-type? :re-core.persistency.quotas/quota-limit-reached))
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


