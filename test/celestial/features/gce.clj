(ns celestial.features.gce
  (:require 
    [celestial.model :refer (vconstruct)]
    [celestial.fixtures.core :refer (with-conf) :as f]
    [celestial.fixtures.data :refer [redis-gce]])
  (:use midje.sweet)
 )

(with-conf
  (let [{:keys [machine gce]} redis-gce]
    (fact "legal gce system"
       (:spec (vconstruct redis-gce)) => (contains {:machine-type  "n1-standard-1"}))))
