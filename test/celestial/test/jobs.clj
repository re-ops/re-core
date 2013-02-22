(ns celestial.test.jobs
  (:require [celestial.jobs :as jobs])
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.jobs :only (initialize-workers workers job-exec)]
    [celestial.redis :only (create-worker with-lock)]
    expectations.scenarios
    )
  (:import java.lang.AssertionError)
 )

(scenario 
  (with-redefs [jobs/jobs {:machine identity}] 
    (stubbing [create-worker :worker]
       (initialize-workers) 
       (expect :machine (in (keys @workers)))
       (expect :worker (in (vals @workers))))))

(scenario 
  (expect AssertionError (job-exec identity {:machine {:host nil}})) 
  (job-exec identity {:machine {:host "red1"}})
  (expect (interaction (with-lock "red1" anything))))
