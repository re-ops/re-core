(ns re-core.test.config
  (:require
   [re-core.fixtures.data :refer (local-prox)]
   [flatland.useful.map :refer (dissoc-in*)]
   [re-core.config :refer (validate-conf)])
  (:use midje.sweet))

(defn validate-missing [& ks]
  (validate-conf (dissoc-in* @local-prox ks)))

(fact "legal configuration"
      (validate-conf @local-prox)  => {})

(fact "missing re-core options detected"
      (validate-missing :re-core :https-port) =>  {:re-core {:https-port "must be present"}}
      (validate-missing :re-core :port) => {:re-core {:port "must be present"}})

(fact "missing aws options"
      (validate-conf (assoc-in @local-prox [:hypervisor :dev :aws] {})) => {:hypervisor {:dev {:aws {:access-key "must be present" :secret-key "must be present"
                                                                                                     :default-vpc {:subnet-id "must be present", :vpc-id "must be present" :assign-public "must be present"}}}}})
