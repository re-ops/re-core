(ns celestial.features.digitial
  "digital ocean support"
  (:require 
    [celestial.model :refer (vconstruct)]
    [celestial.fixtures.core :refer (with-conf) :as f]
    [celestial.fixtures.data :refer [redis-digital]])
  (:use midje.sweet)
 )

(with-conf
  (let [{:keys [machine digital-ocean]} redis-digital]
    (fact "legal digital-ocean system" :digital-ocean
       (:spec (vconstruct redis-digital)) => (contains {:name "red1.local"}))))
