(ns celestial.test.vc
  (:require vc.provider 
    [flatland.useful.map :refer (dissoc-in*)]
    [clojure.core.strint :refer (<<)]
    [celestial.config :refer (config)]
    [celestial.model :refer (vconstruct)]
    [celestial.persistency :as p]
    [celestial.fixtures.data :refer (redis-vc-spec)]
    [celestial.fixtures.core :refer (with-conf with-m?)] 
    [celestial.persistency.systems :as s])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo))

(with-conf
  (do
    (fact "basic construction"
      (let [vm (vconstruct redis-vc-spec)]
        (:allocation vm ) => {:datacenter "playground" :hostsystem "192.168.5.5" :pool "dummy" :disk-format :sparse}
        (:hostname vm)   => "red1"
        (:machine vm) => {:cpus 1 :memory 512 
                          :password "foobar" :user "ronen" :sudo true
                          :gateway "192.168.5.1" :ip "192.168.5.91" :netmask "255.255.255.0"
                          :domain "local" :names ["8.8.8.8"] :search "local"
                          :template "ubuntu-13.04_puppet-3.2"}))

    (fact "missing datacenter"
          (vconstruct (assoc-in redis-vc-spec [:vcenter :datacenter] nil)) => 
            (throws ExceptionInfo (with-m? {:allocation {:datacenter '("must be present")}})))

    (fact "wrong disk format"
          (vconstruct (assoc-in redis-vc-spec [:vcenter :disk-format] :full)) => 
            (throws ExceptionInfo (with-m? {:allocation {:disk-format '("disk format must be either #{:flat :sparse}")}})))

    (fact "missing mask for existing ip"
          (vconstruct (assoc-in redis-vc-spec [:machine :netmask] nil)) => 
            (throws ExceptionInfo (with-m? {:machine {:netmask '("must be present")}})))

    (fact "missing domain"
          (vconstruct (assoc-in redis-vc-spec [:machine :domain] nil)) => 
            (throws ExceptionInfo (with-m? {:machine {:domain '("must be present")}})))


    (fact "missing hostsystem"
          (vconstruct (assoc-in redis-vc-spec [:vcenter :hostsystem] nil)) => 
            (throws ExceptionInfo (with-m? {:allocation {:hostsystem '("must be present")}})))

    (fact "missing mask for missing ip"
          (vconstruct (merge-with merge redis-vc-spec {:machine {:netmask nil :ip nil}})) => truthy)

    (fact "entity sanity"
        (s/validate-system redis-vc-spec) => truthy 
        (provided (p/type-exists? "redis")  => true)
        (provided (p/user-exists? "admin")  => true))

    (fact "missing guest password"
        (s/validate-system (dissoc-in* redis-vc-spec [:machine :password])) => 
          (throws ExceptionInfo (with-m? {:machine {:password '("must be present")}}))
        (provided (p/user-exists? "admin")  => true) 
        (provided (p/type-exists? "redis")  => true))

    (fact "names aren't a vec"
        (s/validate-system (assoc-in redis-vc-spec [:machine :names] "bla")) => 
          (throws ExceptionInfo (with-m? {:machine {:names '("must be a vector")}}))
        (provided (p/user-exists? "admin")  => true) 
        (provided (p/type-exists? "redis")  => true))
    ))



