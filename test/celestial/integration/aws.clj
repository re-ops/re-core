(ns celestial.integration.aws
  "Tests ec2, requires access key and secret key to be defined in ~/.celestial.edn"
  (:require aws.provider)
  (:import clojure.lang.ExceptionInfo)
  (:use 
    [celestial.model :only (vconstruct)]
     clojure.test
     [celestial.fixtures :only (redis-ec2-spec)]))


(deftest ^:ec2 full-cycle 
    (let [instance (vconstruct redis-ec2-spec)]
      (.create instance) 
      (.start instance)
      (is (= (.status instance) "running"))
      (.stop instance)
      (is (= (.status instance) "stopped"))
      (.delete instance) 
      (is (thrown? ExceptionInfo (.status instance)))))


