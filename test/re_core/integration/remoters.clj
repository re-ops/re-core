(ns re-core.integration.remoters
  "Capistrano remoter provider see https://github.com/narkisr/cap-demo and fixtures/cap-deploy.edn"
  (:require
    [re-core.persistency.types :as t]
    [re-core.persistency.systems :as s]
    [re-core.fixtures.data :refer (redis-deploy redis-runall redis-kvm-spec redis-type)]
    [re-core.fixtures.core :refer (with-defaults)]
    [me.raynes.fs :refer (exists?)]
    [re-core.integration.workflows.common :refer (spec get-spec)]
    [re-core.fixtures.populate :refer (populate-system)]
    [re-core.workflows :refer (reload destroy)]
    [re-core.model :refer (rconstruct)]
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
