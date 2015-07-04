(ns celestial.test.freenas
  (:require 
    [celestial.model :refer (vconstruct)]
    [celestial.fixtures.core :refer (with-conf) :as f]
    [celestial.fixtures.data :refer [redis-freenas]])
  (:use midje.sweet))

(with-conf
  (let [{:keys [machine freenas]} redis-freenas]
    (fact "legal freenas system"
       (:spec (vconstruct redis-freenas)) => (contains {:jail_host "red1"}))))
