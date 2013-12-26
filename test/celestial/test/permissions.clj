(ns celestial.test.permissions
  "Testing permission hooks on systems"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.security :refer (current-user)]
    [celestial.fixtures.core :refer  (is-type?)]
    [celestial.fixtures.data :refer (redis-prox-spec)]
    [celestial.persistency.systems :as s :refer (perm)]
    [celestial.persistency :as p]
    [celestial.persistency.systems :as s])
  (:use midje.sweet))

(def curr-admin {:username "admin"})

(def curr-ronen {:username "ronen"})

(def curr-foo {:username "foo"})

(def admin {:username "admin" :roles [:celestial.roles/admin] :envs [:dev]})

(def ronen {:username "ronen" :roles [:celestial.roles/user] :envs [:dev]})

(def foo {:username "foo" :roles [:celestial.roles/user] :envs [:dev]})

(tabular "id based access owner interception"
  (fact 
   (perm identity ?id) => ?res 
   (provided 
    (s/get-system ?id :skip-assert) => redis-prox-spec
    (current-user) =>  ?curr
    (p/get-user! anything) => ?user))
   ?id  ?curr      ?user ?res
   "1" curr-admin  admin "1"
    1  curr-admin  admin  1 
    1  curr-ronen  ronen  1 
    1  curr-foo    foo    (throws ExceptionInfo  (is-type? :celestial.persistency.systems/persmission-owner-violation))
)

(tabular "id based access env interception"
  (fact 
   (perm identity ?id) => ?res 
   (provided 
    (s/get-system ?id :skip-assert) => (assoc redis-prox-spec :env "prod")
    (current-user) =>  ?curr
    (p/get-user! anything) => ?user)
  )
   ?id  ?curr  ?user ?res
   "1" curr-admin  admin (throws ExceptionInfo  (is-type? :celestial.persistency.systems/persmission-env-violation))
    1  curr-ronen (assoc-in ronen [:envs 0] "prod") 1)
