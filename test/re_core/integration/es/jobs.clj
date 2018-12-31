(ns re-core.integration.es.jobs
  "Done jobs persistency"
  (:require
   [es.jobs :as jobs]
   [es.common :as es :refer (types)]
   [re-share.components.elastic :as esc]
   [rubber.node :refer (stop)]
   [re-core.fixtures.populate :refer (re-initlize)])
  (:use clojure.test))

(def job {:tid "" :status :success :identity 1 :args [] :env :dev :topic :stop})

(defn stamp
  "add time start end timestamps"
  [m]
  (merge m {:start (System/currentTimeMillis) :end (+ 1000 (System/currentTimeMillis))}))

(defn add-jobs
  "adds a list of systems into ES"
  []
  (esc/initialize :re-core types false)
  (jobs/put (-> job (merge {:tid "1" :status :success}) stamp))
  (jobs/put (-> job (merge {:tid "2" :status :failure :env :prod}) stamp))
  (jobs/put (-> job (merge {:tid "3" :status :success :identity 2}) stamp))
  (jobs/put (-> job (merge {:tid "4" :status :failure}) stamp)))

(defn setup [f]
  (re-initlize true)
  (add-jobs)
  (f)
  (stop))

(deftest jobs-persistency
  (is (= (:status (jobs/get "1")) "success"))
  (is (= (:env (jobs/get "2")) "prod"))
  (is (= (:identity (jobs/get "3")) 2)))

(use-fixtures :once setup)
