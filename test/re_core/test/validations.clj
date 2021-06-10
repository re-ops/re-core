(ns re-core.test.validations
  (:require
   [re-core.model :refer (check-validity)]
   [re-core.fixtures.data :refer
    (redis-type redis-physical)])
  (:use clojure.test)
  (:import clojure.lang.ExceptionInfo))

(defn errors [f args]
  (try (f args)
       (throw (ex-info "expected an exception with info" {}))
       (catch ExceptionInfo e
         (-> e ex-data :errors))))

(deftest physical-validations
  (is (= {} (check-validity redis-physical)))

  (is (= {:physical {:mac "must be a legal mac address"}}
         (errors check-validity (assoc-in redis-physical [:physical :mac] "aa:bb"))))

  (is (= {:physical {:broadcast "must be a legal ip address"}}
         (errors check-validity (assoc-in redis-physical [:physical :broadcast] "a.1.2")))))
