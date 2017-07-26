(ns re-core.test.provider
  (:require
   [re-core.provider :refer (mappings)])
  (:use midje.sweet))

(fact "mappings"
      (mappings {:os :ubuntu :domain "local"} {:os #{:template :flavor} :domain :search}) =>
      {:template :ubuntu :flavor :ubuntu :search "local"})
