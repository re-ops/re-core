(ns celestial.test.persistency
  "Validating persistency layer"
  (:require 
    [celestial.persistency :as p]
    [taoensso.carmine :as car])
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.redis :only (wcar-disable)]
    expectations.scenarios ))


(scenario 
  (with-redefs [car/hgetall* (fn [_] {:k "v"}) wcar-disable true]
    (expect {:k "v"} (p/host "bar"))))

(scenario 
  (with-redefs [car/hgetall* (fn [_] nil) wcar-disable true]
    (expect clojure.lang.ExceptionInfo (p/host "bar"))))
