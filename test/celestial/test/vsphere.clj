(ns celestial.test.vsphere
  (:require vsphere.provider)
  (:use 
    midje.sweet
    [clojure.core.strint :only (<<)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [celestial.fixtures :only (redis-vsphere-spec with-conf with-m?)])
  (:import clojure.lang.ExceptionInfo) 
  )

(with-conf
  (do 
    (fact "basic construction"
      (let [vm (vconstruct redis-vsphere-spec)]
        (:allocation vm ) => {:datacenter "playground" :pool "" :disk-format :sparse}
        (:machine vm ) => {:cpus 1 :hostname "red1" :memory 512 :template "ubuntu-13.04_puppet-3.1-with-tools"}
        (:guest vm)  => {:user "ronen" :password "foobar"}))
    (fact "missing datacenter"
          (vconstruct (assoc-in redis-vsphere-spec [:vsphere :datacenter] nil)) => 
          (throws ExceptionInfo (with-m? {:allocation {:datacenter '("datacenter must be present")}})))
    (fact "wrong disk format"
          (vconstruct (assoc-in redis-vsphere-spec [:vsphere :disk-format] :full)) => 
          (throws ExceptionInfo (with-m? {:allocation {:disk-format '("disk format must be either #{:flat :sparse}")}})))))
