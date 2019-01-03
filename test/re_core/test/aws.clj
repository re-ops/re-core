(ns re-core.test.aws
  (:require
   [re-core.fixtures.data :refer (redis-ec2-spec)]
   [re-core.fixtures.core :refer (is-type?)]
   [re-core.model :refer (vconstruct set-env)])
  (:use clojure.test aws.provider)
  (:import clojure.lang.ExceptionInfo))

(deftest aws-sanity
  (with-redefs [assign-vpc (fn [_ m] m) instance-id* (fn [_] nil)]
    (let [instance (vconstruct redis-ec2-spec)]
      (testing "We cannot start a instance without an aws instance id"
        (is (thrown-with-msg? ExceptionInfo  #"Instance id not found" (.start instance))))
      (testing "min-count max-count"
        (is (= (get-in instance [:spec :aws :min-count]) 1))
        (is (= (get-in instance [:spec :aws :max-count]) 1))))))

