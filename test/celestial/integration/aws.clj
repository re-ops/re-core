(ns celestial.integration.aws
  "Tests ec2, requires access key and secret key to be defined in ~/.celestial.edn"
  (:require aws.provider)
  (:import clojure.lang.ExceptionInfo)
  (:require [celestial.persistency :as p])
  (:use 
    midje.sweet
    [celestial.model :only (vconstruct)]
    [celestial.redis :only (clear-all)]
    [celestial.fixtures :only (redis-ec2-spec redis-type)]))


(fact "aws full scenario works" :ec2 :integration
      (clear-all)
      (p/add-type redis-type)
      (let [system-id (p/add-system redis-ec2-spec) 
            instance (vconstruct (assoc redis-ec2-spec :system-id system-id))
            hostname (get-in redis-ec2-spec [:machine :hostname])] 
        (.create instance) 
        (.start instance) 
        (get-in (p/get-system system-id) [:machine :ssh-host]) => truthy 
        (.status instance) => "running"
        (.stop instance) 
        (.status instance) => "stopped"
        (.delete instance) 
        (.status instance) => falsey))

