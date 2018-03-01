(ns re-core.integration.es.jobs
  "Done jobs persistency"
  (:require
   [es.jobs :as jobs]
   [es.common :as es]
   [re-share.es.node :refer (stop)]
   [re-core.fixtures.data :refer (redis-ec2-spec)]
   [re-core.fixtures.core :refer (with-conf)]
   [re-core.fixtures.populate :refer (re-initlize)])
  (:use midje.sweet))

(def job {:tid "" :status :success :identity 1 :args [] :env :dev :topic :stop})

(defn stamp
  "add time start end timestamps"
  [m]
  (merge m {:start (System/currentTimeMillis) :end (+ 1000 (System/currentTimeMillis))}))

(defn add-jobs
  "adds a list of systems into ES"
  []
  (es/initialize (es/index))
  (jobs/put (-> job (merge {:tid "1" :status :success}) stamp))
  (jobs/put (-> job (merge {:tid "2" :status :failure :env :prod}) stamp))
  (jobs/put (-> job (merge {:tid "3" :status :success :identity 2}) stamp))
  (jobs/put (-> job (merge {:tid "4" :status :failure}) stamp)))

(with-conf
  (against-background [(before :facts (do (re-initlize true) (add-jobs))) (after :facts (stop))]
                      (fact "basic job get" :integration :elasticsearch
                            (:status (jobs/get "1")) => "success"
                            (:env (jobs/get "2")) => "prod"
                            (:identity (jobs/get "3")) => 2)))
