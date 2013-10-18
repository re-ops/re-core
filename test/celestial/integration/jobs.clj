(ns celestial.integration.jobs
  (:require 
    [celestial.fixtures.core :refer [with-conf]]  
    [celestial.jobs :as jobs :refer (initialize-workers workers)])
  (:use midje.sweet))

(with-conf 
  (with-state-changes [(before :facts (reset! jobs/jobs {:machine [identity 2]}))] 
    (fact "workers creation" :integration :redis
       (initialize-workers)
       (keys @workers) => (just :machine))))
