(ns celestial.test.jobs
  (:require [celestial.jobs :as jobs])
  (:use 
    midje.sweet
    [celestial.common :only (minute)]
    [clojure.core.strint :only (<<)]
    [celestial.jobs :only (initialize-workers workers job-exec create-wks enqueue)]
    [celestial.redis :only (with-lock)])
  (:import java.lang.AssertionError))


(with-state-changes [(before :facts (reset! jobs/jobs {:machine [identity 2]}))] 
  (fact "workers creation" :integration :redis
     (initialize-workers)
     (keys @workers) => (just :machine)
    ))

(fact "with-lock used if :identity key was provided" 
   (job-exec identity {:identity "red1" :args {:machine {:hostname "red1"}}}) => nil
   (provided 
     (with-lock "red1" anything {:expiry (* minute 30) :wait-time (* minute 5)}) => nil :times 1))


(fact "enqueue to workless queue should fail"
     (enqueue "foobar" {}) => (throws AssertionError))

