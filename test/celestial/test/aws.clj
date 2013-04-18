(ns celestial.test.aws
  (:import clojure.lang.ExceptionInfo)
  (:use 
    midje.sweet
    aws.provider
    [celestial.fixtures :only (redis-ec2-spec is-type?)]
    [celestial.model :only (vconstruct)]))


(with-redefs [ids (fn [] (atom {}))]
  (let [instance (vconstruct redis-ec2-spec)] 
    (fact "We cannot start a instance without an aws instance id"
          (.start instance) => (throws ExceptionInfo (is-type? :aws.provider/aws:missing-id)))))

