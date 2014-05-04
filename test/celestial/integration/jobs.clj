(ns celestial.integration.jobs
  (:require 
    [celestial.model :refer (operations)]
    [celestial.fixtures.core :refer (with-conf)]  
    [celestial.jobs :refer (jobs initialize-workers workers)])
  (:use midje.sweet))

(with-conf 
  (with-state-changes [(before :facts (reset! workers {}))]
    (fact "workers creation" :integration :redis
      (initialize-workers) => nil
      (provided 
        (jobs) => {:machine [identity 2]})
      (keys @workers) => (just :machine))))
