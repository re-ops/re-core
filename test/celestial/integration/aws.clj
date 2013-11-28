(ns celestial.integration.aws
  "Tests ec2, requires access key and secret key to be defined in ~/.celestial.edn"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.fixtures.core :refer  (with-conf with-admin)]  
    [celestial.fixtures.data :refer (redis-type local-conf redis-ec2-spec)]  
    [aws.provider :as awp]
    [celestial.persistency :as p])
  (:use 
    midje.sweet
    [celestial.model :only (vconstruct)]
    [celestial.redis :only (clear-all)]
    ))

(defn flow [ami ssh-host]
  (let [system-id (s/add-system ami) instance (vconstruct (assoc ami :system-id system-id)) ]
        (.create instance) 
        (.start instance) 
        (get-in (s/get-system system-id) [:machine :ssh-host]) => ssh-host
        (.status instance) => "running"
        (.stop instance) 
        (.status instance) => "stopped"
        (.delete instance) 
        (.status instance) => "terminated"))

(with-admin
  (with-conf local-conf
   (with-state-changes [(before :facts (populate-system redis-type redis-ec2-spec))]
    (fact "aws full scenario works" :ec2  :integration (flow redis-ec2-spec truthy))
 
    (fact "aws with elastic ip" :ec2 :integration :eip
       (flow (merge-with merge redis-ec2-spec {:machine {:ip "54.217.236.112"}})
           "ec2-54-217-236-112.eu-west-1.compute.amazonaws.com")) 

    (fact "aws with volumes" :ec2 :integration :volumes
      (flow (merge-with merge redis-ec2-spec 
        {:aws {:volumes [{:device "/dev/sdn" :size 10 :clear true}]}}) truthy))))) 
