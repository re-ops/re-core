(ns celestial.integration.es.systems
  "Testing system searching"
  (:require 
    [es.node :as es]
    [es.systems :as sys]
    [clojurewerkz.elastisch.query :as q]
    [celestial.fixtures.data :refer (redis-prox-spec redis-ec2-spec)]
    )
  (:use midje.sweet))

(defn add-systems 
   "adds a list of systems into ES" 
   []
  (sys/put "1" redis-prox-spec)        
  (sys/put "2" redis-ec2-spec)        
  (sys/put "3" redis-prox-spec))

(with-state-changes [(before :facts (do (es/start-n-connect) (sys/initialize) (add-systems)))
                     (after :facts (es/stop))]
  (fact "basic system put and get" :integration :elasticsearch
    (get-in (sys/get "1") [:source :env]) => ":dev")
  
  (fact "term searching" :integration :elasticsearch
    (get-in (sys/query {:bool {:must {:term {"machine.cpus" "4" }}}}) [:hits :total]) => 2)

  (fact "pagination" :integration :elasticsearch
    (let [query {:bool {:must {:term {"machine.cpus" "4" }}}}
          {:keys [hits]} (sys/query query :size 2 :from 1)]
       (-> hits :hits count) => 1
      ) 
    )
  )
