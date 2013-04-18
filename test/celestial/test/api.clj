(ns celestial.test.api
  "These scenarios describe how the API works and mainly validates routing"
  (:refer-clojure :exclude [type])
  (:use 
    midje.sweet
    [clojure.core.strint :only (<<)]
    [celestial.common :only (slurp-edn)]
    [celestial.fixtures :only (redis-prox-spec redis-type)]
    ring.mock.request
    [celestial.api :only (app)])
  (:require 
    [celestial.jobs :as jobs]
    [celestial.persistency :as p]))

; TODO is seems that ring-mock isn't working correctly with '&' distructuing 
#_(def host-req 
    (merge {:params redis-prox-spec}
           (header (request :post "/registry/host") "Content-type" "application/edn")))

#_(scenario
    (expect {:status 200} (in (non-sec-app (request :post "/registry/host" host-req)))) 
    (expect (interaction (p/register-host redis-prox-spec))))

(def non-sec-app (app false))

(fact "host get"
      (non-sec-app (request :get (<< "/host/machine/redis-local"))) => (contains {:status 200}) 
      (provided (p/host "redis-local") => "foo"))

(fact "getting host type"
      (non-sec-app 
        (header 
          (request :get (<< "/host/type/redis-local")) "accept" "application/json")) => (contains {:status 200})
      (provided 
        (p/fuzzy-host "redis-local") => {:type "redis"} 
        (p/type-of "redis") => {:classes {:redis {:append true}}}))

#_(def type-req
    (merge {:params (slurp-edn "fixtures/redis-type.edn")}
           (header (request :post "/type") "Content-type" "application/edn")))

; TODO is seems that ring-mock isn't working correctly with '&' distructuing 
#_(fact "type requests"
        (non-sec-app type-req) => {:status 200}
        (provided 
          (p/new-type "redis" (dissoc (slurp-edn "fixtures/redis-type.edn") :type)) => nil
          ))

(let [machine {:type "redis" :machine {:host "foo"}} type {:classes {:redis {}}}]
  (fact "provisioning job"
        (non-sec-app (request :post "/job/provision/redis-local")) => (contains {:status 200})
        (provided 
          (p/host "redis-local")  => machine
          (p/type-of "redis") => type
          (jobs/enqueue "provision" 
                        {:identity "redis-local" :args [type machine]}) => nil)))

(fact "staging job" 
      (non-sec-app (request :post "/job/stage/redis-local")) => (contains {:status 200})
      (provided
        (p/host "redis-local") => "p/host result"
        (jobs/enqueue "stage" {:identity "redis-local" :args ["p/host result"]}) => nil))

(fact "creation job"
      (non-sec-app (request :post "/job/create/redis-local"))  => (contains {:status 200})
      (provided 
        (p/host "redis-local")  => "p/host result"
        (jobs/enqueue "machine" {:identity "redis-local" :args ["p/host result"]}) => nil))
