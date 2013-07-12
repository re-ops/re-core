(ns celestial.test.aws
  (:import clojure.lang.ExceptionInfo)
  (:use 
    midje.sweet
    aws.provider
    [aws.sdk.ec2 :only (start-instances)]
    [celestial.fixtures :only (redis-ec2-spec is-type?)]
    [celestial.model :only (vconstruct)]))


(let [instance (vconstruct redis-ec2-spec)] 
  (fact "We cannot start a instance without an aws instance id"
        (.start instance) => (throws ExceptionInfo (is-type? :aws.provider/aws:missing-id))
    (provided 
      (instance-id* anything) => nil))
  (fact "min-count max-count"
        (get-in instance [:spec :aws])  => (contains {:min-count 1 :max-count 1})))

