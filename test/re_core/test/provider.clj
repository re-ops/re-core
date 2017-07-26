(ns re-core.test.provider
  (:require 
    [midje.sweet :reffer :all] 
    [re-core.provider :refer mappgins]))

(fact "mappgins"
  (mappings {:os :ubuntu :domain "local"} {:os #{:template :flavor} :domain :search}) => 
    {:template :ubuntu :flavor :ubuntu :search "local"})
