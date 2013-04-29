(ns celestial.integration.puny
  (:require [puny.core :as p])
  (:use  
     midje.sweet
    [celestial.redis :only (clear-all)]))

(with-state-changes [(before :facts (clear-all))]
  (fact "id-less entity" :integration :redis
        (p/entity foo)        
        (defn validate-foo [foo] {})
        (let [id (add-foo {:bar 1})]
          (get-foo id) => {:bar 1}
          (foo-exists? id) => truthy
          (update-foo id {:bar 2}) 
          (get-foo id) => {:bar 2}
          (delete-foo id)
          (foo-exists? id) => falsey))

  (fact "entity with id" :integration :redis
        (p/entity user :id name)        
        (defn validate-user [user] {})
        (let [id (add-user {:name "me"})]
          (get-user "me") => {:name "me"}
          (user-exists? "me") => truthy
          (update-user {:name "me" :blue "bar"}) 
          (get-user "me") => {:name "me" :blue "bar"}
          (delete-user "me")
          (user-exists? "me") => falsey))

  (fact "basic index actions" :integration :redis
        (p/index-fns house {:indices [zip n]})
        (index-house 5 {:zip "1234" :n 5})
        (index-house 6 {:zip "1234" :n 6})
        (get-house-index :zip "1234") => ["5" "6"]
        (get-house-index :n 5) => ["5"]
        (clear-house-indices 5 {:zip "1234" :n 5})
        (get-house-index :zip "1234")  => ["6"]
        (reindex-house 6 {:zip "1234" :n 6} {:zip "1235" :n 6}) 
        (get-house-index :zip "1234")  => []
        (get-house-index :zip "1235")  => ["6"]
        )

  (fact "indexed entity" :integration :redis
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

  (fact "indexed entity with id" :integration :redis
        (p/entity car :id license :indices [color])        
        (defn validate-car [car] {})
        (add-car {:license 123 :color "black"})
        (get-car 123) => {:license 123 :color "black"}
        (car-exists? 123) => truthy
        (get-car-index :color "black") => ["123"]
        (update-car {:license 123 :color "blue"}) 
        (get-car-index :color "black") => []
        (get-car-index :color "blue") => ["123"]
        (delete-car 123)
        (car-exists? 123) => falsey
        (get-car-index :color "blue") => []))

