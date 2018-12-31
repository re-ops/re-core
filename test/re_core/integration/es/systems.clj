(ns re-core.integration.es.systems
  "Testing system searching"
  (:require
   [re-core.log :refer (setup-logging)]
   [es.common :as es :refer (types)]
   [es.systems :as s]
   [rubber.node :refer (stop)]
   [re-share.components.elastic :as esc]
   [re-core.fixtures.data :refer (redis-kvm-spec redis-ec2-spec)]
   [re-core.fixtures.populate :refer (re-initlize)])
  (:use clojure.test))

(setup-logging)

(defn puts
  "adds a list of systems into ES" []
  (esc/initialize :re-core types false)
  (s/put "1" (assoc redis-kvm-spec :owner "admin"))
  (s/put "2" (assoc-in redis-ec2-spec [:machine :hostname] "foo-1"))
  (s/put "3" (assoc redis-ec2-spec :env :prod-1))
  (s/put "4" redis-kvm-spec)
  (Thread/sleep 1000))

(defn total [res] (get-in res [:hits :total]))

(defn setup [f]
  (re-initlize true)
  (puts)
  (f)
  (stop))

(deftest basic-put- get
  (is (= (get (s/get "1") :env) :dev)))

(deftest term-searching
  (is (= (total (s/query {:bool {:must {:match {:machine.cpu 4}}}})) 2)))

(deftest pagination
  (let [query {:bool {:must {:term {:machine.cpu 4}}}}
        {:keys [hits]} (s/query query :size 2 :from 1)]
    (is (= (-> hits :hits count) 1))))

(deftest aws
  (let [query {:wildcard {:aws.endpoint "*"}} {:keys [hits]} (s/query query :size 2 :from 0)]
    (is (= (-> hits :hits count) 2))))

(use-fixtures :each setup)
