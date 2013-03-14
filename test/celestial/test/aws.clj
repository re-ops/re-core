(ns celestial.test.aws
  (:use 
    [celestial.fixtures :only (redis-ec2-spec)]
    expectations.scenarios
    aws.provider
    [celestial.model :only (vconstruct)]
    ))

(scenario 
  (with-redefs [ids (fn [] (atom {}))]
    (let [instance (vconstruct redis-ec2-spec)] 
      (expect clojure.lang.ExceptionInfo (.start instance)))))

