(ns celestial.test.aws
  (:use 
    [celestial.fixtures :only (redis-ec2-spec)]
    expectations
    aws.provider
    [celestial.model :only (vconstruct)]
    ))

(let [instance (vconstruct redis-ec2-spec)] 
  (expect clojure.lang.ExceptionInfo (.start instance)))

