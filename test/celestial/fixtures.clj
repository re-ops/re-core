(ns celestial.fixtures
  (:refer-clojure :exclude [type])
  (:use [celestial.common :only (slurp-edn)]))

(def redis-prox-spec (slurp-edn "fixtures/redis-system.edn"))

(def redis-type (slurp-edn "fixtures/redis-type.edn"))

(def redis-ec2-spec (slurp-edn "fixtures/redis-ec2-system.edn"))

(def redis-vsphere-spec (slurp-edn "fixtures/redis-vsphere-system.edn"))

(def local-prox (slurp-edn "fixtures/.celestial.edn"))

(def cap-deploy (slurp-edn "fixtures/cap-deploy.edn"))

(def user-quota (slurp-edn "fixtures/user-quota.edn"))

(defn is-type? [type]
  (fn [exception] 
    (= type (get-in (.getData exception) [:object :type]))))

(defn with-m? [m]
  (fn [actual]
    (= (get-in (.getData actual) [:object :errors]) m)))

(defmacro with-conf 
  "Using fixture/.celestial.edn conf file"
  [body]
 `(with-redefs [celestial.config/config celestial.fixtures/local-prox]
   ~body
    ))
