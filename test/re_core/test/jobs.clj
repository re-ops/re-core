(ns re-core.test.jobs
  (:require
    [re-core.persistency.systems :as s]
    [taoensso.carmine :as car]
    [re-core.redis :refer (server-conn)]
    [taoensso.carmine.locks :refer (with-lock acquire-lock)]
    [re-core.common :refer (minute)]
    [clojure.core.strint :refer (<<)]
    [re-core.jobs :refer (initialize-workers workers job-exec create-wks enqueue)]
    [re-core.jobs :as jobs])
  (:use midje.sweet)
  (:import java.lang.AssertionError))


(fact "with-lock used if :identity key was provided"
   (job-exec identity {:message {:identity "red1" :args {:machine {:hostname "red1"}}} :attempt 1 :user "ronen"}) => {:status :success}
   (provided
     (s/get-system "red1") => {:machine {:hostname "red1"}}
     (jobs/save-status anything :success)  => {:status :success}  :times 1))


(fact "enqueue to workless queue should fail"
     (enqueue "foobar" {}) => (throws AssertionError))


