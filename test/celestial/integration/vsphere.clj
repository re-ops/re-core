(ns celestial.integration.vsphere
  "VSphere integration test, require a vpshere instance to be configured"
  (:use 
     midje.sweet 
     vsphere.provider 
     [celestial.fixtures :only (redis-vsphere-spec with-conf)]
     [celestial.model :only (vconstruct)]))

(with-conf
  (fact "creating a virtualmachine" :integration :vsphere
    (let [vm (vconstruct redis-vsphere-spec)]
      (.create vm)
      (.start vm)
      (.status vm)  => :running
      (.stop vm)
      (.status vm)  => :stopped
      (.delete vm)
      )))
