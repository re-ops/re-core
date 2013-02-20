(ns celestial.test.api
  (:use 
    [clojure.core.strint :only (<<)]
    ring.mock.request
    clojure.test
    expectations.scenarios 
    [celestial.api :only (app-routes)]
    )
  (:require 
    [celestial.persistency :as p]) 
  )

(deftest register-host
  (is (= (app-routes (request :post "/registry/host/redis.local/redis" ))
         {:status 200
          :headers {"content-type" "text/plain"}
          :body "Your expected result"})))

(scenario
  (let [h "redis-1" t "redis" m {:machine {:cpu 1}}]
    (app-routes (request :post "/registry/host/"  (assoc (assoc m :type t) :host h))) 
    (expect (interaction (p/register-host h t m)))))

(app-routes (request :post "/registry/host"  {:host "zoo" :t 1  :machine {:cpu 1} }))
;(app-routes (request :post "/registry/type/redis" {:foo 1}))
