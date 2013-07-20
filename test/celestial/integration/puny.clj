(ns celestial.integration.puny
  (:require [puny.core :as p])
  (:import clojure.lang.ExceptionInfo)
  (:use  
     midje.sweet
    [celestial.fixtures :only (is-type?)]
    [celestial.redis :only (clear-all wcar)]))

(with-state-changes [(before :facts (clear-all))]
  (fact "id-less entity" :integration :puny
        (p/entity foo)        
        (defn validate-foo [foo] {})
        (let [id (add-foo {:bar 1})]
          (get-foo id) => {:bar 1}
          (foo-exists? id) => truthy
          (update-foo id {:bar 2}) 
          (get-foo id) => {:bar 2}
          (delete-foo id)
          (foo-exists? id) => falsey))

  (fact "entity with id" :integration :puny
        (p/entity {:ver 1} user :id name)        
        (defn validate-user [user] {})
        (let [id (add-user {:name "me"})]
          (get-user "me") => {:name "me"}
          (user-exists? "me") => truthy
          (update-user {:name "me" :blue "bar"}) 
          (get-user "me") => {:name "me" :blue "bar"}
          (delete-user "me")
          (user-exists? "me") => falsey))

  (fact "basic index actions" :integration :puny
        (p/index-fns house {:indices [zip n]})
        (wcar (index-house 5 {:zip "1234" :n 5}))
        (wcar (index-house 6 {:zip "1234" :n 6}))
        (get-house-index :zip "1234") => ["5" "6"]
        (get-house-index :n 5) => ["5"]
        (wcar (clear-house-indices 5 {:zip "1234" :n 5}))
        (get-house-index :zip "1234")  => ["6"]
        (wcar (reindex-house 6 {:zip "1234" :n 6} {:zip "1235" :n 6})) 
        (get-house-index :zip "1234")  => []
        (get-house-index :zip "1235")  => ["6"]
        )

  (fact "indexed entity" :integration :puny
        (p/entity position :indices [longi alti])        
        (defn validate-position [postion] {})
        (let [id (add-position {:longi 10 :alti 12})]
          (get-position id) => {:longi 10 :alti 12}
          (position-exists? id) => truthy
          (update-position id {:longi 11 :alti 12}) 
          (get-position-index :longi 11) => [(str id)]
          (delete-position id)
          (position-exists? id) => falsey
          (get-position-index :longi 11) => []
          ))

  (fact "indexed entity with id" :integration :puny
        (p/entity car :id license :indices [color])        
        (defn validate-car [car] {})
        (add-car {:license 123 :color "black"}) => 123
        (add-car {:license 123 :color "black"}) => (throws ExceptionInfo (is-type? :celestial.integration.puny/conflicting-car))
        (get-car 123) => {:license 123 :color "black"}
        (car-exists? 123) => truthy
        (get-car-index :color "black") => ["123"]
        (update-car {:license 123 :color "blue"}) 
        (get-car-index :color "black") => []
        (get-car-index :color "blue") => ["123"]
        (delete-car 123)
        (car-exists? 123) => falsey
        (get-car-index :color "blue") => [])

  (fact "fail fase actions" :integration :puny
        (p/entity planet :id name)
        (defn validate-planet [planet] {})
        (add-planet {:name "lunar"})
        (get-planet "lunar") => {:name "lunar"}
        (planet-exists! "lunar") => true
        (planet-exists! "foo") => (throws ExceptionInfo (is-type? :celestial.integration.puny/missing-planet))
        (delete-planet! "foo") => (throws ExceptionInfo (is-type? :celestial.integration.puny/missing-planet))
        (update-planet {:name "foo"}) => (throws ExceptionInfo (is-type? :celestial.integration.puny/missing-planet))
        (get-planet! "lunar") => {:name "lunar"}
        (delete-planet! "lunar") 
        (get-planet "lunar") => {} 
        (get-planet! "lunar") => (throws ExceptionInfo (is-type? :celestial.integration.puny/missing-planet)) 
        )

  (fact "entity metadata" :integration :puny
     (p/entity {:ver 1} metable :id name)
     (defn validate-metable [metable] {})
     (add-metable {:name "foo"}) 
     (get-metable "foo") => {:name "foo"}
     (meta (get-metable "foo")) => {:ver 1})
  )

