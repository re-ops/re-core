(ns celestial.test.persistency
  "Validating persistency layer"
  (:require 
    [celestial.persistency :as p]
    [taoensso.carmine :as car])
  (:use 
    [clojure.core.strint :only (<<)]
    expectations.scenarios ))


(scenario 
  (stubbing [p/hgetall [:k "v"] ]
    (expect {:k "v"} (p/host "bar"))))

(scenario 
  (stubbing [p/hgetall nil]
    (expect clojure.lang.ExceptionInfo (p/host "bar"))))
