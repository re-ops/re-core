(ns celestial.test.api
  (:refer-clojure :exclude [type])
  (:use 
    [clojure.core.strint :only (<<)]
    ring.mock.request
    clojure.test
    expectations.scenarios 
    [celestial.api :only (app)]
    )
  (:require 
    [celestial.persistency :as p]) 
  )

(def host-req 
  (merge {:params {:machine {:cpu 1} :host "redis-local" :type "redis"}}
    (header (request :post "/registry/host") "Content-type" "application/edn")))

(def host
    )

(scenario
  (let [{:keys [machine host type]} host-req]
    (app (request :post "/registry/host" host-req)) 
    (expect (interaction (p/register-host host type machine)))))

(scenario
  (app (request :get (<< "/registry/host/redis-local"))) 
  (expect (interaction (p/host "redis-local"))))
;(app-routes (request :post "/registry/type/redis" {:foo 1}))


