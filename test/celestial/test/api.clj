(ns celestial.test.api
  (:refer-clojure :exclude [type])
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.common :only (slurp-edn)]
    ring.mock.request
    clojure.test
    expectations.scenarios 
    [celestial.api :only (app)])
  (:require 
    [celestial.persistency :as p]))

(def host-req 
  (merge {:params (slurp-edn "fixtures/redis-system.edn")}
         (header (request :post "/registry/host") "Content-type" "application/edn")))

(def type-req
  (merge {:params (slurp-edn "fixtures/redis-type.edn")}
         (header (request :post "/registry/type") "Content-type" "application/edn")))

(scenario
  (let [{:keys [machine host type]} host-req]
    (app (request :post "/registry/host" host-req)) 
    (expect (interaction (p/register-host host type machine)))))

(scenario
  (app (request :get (<< "/registry/host/redis-local"))) 
  (expect (interaction (p/host "redis-local"))))

(scenario
    (app type-req) 
    (expect (interaction (p/new-type "redis"  (:classes (select-keys (slurp-edn "fixtures/redis-type.edn") [:classes]))))))

