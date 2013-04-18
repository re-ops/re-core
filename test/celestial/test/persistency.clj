(ns celestial.test.persistency
  "Validating persistency layer"
  (:require 
    [celestial.persistency :as p]
    [taoensso.carmine :as car])
  (:use 
    midje.sweet
    [clojure.core.strint :only (<<)]
    [celestial.redis :only (wcar-disable)]
    ))


(with-redefs [car/hgetall* (fn [_] {:k "v"}) wcar-disable true]
 (fact "host get" 
    (p/host "bar") =>  {:k "v"}))

(with-redefs [car/hgetall* (fn [_] nil) wcar-disable true]
    (fact "missing host error"
      (p/host "bar") => (throws clojure.lang.ExceptionInfo)))
