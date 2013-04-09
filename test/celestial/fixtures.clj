(ns celestial.fixtures
  (:refer-clojure :exclude [type])
  (:use [celestial.common :only (slurp-edn)]))

(def redis-prox-spec (slurp-edn "fixtures/redis-system.edn"))

(def redis-type (slurp-edn "fixtures/redis-type.edn"))

(def redis-ec2-spec (slurp-edn "fixtures/redis-ec2-system.edn"))

(def local-prox (slurp-edn "fixtures/.celestial.edn"))

(defn is-type? [type]
  (fn [exception] (= type (get-in (.getData exception) [:object :type]))))

(defmacro with-conf 
  "Using fixture/.celestial.edn conf file"
  [body]
 `(with-redefs [celestial.config/config celestial.fixtures/local-prox]
   ~body
    ))
