(ns celestial.integration.aws
  "Tests ec2, requires access key and secret key to be defined in ~/.celestial.edn"
  (:require aws.provider)
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [aws.provider :as  awp]
    [celestial.persistency :as p])
  (:use 
    midje.sweet
    [celestial.model :only (vconstruct)]
    [celestial.redis :only (clear-all)]
    [celestial.fixtures :only (redis-ec2-spec redis-type host puppet-ami)]))

(fact "aws full scenario works" :ec2  :integration
      (clear-all)
      (p/add-type redis-type)
      (let [system-id (p/add-system puppet-ami) 
            instance (vconstruct (assoc puppet-ami :system-id system-id)) ]
        (.create instance) 
        (.start instance) 
        (get-in (p/get-system system-id) [:machine :ssh-host]) => truthy 
        (.status instance) => "running"
        (.stop instance) 
        (.status instance) => "stopped"
        (.delete instance) 
        (.status instance) => "terminated"))

 
(fact "aws with elastic ip" :ec2 :integration :eip
      (clear-all)
      (p/add-type redis-type)
      (let [system-id (p/add-system puppet-ami) 
            updated-ami (merge-with merge puppet-ami {:machine {:ip "54.217.236.112"} :system-id system-id})
            instance (vconstruct updated-ami)]
        (.create instance) 
        (.start instance) 
        (get-in (p/get-system system-id) [:machine :ssh-host]) => "ec2-54-217-236-112.eu-west-1.compute.amazonaws.com"
        (.status instance) => "running"
        (.stop instance) 
        (.status instance) => "stopped"
        (.delete instance) 
        (.status instance) => "terminated"))
