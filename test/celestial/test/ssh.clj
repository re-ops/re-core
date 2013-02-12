(ns celestial.test.ssh
  (:use 
    clojure.test 
    [celestial.ssh :only (execute)]))

(deftest batch-check 
  (is (thrown? AssertionError (execute {:host "bla"} "one two"))))


