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
    [celestial.fixtures :only (redis-ec2-spec redis-type host puppet-ami with-conf local-conf)]))

(defn flow [ami ssh-host]
  (let [system-id (p/add-system ami) instance (vconstruct (assoc ami :system-id system-id)) ]
        (.create instance) 
        (.start instance) 
        (get-in (p/get-system system-id) [:machine :ssh-host]) => ssh-host
        (.status instance) => "running"
        (.stop instance) 
        (.status instance) => "stopped"
        (.delete instance) 
        (.status instance) => "terminated"))

(with-conf local-conf
  (with-state-changes [(before :facts (do (clear-all) (p/add-type redis-type)))]
    (fact "aws full scenario works" :ec2  :integration (flow puppet-ami truthy))
 
    (fact "aws with elastic ip" :ec2 :integration :eip
       (flow (merge-with merge puppet-ami {:machine {:ip "54.217.236.112"}})
           "ec2-54-217-236-112.eu-west-1.compute.amazonaws.com")) 

    (fact "aws with volumes" :ec2 :integration :volumes
      (flow (merge-with merge puppet-ami {:aws {:volumes [{:device "/dev/sdn" :size 10}]}}) truthy)))) 
