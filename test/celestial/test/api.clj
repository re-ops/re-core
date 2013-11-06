(ns celestial.test.api
  "These scenarios describe how the API works and mainly validates routing"
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.fixtures.data :as d]
    [celestial.api.users :refer (into-persisted)]
    [celestial.jobs :as jobs]
    [celestial.persistency.systems :as s] 
    [clojure.core.strint :refer (<<)]
    [cemerick.friend :as friend]
    [celestial.roles :as roles]
    [celestial.persistency :as p])
  (:use 
    midje.sweet ring.mock.request
    [celestial.api :only (app)]))

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

(let [machine {:type "redis" :machine {:host "foo"}} type {:classes {:redis {}}}]
    (fact "provisioning job"
          (non-sec-app (request :post "/jobs/provision/1")) => (contains {:status 200})
          (provided 
            (s/system-exists? "1") => true
            (s/get-system "1")  => machine
            (s/get-system "1" :env)  => :dev
            (p/get-type "redis") => type
            (jobs/enqueue "provision" {:identity "1" :args [type (assoc machine :system-id 1)] :tid nil :env :dev :user nil}) => nil)))

(fact "staging job" 
      (non-sec-app (request :post "/jobs/stage/1")) => (contains {:status 200})
      (provided
        (s/system-exists? "1") => true
        (p/get-type "redis") => {:puppet-module "bar"}
        (s/get-system "1") => {:type "redis"}
        (s/get-system "1" :env)  => :dev
        (jobs/enqueue "stage" {:identity "1" :args [{:puppet-module "bar"} {:system-id 1 :type "redis"}] :tid nil :env :dev :user nil}) => nil))

(fact "creation job"
      (non-sec-app (request :post "/jobs/create/1"))  => (contains {:status 200})
      (provided 
        (s/system-exists? "1") => true
        (s/get-system "1")  => {}
        (s/get-system "1" :env)  => :dev
        (jobs/enqueue "create" {:identity "1" :args [{:system-id 1}] :tid nil :env :dev :user nil}) => nil))

(let [user (merge d/admin {:roles ["admin"] :envs ["dev" "qa"]})]
  (fact "user conversion"
     (dissoc (into-persisted user) :password) => 
       {:envs [:dev :qa], :roles #{:celestial.roles/admin}, :username "admin"}
     (into-persisted (dissoc user :password)) => 
       {:envs [:dev :qa], :roles #{:celestial.roles/admin}, :username "admin"})) 
