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
    (expect (interaction (p/add-system redis-prox-spec))))

(def non-sec-app (app false))

(fact "system get"
      (non-sec-app (request :get (<< "/host/system/1"))) => (contains {:status 200}) 
      (provided (p/get-system "1") => "foo"))

(fact "getting host type"
      (non-sec-app 
        (header 
          (request :get (<< "/host/type/1")) "accept" "application/json")) => (contains {:status 200})
      (provided 
        (p/get-system "1") => {:type "redis"} 
        (p/get-type "redis") => {:classes {:redis {:append true}}}))

#_(def type-req
    (merge {:params (slurp-edn "fixtures/redis-type.edn")}
           (header (request :post "/type") "Content-type" "application/edn")))

; TODO is seems that ring-mock isn't working correctly with '&' distructuing 
#_(fact "type requests"
        (non-sec-app type-req) => {:status 200}
        (provided 
          (p/add-type (slurp-edn "fixtures/redis-type.edn") :type) => nil
          ))

(let [machine {:type "redis" :machine {:host "foo"}} type {:classes {:redis {}}}]
    (fact "provisioning job"
          (non-sec-app (request :post "/job/provision/1")) => (contains {:status 200})
          (provided 
            (p/get-system "1")  => machine
            (p/get-type "redis") => type
            (jobs/enqueue "provision" {:identity "1" :args [type (assoc machine :system-id 1)]}) => nil)))

(fact "staging job" 
      (non-sec-app (request :post "/job/stage/1")) => (contains {:status 200})
      (provided
        (p/get-system "1") => {}
        (jobs/enqueue "stage" {:identity "1" :args [{:system-id 1}]}) => nil))

(fact "creation job"
      (non-sec-app (request :post "/job/create/1"))  => (contains {:status 200})
      (provided 
        (p/system-exists? "1") => true
        (p/get-system "1")  => {}
        (jobs/enqueue "reload" {:identity "1" :args [{:system-id 1}]}) => nil))
