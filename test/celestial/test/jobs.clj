(ns celestial.test.jobs
  (:require [celestial.jobs :as jobs])
  (:use 
    midje.sweet
    [clojure.core.strint :only (<<)]
    [celestial.jobs :only (initialize-workers workers job-exec create-wks )]
    [celestial.redis :only (with-lock half-hour minute)]
    ))


(with-state-changes [(before :facts (reset! jobs/jobs {:machine [identity 2]}))] 
  (fact "workers creation"
     (initialize-workers)
     (keys @workers) => (just :machine)
     ;; (vals @workers) => (contains [:w1 :w2])
    #_(provided 
      (jobs/create-wks :machine identity 2) => [:w1 :w2])))

(fact "with-lock used if :identity key was provided" 
      (job-exec identity {:identity "red1" :args {:machine {:hostname "red1"}}}) => nil
      (provided 
        (with-lock "red1" anything {:expiry half-hour :wait-time minute}) => nil :times 1))
