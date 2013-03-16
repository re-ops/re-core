(ns celestial.test.api
  "These scenarios describe how the API works and mainly validates routing"
  (:refer-clojure :exclude [type])
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.common :only (slurp-edn)]
    [celestial.fixtures :only (redis-prox-spec redis-type)]
    ring.mock.request
    expectations.scenarios 
    [celestial.api :only (app)])
  (:require 
    [celestial.jobs :as jobs]
    [celestial.persistency :as p]))

; TODO is seems that ring-mock isn't working correctly with '&' distructuing 
#_(def host-req 
  (merge {:params redis-prox-spec}
         (header (request :post "/registry/host") "Content-type" "application/edn")))

#_(scenario
    (expect {:status 200} (in (app (request :post "/registry/host" host-req)))) 
    (expect (interaction (p/register-host redis-prox-spec))))

(scenario
  (expect {:status 200} (in (app (request :get (<< "/registry/host/machine/redis-local"))))) 
  (expect (interaction (p/host "redis-local"))))

(scenario
  (stubbing [p/fuzzy-host {:type "redis"} p/type-of {:classes {:redis {:append true}}}]
     (expect {:status 200} 
         (in (app (header (request :get (<< "/registry/host/type/redis-local")) "accept"  "application/json")))) 
            ))

(def type-req
  (merge {:params (slurp-edn "fixtures/redis-type.edn")}
         (header (request :post "/registry/type") "Content-type" "application/edn")))

(scenario
  (expect {:status 200} (in (app type-req))) 
  (expect 
    (interaction 
      (p/new-type "redis" (dissoc (slurp-edn "fixtures/redis-type.edn") :type)))))

(scenario 
  (let [machine {:type "redis" :machine {:host "foo"}} type {:classes {:redis {}}}]
    (stubbing [p/host machine  p/type-of type]
        (expect {:status 200} (in (app (request :post "/provision/redis-local")))) 
        (expect (interaction (jobs/enqueue "provision" {:identity "redis-local" :args [type machine]}))))))

(scenario 
  (expect {:status 200} (in (app (request :post "/stage/redis-local")))) 
  (expect (interaction (p/host "redis-local")))  
  (expect (interaction (jobs/enqueue "stage" {:identity "redis-local" :args ["p/host result"]}))))

(scenario 
  (expect {:status 200} (in (app (request :post "/machine/redis-local")))) 
  (expect (interaction (p/host "redis-local")))  
  (expect (interaction (jobs/enqueue "machine" {:identity "redis-local" :args ["p/host result"]}))))
