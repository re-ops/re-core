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
          (update-user {:name "me"}) 
          (delete-user "me")
          (user-exists? "me") => falsey))

  (fact "basic index actions" :integration :redis
        (p/index-fns house {:indices [zip n]})
        (index-house 5 {:zip "1234" :n 5})
        (index-house 6 {:zip "1234" :n 6})
        (get-house-index :zip "1234") => ["5" "6"]
        (get-house-index :n 5) => ["5"]
        (clear-house-indices 5 {:zip "1234" :n 5})
        (get-house-index :zip "1234")  => ["6"])

  #_(fact "property index" :integration :redis
          (p/entity user-2 :id name :indices [city])        
          (defn validate-user-2 [user] {})
          (let [id (add-user-2 {:name "me"})]
            (get-user-2 "me") => {:name "me"}
            (user-2-exists? "me") => truthy
            (update-user-2 {:name "me"}) 
            (delete-user-2 "me")
            (user-2-exists? "me") => falsey)))

