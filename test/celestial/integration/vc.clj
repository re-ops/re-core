(ns celestial.integration.vc
  "vCenter integration test, require a vcenter instance to be configured"
  (:use 
     midje.sweet 
     vc.provider 
     vc.guest
     [vc.vijava :only (guest-status)]
     [celestial.fixtures :only (redis-vc-spec with-conf)]
     [celestial.model :only (vconstruct)])
   (:require 
     [hypervisors.networking :refer (clear-range initialize-networking)]
     [flatland.useful.map :refer (dissoc-in*)]))

(with-conf
  (with-state-changes [(before :facts (do (clear-range :vcenter) (initialize-networking)))] 
    (fact "creating a virtualmachine" :integration :vcenter
      (let [vm (.create (vconstruct redis-vc-spec))]
        (try 
         (.start vm) 
         (.status vm)  => "running"
         (guest-status (get-in redis-vc-spec [:machine :hostname]))  => :running 
         (.stop vm) 
         (.status vm)  => "stopped" 
         (finally
           (when-not (= (.status vm) "stopped") (.stop vm)) 
           (.delete vm))))) 
  
   (fact "generated ip" :integration :vcenter
      (let [vm (.create (vconstruct (dissoc-in* redis-vc-spec [:machine :ip])))]
        (try 
         (.start vm) 
         (.status vm)  => "running"
         (guest-status (get-in redis-vc-spec [:machine :hostname]))  => :running 
         (finally
           (when-not (= (.status vm) "stopped") (.stop vm)) 
           (.delete vm)))))))
