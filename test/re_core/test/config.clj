(ns re-core.test.config
  (:require
    [re-core.fixtures.data :refer (local-prox)]
    [flatland.useful.map :refer (dissoc-in*)]
    [re-core.config :refer (validate-conf)])
  (:use midje.sweet))

(defn validate-missing [& ks]
  (validate-conf (dissoc-in* local-prox ks)))

(fact "legal configuration"
      (validate-conf local-prox)  => {})

(fact "missing re-core options detected"
      (validate-missing :re-core :https-port) =>  {:re-core {:https-port "must be present"}}
      (validate-missing :re-core :port) => {:re-core {:port "must be present"}})

(fact "missing aws options"
    (validate-conf (assoc-in local-prox [:hypervisor :dev :aws] {})) => {
       :hypervisor {
         :dev {
           :aws {
             :access-key "must be present" :secret-key "must be present"
             :default-vpc {
               :subnet-id "must be present", :vpc-id "must be present" :assign-public "must be present" 
               }}}}})

(fact "wrong central logging"
  (validate-conf (assoc-in local-prox [:re-core :log :gelf :type] :foo)) =>
    {:re-core {:log {:gelf
      {:type "type must be either #{:kibana3 :graylog2 :logstash :kibana4}"}}}})

(fact "workers configuration"
  (validate-conf (assoc-in local-prox [:re-core :job :workers :stage] "bar")) =>
     {:re-core {:job {:workers  {:stage "must be a integer"}}}})
