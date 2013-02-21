(ns celestial.test.jobs
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.jobs :only (initialize-workers)]
    [celestial.redis :only (create-worker)]
    expectations.scenarios)
 )

(scenario 
  (expect (initialize-workers)) 
  (expect (interaction (jobs/enqueue "stage" "p/host result"))))
