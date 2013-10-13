(ns celestial.test.api
  "These scenarios describe how the API works and mainly validates routing"
  (:refer-clojure :exclude [type])
  (:use 
    midje.sweet ring.mock.request
    [clojure.core.strint :only (<<)]
    [celestial.common :only (slurp-edn)]
    [celestial.fixtures :only (redis-prox-spec redis-type)]
    [celestial.api :only (app)])
  (:require 
    [celestial.jobs :as jobs]
    [celestial.persistency.systems :as s] 
    [celestial.persistency :as p]))

; TODO is seems that ring-mock isn't working correctly with '&' distructuing 
#_(def host-req 
    (merge {:params redis-prox-spec}
           (header (request :post "/registry/host") "Content-type" "application/edn")))

(def non-sec-app (app false))

(fact "system get"
      (non-sec-app (request :get (<< "/systems/1"))) => (contains {:status 200}) 
      (provided (s/get-system "1") => "foo"))

(fact "getting host type"
      (non-sec-app 
        (header 
          (request :get (<< "/systems/1/type")) "accept" "application/json")) => (contains {:status 200})
      (provided 
        (s/get-system "1") => {:type "redis"} 
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
          (non-sec-app (request :post "/jobs/provision/1")) => (contains {:status 200})
          (provided 
            (s/system-exists? "1") => true
            (s/get-system "1")  => machine
            (s/get-system "1" :env)  => :dev
            (p/get-type "redis") => type
            (jobs/enqueue "provision" {:identity "1" :args [type (assoc machine :system-id 1)] :tid nil :env :dev}) => nil)))

(fact "staging job" 
      (non-sec-app (request :post "/jobs/stage/1")) => (contains {:status 200})
      (provided
        (s/system-exists? "1") => true
        (p/get-type "redis") => {:puppet-module "bar"}
        (s/get-system "1") => {:type "redis"}
        (s/get-system "1" :env)  => :dev
        (jobs/enqueue "stage" {:identity "1" :args [{:puppet-module "bar"} {:system-id 1 :type "redis"}] :tid nil :env :dev}) => nil))

(fact "creation job"
      (non-sec-app (request :post "/jobs/create/1"))  => (contains {:status 200})
      (provided 
        (s/system-exists? "1") => true
        (s/get-system "1")  => {}
        (s/get-system "1" :env)  => :dev
        (jobs/enqueue "reload" {:identity "1" :args [{:system-id 1}] :tid nil :env :dev}) => nil))
