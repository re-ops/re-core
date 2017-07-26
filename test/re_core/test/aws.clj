(ns re-core.test.aws
  (:require
   [re-core.fixtures.data :refer (redis-ec2-spec)]
   [re-core.fixtures.core :refer (is-type?)]
   [re-core.model :refer (vconstruct set-env)])
  (:use midje.sweet aws.provider)
  (:import clojure.lang.ExceptionInfo))

(set-env :dev
         (with-redefs [assign-vpc (fn [_ m] m)]
           (let [instance (vconstruct redis-ec2-spec)]
             (fact "We cannot start a instance without an aws instance id"
                   (.start instance) => (throws ExceptionInfo (is-type? :aws.provider/aws:missing-id))
                   (provided (instance-id* anything) => nil))
             (fact "min-count max-count"
                   (get-in instance [:spec :aws])  => (contains {:min-count 1 :max-count 1})))))

