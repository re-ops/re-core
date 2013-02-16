(ns celestial.test.ssh
  (:use 
    expectations.scenarios  
    [celestial.ssh :only (execute)]))

(scenario 
  (expect java.lang.AssertionError (execute {:host "bla"} "one two")))

