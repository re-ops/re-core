(ns celestial.integration.workflows.docker
  "Docker workflows"
  (:require 
    [celestial.fixtures.core :refer (with-defaults is-type? with-admin with-conf)]  
    [celestial.fixtures.data :refer 
     (redis-type local-conf redis-docker-spec)]
    [celestial.persistency.systems :as s]
    [celestial.fixtures.populate :refer (populate-system)]  
    [celestial.integration.workflows.common :refer (spec get-spec)]
    [celestial.workflows :as wf])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet)
 )

(with-admin
 (with-conf local-conf
  (with-state-changes [(before :facts (populate-system redis-type redis-docker-spec))]
    (fact "docker creation workflows" :integration :docker :workflow
       (wf/create (spec)) => nil 
       (get-in (spec) [:docker :container-id]) => (comp not nil?)
       (wf/create (spec)) => (throws ExceptionInfo  (is-type? :celestial.workflows/machine-exists)) 
       (wf/stop (spec)) => nil 
       (wf/destroy (spec)) => nil 
      )

    (fact "docker start/stop workflows" :integration :docker :workflow
       (wf/create (spec)) => nil 
       (wf/stop (spec)) => nil 
       (let [{:keys [docker]} (spec)]
         (wf/start (spec)) => nil
         (get-in (spec) [:docker :container-id]) => (docker :container-id)
         ) 
       (wf/destroy (spec)) => nil 
      )

   (fact "docker reload workflows" :integration :docker :workflow
        (wf/create (spec)) => nil
        (let [{:keys [docker]} (spec)]
          (wf/reload (spec)) => nil 
          (get-in (spec) [:docker :container-id]) =not=> (docker :container-id))
        (wf/destroy (spec)) => nil)

   (fact "docker clone workflows" :integration :docker :workflow
        (wf/create (spec)) => nil
        (wf/clone 1 {:hostname "bar" :owner "ronen" :docker {:port-bindings ["22/tcp:2223/0.0.0.0"]}}) => nil
        (wf/destroy (assoc (s/get-system 2) :system-id 2)) => nil
        (wf/destroy (spec)) => nil))) 
) 
