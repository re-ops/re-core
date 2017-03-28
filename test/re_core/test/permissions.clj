(ns re-core.test.permissions
  "Testing permission hooks on systems"
  (:import clojure.lang.ExceptionInfo)
  (:require
    [re-core.security :refer (current-user)]
    [re-core.fixtures.core :refer  (is-type?)]
    [re-core.fixtures.data :refer (redis-kvm-spec)]
    [re-core.persistency.systems :as s :refer (perm)]
    [re-core.persistency.users :as u]
    [re-core.persistency.systems :as s])
  (:use midje.sweet))

(def curr-admin {:username "admin"})

(def curr-ronen {:username "ronen"})

(def curr-foo {:username "foo"})

(def admin {:username "admin" :roles [:re-core.roles/admin] :envs [:dev] :operations []})

(def ronen {:username "ronen" :roles [:re-core.roles/user] :envs [:dev] :operations []})

(def foo {:username "foo" :roles [:re-core.roles/user] :envs [:dev] :operations []})

(tabular "id based access owner interception"
  (fact
   (perm identity ?id) => ?res
   (provided
    (s/get-system ?id :skip-assert) => redis-kvm-spec
    (current-user) =>  ?curr
    (u/get-user! anything) => ?user))
   ?id  ?curr      ?user ?res
   "1" curr-admin  admin "1"
    1  curr-admin  admin  1
    1  curr-ronen  ronen  1
    1  curr-foo    foo    (throws ExceptionInfo  (is-type? :re-core.persistency.systems/persmission-owner-violation))
)

(tabular "id based access env interception"
  (fact
    (perm identity ?id) => ?res
    (provided
      (s/get-system ?id :skip-assert) => (assoc redis-kvm-spec :env "prod")
      (current-user) =>  ?curr
      (u/get-user! anything) => ?user))
   ?id  ?curr  ?user ?res
   "1" curr-admin  admin (throws ExceptionInfo  (is-type? :re-core.persistency.systems/persmission-env-violation))
    1  curr-ronen (assoc-in ronen [:envs 0] "prod") 1)
