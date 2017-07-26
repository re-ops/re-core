(ns re-core.integration.es.jobs
  "Done jobs persistency"
  (:require
   [es.jobs :as jobs]
   [es.common :as es]
   [clojurewerkz.elastisch.query :as q]
   [re-core.fixtures.data :refer (redis-ec2-spec)]
   [re-core.fixtures.core :refer (with-conf)]
   [re-core.fixtures.populate :refer (re-initlize)])
  (:use midje.sweet))

(def job {:tid "" :status :success :identity 1 :args [] :env :dev :queue :stop})

(defn stamp
  "add time start end timestamps"
  [m]
  (merge m {:start (System/currentTimeMillis) :end (+ 1000 (System/currentTimeMillis))}))

(defn add-jobs
  "adds a list of systems into ES"
  []
  (es/initialize {:indices.ttl.interval 2})
  (jobs/put (-> job (merge {:tid "1" :status :success}) stamp) 2000)
  (jobs/put (-> job (merge {:tid "2" :status :failure :env :prod}) stamp) 2000)
  (jobs/put (-> job (merge {:tid "3" :status :success :identity 2}) stamp) 2000)
  (jobs/put (-> job (merge {:tid "4" :status :failure}) stamp) 2000 :flush? true))

(with-conf
  (against-background [(before :facts (do (re-initlize true) (add-jobs)))]
                      (fact "basic job get" :integration :elasticsearch
                            (get-in (jobs/get "1") [:source :status]) => "success"
                            (get-in (jobs/get "2") [:source :env]) => "prod"
                            (get-in (jobs/get "3") [:source :identity]) => 2)

                      (fact "jobs pagination" :integration :elasticsearch
                            (:total (jobs/paginate 0 5 [:dev])) => 3
                            (:total (jobs/paginate 0 5 [:prod])) => 1
                            (:total (jobs/paginate 0 5 [:prod :dev])) => 4)

                      (fact "expiry (ttl)" :integration :ttl
                            (jobs/put (-> job (merge {:tid "foo" :status :success}) stamp) 100 :flush? true)
                            (Thread/sleep 2000)
                            (jobs/get "foo") => nil)))
