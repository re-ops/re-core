(ns celestial.test.jobs
  (:require 
    [taoensso.carmine :as car]
    [celestial.redis :refer (server-conn)]
    [taoensso.carmine.locks :refer (with-lock acquire-lock)]
    [celestial.common :refer (minute)]
    [clojure.core.strint :refer (<<)]
    [celestial.jobs :refer (initialize-workers workers job-exec create-wks enqueue)] 
    [celestial.jobs :as jobs])
  (:use midje.sweet)
  (:import java.lang.AssertionError))


(fact "with-lock used if :identity key was provided" 
   (job-exec identity {:message {:identity "red1" :args {:machine {:hostname "red1"}}} :attempt 1 :user "ronen"}) => {:status :success}
   (provided 
     (server-conn) => {}
     (acquire-lock {} "red1" 1800000 300000) => nil :times 1
     (jobs/save-status anything :success)  => {:status :success}  :times 1
     ))


(fact "enqueue to workless queue should fail"
     (enqueue "foobar" {}) => (throws AssertionError))


(fact "jobs by envs"
   (jobs/jobs-status [:dev]) => 
      {:jobs [{:env :dev}] :succesful [{:env :dev}] :erroneous [{:env :dev}]}
   (provided
     (jobs/running-jobs-status) => [{:env :dev} {:env :qa}]
     (jobs/done-jobs-status) => {:succesful [{:env :dev} {:env :qa}] 
                                 :erroneous [{:env :dev} {:env :qa}]}))
