(ns celestial.test.jobs
  (:require [celestial.jobs :as jobs])
  (:use 
    midje.sweet
    [celestial.common :only (half-hour minute)]
    [clojure.core.strint :only (<<)]
    [celestial.jobs :only (initialize-workers workers job-exec create-wks )]
    [celestial.redis :only (with-lock)]
    ))


(with-state-changes [(before :facts (reset! jobs/jobs {:machine [identity 2]}))] 
  (fact "workers creation" :integration :redis
     (initialize-workers)
     (keys @workers) => (just :machine)
    ))

(fact "with-lock used if :identity key was provided" 
      (job-exec identity {:identity "red1" :args {:machine {:hostname "red1"}}}) => nil
      (provided 
        (with-lock "red1" anything {:expiry half-hour :wait-time minute}) => nil :times 1))
