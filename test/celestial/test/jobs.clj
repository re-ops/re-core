(ns celestial.test.jobs
  (:require 
    [taoensso.carmine :as car]
    [celestial.jobs :as jobs])
  (:use 
    midje.sweet
    [celestial.redis :only (server-conn)]
    [taoensso.carmine.locks :only (with-lock acquire-lock)]
    [celestial.common :only (minute)]
    [clojure.core.strint :only (<<)]
    [celestial.jobs :only (initialize-workers workers job-exec create-wks enqueue)])
  (:import java.lang.AssertionError))


(fact "with-lock used if :identity key was provided" 
   (job-exec identity {:message {:identity "red1" :args {:machine {:hostname "red1"}}} :attempt 1}) => {:status :success}
   (provided 
     (server-conn) => {}
     (acquire-lock {} "red1" 1800000 300000) => nil :times 1))


(fact "enqueue to workless queue should fail"
     (enqueue "foobar" {}) => (throws AssertionError))

