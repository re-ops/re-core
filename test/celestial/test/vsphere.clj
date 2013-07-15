(ns celestial.test.vsphere
  (:require vsphere.provider)
  (:use 
    midje.sweet
    [flatland.useful.map :only  (dissoc-in*)]
    [clojure.core.strint :only (<<)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [celestial.persistency :only (validate-system type-exists?)]
    [celestial.fixtures :only (redis-vsphere-spec with-conf with-m?)])
  (:import clojure.lang.ExceptionInfo) 
  )

(with-conf
  (do
    (fact "basic construction"
      (let [vm (vconstruct redis-vsphere-spec)]
        (:allocation vm ) => {:datacenter "playground" :pool "" :disk-format :sparse}
        (:hostname vm)   => "red1"
        (:machine vm) => {:cpus 1 :memory 512 
                          :password "foobar" :user "ronen" :sudo true
                          :gateway "192.168.5.1" :ip "192.168.5.91" :mask "255.255.255.0"
                          :names ["8.8.8.8"] :network "192.168.5.0" :search "local"
                          :template "ubuntu-13.04_puppet-3.2.2_with-tools" }))

    (fact "missing datacenter"
          (vconstruct (assoc-in redis-vsphere-spec [:vsphere :datacenter] nil)) => 
            (throws ExceptionInfo (with-m? {:allocation {:datacenter '("datacenter must be present")}})))

    (fact "wrong disk format"
          (vconstruct (assoc-in redis-vsphere-spec [:vsphere :disk-format] :full)) => 
            (throws ExceptionInfo (with-m? {:allocation {:disk-format '("disk format must be either #{:flat :sparse}")}})))

    (fact "missing mask for existing ip"
          (vconstruct (assoc-in redis-vsphere-spec [:machine :mask] nil)) => 
            (throws ExceptionInfo (with-m? {:machine {:mask '("mask must be present")}})))

    (fact "missing mask for missing ip"
          (vconstruct (merge-with merge redis-vsphere-spec {:machine {:mask nil :ip nil}})) => truthy)

    (fact "entity sanity"
        (validate-system redis-vsphere-spec) => truthy 
        (provided 
          (type-exists? "redis")  => true))

    (fact "missing guest password"
        (validate-system (dissoc-in* redis-vsphere-spec [:machine :password])) => 
          (throws ExceptionInfo (with-m? {:machine {:password '("password must be present")}}))
        (provided (type-exists? "redis")  => true))

    (fact "names aren't a vec"
        (validate-system (assoc-in redis-vsphere-spec [:machine :names] "bla")) => 
          (throws ExceptionInfo (with-m? {:machine {:names '("names must be a vector")}}))
        (provided (type-exists? "redis")  => true))
    ))



