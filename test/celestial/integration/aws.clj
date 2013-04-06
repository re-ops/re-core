(ns celestial.integration.aws
  "Tests ec2, requires access key and secret key to be defined in ~/.celestial.edn"
  (:require aws.provider)
  (:import clojure.lang.ExceptionInfo)
  (:use 
    clojure.test
    midje.sweet
    [celestial.model :only (vconstruct)]
    [celestial.redis :only (clear-all)]
    [celestial.persistency :only (host register-host new-type)]  
    [celestial.fixtures :only (redis-ec2-spec redis-type)]))


(fact "aws full scenario works" :ec2 :integration
    (let [instance (vconstruct redis-ec2-spec) hostname (get-in redis-ec2-spec [:machine :hostname])]
      (clear-all)
      (new-type "redis" redis-type)
      (register-host redis-ec2-spec)
      (.create instance) 
      (.start instance)
      (is (not (nil? (get-in (host hostname) [:machine :ssh-host]))))
      (is (= (.status instance) "running"))
      (.stop instance)
      (is (= (.status instance) "stopped"))
      (.delete instance) 
      (is (= (.status instance) false))))

