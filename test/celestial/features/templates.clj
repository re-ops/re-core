(ns celestial.features.templates
 (:require 
    [celestial.security :refer (set-user current-user)]
    [celestial.persistency.systems :as s]
    [celestial.fixtures.core :refer (is-type? with-defaults with-conf)]
    [celestial.fixtures.populate :refer (populate-all)]
    [celestial.fixtures.data :refer (small-redis)])
 (:use midje.sweet))

(set-user {:username "admin"}
  (with-conf
    (with-state-changes [(before :facts (populate-all :skip :templates))]
      (fact "basic template persistency" :integration :redis :templates
      (let [id (s/add-template small-redis)]
        (s/get-template id) => (contains {:type "redis" :name "small-redis"})) 
      (let [provided {:env :dev :owner "admin" :machine {:hostname "foo" :domain "local"}}]
        (s/templatize "small-redis" provided) => 101)))))
