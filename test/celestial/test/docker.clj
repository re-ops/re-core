(ns celestial.test.docker
  "Docker construction"
 (:require
  [clojure.core.strint :refer (<<)]
  [celestial.model :refer (vconstruct)]
  [celestial.fixtures.data :refer [redis-docker-spec]]
  [celestial.fixtures.core :refer [with-conf with-m?]] 
  )
 (:use midje.sweet))


(with-conf
  (fact "basic creation"
     (vconstruct redis-docker-spec) => (contains {})))

