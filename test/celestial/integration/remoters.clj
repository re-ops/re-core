(ns celestial.integration.remoters
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require
    [celestial.persistency.types :as t]
    [celestial.persistency.systems :as s]
    [celestial.fixtures.data :refer (redis-deploy redis-runall redis-kvm-spec redis-type)]
    [celestial.fixtures.core :refer (with-defaults)]
    [me.raynes.fs :refer (exists?)]
    [celestial.integration.workflows.common :refer (spec get-spec)]
    [celestial.fixtures.populate :refer (populate-system)]
    [celestial.workflows :refer (reload destroy)]
    [celestial.model :refer (rconstruct)]
    remote.capistrano)
  (:use midje.sweet))

(with-defaults
  (with-state-changes [(before :facts (populate-system redis-type redis-kvm-spec))]
    (fact "basic deploy" :integration :capistrano
      (let [cap (rconstruct redis-deploy {:target "192.168.3.200"})]
         (reload (spec))
         (.setup cap)
         (exists? (:dst cap)) => truthy
         (.run cap)
         (.cleanup cap)
         (destroy (spec)) => nil
         (exists? (:dst cap))  => falsey
         )))

   (fact "ruby runall" :integration :ruby :capistrano
      (let [cap (rconstruct redis-runall {:target "192.168.3.200"}) ]
         (reload (spec))
         (.setup cap)
         (exists? (:dst cap)) => truthy
         (.run cap)
         (.cleanup cap)
         (destroy (spec)) => nil
         (exists? (:dst cap))  => falsey
         )))
