(ns celestial.test.config
  (:require 
    [celestial.fixtures.data :refer (local-prox)]
    [flatland.useful.map :refer (dissoc-in*)]
    [celestial.config :refer (validate-conf)])
  (:use midje.sweet))

(defn validate-missing [& ks]
  (validate-conf (dissoc-in* local-prox ks)))

(fact "legal configuration"
      (validate-conf local-prox)  => {})

(fact "missing celestial options detected"
      (validate-missing :celestial :https-port) =>  {:celestial {:https-port "must be present"}}  
      (validate-missing :celestial :port) => {:celestial {:port "must be present"}})


(fact "missing aws options"
    (validate-conf (assoc-in local-prox [:hypervisor :dev :aws] {})) => 
      {:hypervisor {:dev {:aws {:access-key "must be present" :secret-key "must be present"}}}})

(fact "missing openstack options"
    (validate-conf (assoc-in local-prox [:hypervisor :dev :openstack] {})) => 
      {:hypervisor 
        {:dev 
          {:openstack {
            :username "must be present" :password "must be present" 
            :endpoint "must be present" :managment-interface "must be present"}}}})

(fact "wrong central logging"
  (validate-conf (assoc-in local-prox [:celestial :log :gelf :type] :foo)) =>
    {:celestial {:log {:gelf 
      {:type "type must be either #{:kibana3 :graylog2 :logstash :kibana4}"}}}})

(fact "workers configuration"
  (validate-conf (assoc-in local-prox [:celestial :job :workers :stage] "bar")) =>
     {:celestial {:job {:workers  {:stage "must be a integer"}}}})
