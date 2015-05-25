(ns celestial.test.config
  (:require 
    [celestial.fixtures.data :refer (local-prox)]
    [flatland.useful.map :refer (dissoc-in*)]
    [celestial.config :refer (validate-conf proxmox-v)])
  (:use midje.sweet))

(defn validate-missing [& ks]
  (validate-conf (dissoc-in* local-prox ks)))

(fact "legal configuration"
      (validate-conf local-prox)  => {})

(fact "missing celestial options detected"
      (validate-missing :celestial :https-port) =>  {:celestial {:https-port "must be present"}}  
      (validate-missing :celestial :port) => {:celestial {:port "must be present"}})

(fact "missing proxmox options"
    (validate-missing :hypervisor :dev :proxmox :nodes :proxmox-a :password) =>  
      '{:hypervisor {:dev {:proxmox {:nodes ({:proxmox-a {:password "must be present"}})}}}}
     
    (validate-missing :hypervisor :dev :proxmox :nodes) => {:hypervisor {:dev {:proxmox {:nodes "must be present"}}}}
    (validate-missing :hypervisor :dev :proxmox :master) => {:hypervisor {:dev {:proxmox {:master "must be present"}}}}
      )

(fact "non legal proxmox template flavor" 
  (validate-conf (assoc-in local-prox [:hypervisor :dev :proxmox :ostemplates :ubuntu-12.04 :flavor] :bar)) => 
     '{:hypervisor {:dev {:proxmox {:ostemplates ({:ubuntu-12.04 {:flavor "flavor must be either #{:redhat :debian}"}})}}}} 
      )

(fact "missing aws options"
    (validate-conf (assoc-in local-prox [:hypervisor :dev :aws] {})) => 
      {:hypervisor {:dev {:aws {:access-key "must be present" :secret-key "must be present"}}}})

(fact "missing openstack  options"
    (validate-conf (assoc-in local-prox [:hypervisor :dev :openstack] {})) => 
      {:hypervisor {:dev {:openstack {:username "must be present" :password "must be present" :endpoint "must be present"}}}})

(fact "vcenter validations" 
   (validate-missing :hypervisor :dev :vcenter :password) => {:hypervisor {:dev {:vcenter {:password "must be present"}}}}
   (validate-missing :hypervisor :dev :vcenter :url) => {:hypervisor {:dev {:vcenter {:url "must be present"}}}}
   (validate-conf (assoc-in local-prox [:hypervisor :dev :vcenter :ostemplates] [])) => 
      {:hypervisor {:dev {:vcenter {:ostemplates "must be a map"}}}}
  )

(fact "wrong central logging"
  (validate-conf (assoc-in local-prox [:celestial :log :gelf :type] :foo)) =>
    {:celestial {:log {:gelf 
      {:type "type must be either #{:kibana3 :graylog2 :logstash :kibana4}"}}}})

(fact "docker sanity"
   (validate-conf (assoc-in local-prox [:hypervisor :dev :docker :nodes] nil)) =>
       {:hypervisor {:dev {:docker {:nodes "must be present"}}}}
   (validate-missing :hypervisor :dev :docker :nodes :local :host) => 
      {:hypervisor {:dev {:docker {:nodes '({:local {:host "must be present"}})}}}}
  )

(fact "workers configuration"
  (validate-conf (assoc-in local-prox [:celestial :job :workers :stage] "bar")) =>
     {:celestial {:job {:workers  {:stage "must be a integer"}}}})
