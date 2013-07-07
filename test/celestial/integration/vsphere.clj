(ns celestial.integration.vsphere
  "VSphere integration test, require a vpshere instance to be configured"
  (:use 
     midje.sweet 
     vsphere.provider 
     vsphere.guest
     [vsphere.vijava :only (guest-status)]
     [celestial.fixtures :only (redis-vsphere-spec with-conf)]
     [celestial.model :only (vconstruct)]))

(with-conf
  (fact "creating a virtualmachine" :integration :vsphere
    (let [vm (vconstruct redis-vsphere-spec)]
      (.create vm)
      (.start vm)
      (.status vm)  => :running
      (guest-status (get-in redis-vsphere-spec [:machine :hostname]))  => :running
      (.stop vm)
      (.status vm)  => :stopped
      (.delete vm)
      )))
