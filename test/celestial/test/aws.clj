(ns celestial.test.aws
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.fixtures.data :refer (redis-ec2-spec)]
    [celestial.fixtures.core :refer (is-type?)]
    [celestial.model :refer (vconstruct)])
  (:use midje.sweet aws.provider))


(let [instance (vconstruct redis-ec2-spec)] 
  (fact "We cannot start a instance without an aws instance id"
        (.start instance) => (throws ExceptionInfo (is-type? :aws.provider/aws:missing-id))
    (provided 
      (instance-id* anything) => nil))
  (fact "min-count max-count"
        (get-in instance [:spec :aws])  => (contains {:min-count 1 :max-count 1})))

