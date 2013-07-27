(ns celestial.test.vc
  (:require vc.provider)
  (:use 
    midje.sweet
    [flatland.useful.map :only  (dissoc-in*)]
    [clojure.core.strint :only (<<)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [celestial.persistency :only (validate-system type-exists?)]
    [celestial.fixtures :only (redis-vc-spec with-conf with-m?)])
  (:import clojure.lang.ExceptionInfo) 
  )

(with-conf
  (do
    (fact "basic construction"
      (let [vm (vconstruct redis-vc-spec)]
        (:allocation vm ) => {:datacenter "playground" :hostsystem "192.168.5.5" :pool "dummy" :disk-format :sparse}
        (:hostname vm)   => "red1"
        (:machine vm) => {:cpus 1 :memory 512 
                          :password "foobar" :user "ronen" :sudo true
                          :gateway "192.168.5.1" :ip "192.168.5.91" :mask "255.255.255.0"
                          :names ["8.8.8.8"] :network "192.168.5.0" :search "local"
                          :template "ubuntu-13.04_puppet-3.2"}))

    (fact "missing datacenter"
          (vconstruct (assoc-in redis-vc-spec [:vcenter :datacenter] nil)) => 
            (throws ExceptionInfo (with-m? {:allocation {:datacenter '("must be present")}})))

    (fact "wrong disk format"
          (vconstruct (assoc-in redis-vc-spec [:vcenter :disk-format] :full)) => 
            (throws ExceptionInfo (with-m? {:allocation {:disk-format '("disk format must be either #{:flat :sparse}")}})))

    (fact "missing mask for existing ip"
          (vconstruct (assoc-in redis-vc-spec [:machine :mask] nil)) => 
            (throws ExceptionInfo (with-m? {:machine {:mask '("must be present")}})))

    (fact "missing hostsystem"
          (vconstruct (assoc-in redis-vc-spec [:vcenter :hostsystem] nil)) => 
            (throws ExceptionInfo (with-m? {:allocation {:hostsystem '("must be present")}})))

    (fact "missing mask for missing ip"
          (vconstruct (merge-with merge redis-vc-spec {:machine {:mask nil :ip nil}})) => truthy)

    (fact "entity sanity"
        (validate-system redis-vc-spec) => truthy 
        (provided 
          (type-exists? "redis")  => true))

    (fact "missing guest password"
        (validate-system (dissoc-in* redis-vc-spec [:machine :password])) => 
          (throws ExceptionInfo (with-m? {:machine {:password '("must be present")}}))
        (provided (type-exists? "redis")  => true))

    (fact "names aren't a vec"
        (validate-system (assoc-in redis-vc-spec [:machine :names] "bla")) => 
          (throws ExceptionInfo (with-m? {:machine {:names '("must be a vector")}}))
        (provided (type-exists? "redis")  => true))
    ))



