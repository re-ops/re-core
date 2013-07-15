(ns celestial.integration.vc
  "vCenter integration test, require a vcenter instance to be configured"
  (:use 
     midje.sweet 
     vc.provider 
     vc.guest
     [vc.vijava :only (guest-status)]
     [celestial.fixtures :only (redis-vc-spec with-conf)]
     [celestial.model :only (vconstruct)]))

(with-conf
  (fact "creating a virtualmachine" :integration :vcenter
    (let [vm (vconstruct redis-vc-spec)]
      (.create vm)
      (.start vm)
      (.status vm)  => :running
      (guest-status (get-in redis-vc-spec [:machine :hostname]))  => :running
      (.stop vm)
      (.status vm)  => :stopped
      (.start vm)
      (.stop vm)
      (.delete vm)
      )))
