(ns celestial.integration.hooks
  "Updating an actual tinymasq instance expected to be available at https://192.168.1.10:8444"
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.fixtures.populate :refer (populate-all)]
    [celestial.security :refer (set-user)]
    [celestial.fixtures.core :refer (with-defaults with-conf)]
    [hooks.tinymasq :refer (update-dns remove-host)])
  (:use midje.sweet)
 )

(def conf {
   :domain "local" :tinymasq "https://192.168.1.10:8444" :user "admin" :password "foobar"
 })

(def create (merge conf {:event :success :workflow :create :system-id 1}))

(def reload (merge conf {:event :success :workflow :reload :system-id 1}))

(def machine {:machine {:hostname "dummy" "ip" "1.2.3.4"}})

(def delete (merge conf machine))

(with-conf
  (with-state-changes [(before :facts (set-user {:username "admin"} (populate-all)))]
    (fact "adding a new host" :hooks :tinymasq
      (set-user {:username "admin"}
        (s/update-system 1 (merge-with merge (s/get-system 1) machine))
        (update-dns create) => "host added"
        (remove-host delete) => "host removed"))))
