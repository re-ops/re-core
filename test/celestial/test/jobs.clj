(ns celestial.test.jobs
  (:require [celestial.jobs :as jobs])
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.jobs :only (initialize-workers workers job-exec create-wks )]
    [celestial.redis :only (with-lock half-hour minute)]
    expectations.scenarios
    )
  (:import java.lang.AssertionError)
 )

(scenario 
  (with-redefs [jobs/jobs {:machine [identity 2]}] 
    (stubbing [create-wks [:w1 :w2]]
       (initialize-workers) 
       (expect :machine (in (keys @workers)))
       (expect [:w1 :w2] (in (vals @workers))))))

(scenario 
  (expect AssertionError (job-exec identity {:machine {:host nil}})) 
  (job-exec identity {:machine {:hostname "red1"}})
  (expect (interaction (with-lock "red1" anything {:expiry half-hour :wait-time minute}))))
