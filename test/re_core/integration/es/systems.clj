(ns re-core.integration.es.systems
  "Testing system searching"
  (:require
   [re-core.log :refer (setup-logging)]
   [es.common :as es]
   [es.systems :as s]
   [re-share.es.node :refer (stop)]
   [re-core.fixtures.data :refer (redis-kvm-spec redis-ec2-spec)]
   [re-core.fixtures.core :refer (with-conf)]
   [re-core.fixtures.populate :refer (re-initlize)])
  (:use midje.sweet))

(setup-logging)

(defn puts
  "adds a list of systems into ES" []
  (es/initialize (es/index))
  (s/put "1" (assoc redis-kvm-spec :owner "admin"))
  (s/put "2" (assoc-in redis-ec2-spec [:machine :hostname] "foo-1"))
  (s/put "3" (assoc redis-ec2-spec :env :prod-1))
  (s/put "4" redis-kvm-spec)
  (Thread/sleep 1000))

(defn total [res] (get-in res [:hits :total]))

(with-conf
  (against-background [(before :facts (do (re-initlize true) (puts))) (after :facts (stop))]
                      (fact "basic system put and get" :integration :elasticsearch
                            (get (s/get "1") :env) => :dev)

                      (fact "term searching" :integration :elasticsearch
                            (total (s/query {:bool {:must {:match {:machine.cpu 4}}}})) => 2)

                      (fact "pagination" :integration :elasticsearch
                            (let [query {:bool {:must {:term {:machine.cpu 4}}}}
                                  {:keys [hits]} (s/query query :size 2 :from 1)]
                              (-> hits :hits count) => 1))

                      (fact "aws" :integration :elasticsearch
                            (let [query {:wildcard {:aws.endpoint "*"}} {:keys [hits]} (s/query query :size 2 :from 0)]
                              (-> hits :hits count) => 2))))

