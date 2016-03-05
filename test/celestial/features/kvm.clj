(ns celestial.features.kvm
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.model :refer (vconstruct)]
    [celestial.fixtures.core :refer (with-conf with-admin is-type?) :as f]
    [celestial.fixtures.data :refer (redis-type local-conf redis-gce)]  
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec)]
    [celestial.workflows :as wf]
    [celestial.fixtures.data :refer [redis-kvm]])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo)
 )

(with-conf
  (let [{:keys [machine kvm]} redis-gce]
    (fact "legal instance spec" :kvm
      (let [domain (vconstruct (assoc redis-kvm :system-id 1))]
        (:system-id domain ) => 1
        (:node domain)  => (just {:username "ronen" :host "localhost" :ssh-port 22})
        (:domain domain) => 
          (just {
            :user "celestial" :name "red1.local" 
            :image {:flavor :debian :template "/home/ronen/images/ubuntu-1504-server.img"}
           })))

    #_(fact "legal instance gce" :kvm
      (:gce (vconstruct redis-gce)) => 
         (contains {
           :name "red1"
           :machineType machine-type
           :disks [{
              :initializeParams {:sourceImage source-image} :autoDelete true
              :type "PERSISTENT" :boot true 
            }]
          })

         )))
