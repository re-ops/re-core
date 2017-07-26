(ns re-core.features.templates
  (:require
   [re-core.persistency.systems :as s]
   [re-core.fixtures.core :refer (is-type? with-defaults with-conf)]
   [re-core.fixtures.populate :refer (populate-all)]
   [re-core.fixtures.data :refer (small-redis)])
  (:use midje.sweet))

(with-conf
  (with-state-changes [(before :facts (populate-all :skip :templates))]
    (fact "basic template persistency" :integration :redis :templates
          (let [id (s/add-template small-redis)]
            (s/get-template id) => (contains {:type "redis" :name "small-redis"}))
          (let [provided {:env :dev :owner "admin" :machine {:hostname "foo" :domain "local"}}]
            (s/templatize "small-redis" provided) => 101))))
