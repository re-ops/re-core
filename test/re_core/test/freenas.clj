(ns re-core.test.freenas
  (:require 
    [re-core.model :refer (vconstruct)]
    [re-core.fixtures.core :refer (with-conf) :as f]
    [re-core.fixtures.data :refer [redis-freenas]])
  (:use midje.sweet))

(with-conf
  (let [{:keys [machine freenas]} redis-freenas]
    (fact "legal freenas system"
       (:spec (vconstruct redis-freenas)) => (contains {:jail_host "red1"}))))
