(ns celestial.persistency
  (:refer-clojure :exclude [type])
  (:use 
    [celestial.redis :only (wcar)]
    [clojure.core.strint :only (<<)]) 
  (:require 
    [taoensso.carmine :as car])
  )

(defn profile [host]
  #_(hosts host))

(defn sys-key []
  (let [id (car/incr "systems")]
    (<< "systems:~{id}")))

(defn type [t]
  "Reading a type"
  (wcar (car/get t)))

(defn new-type [t spec]
  "An application type and its spec see fixtures/redis-type.edn"
  (wcar (car/set t spec)))

(defn register-host [host t m]
  {:pre [(type t)]}
  "Mapping host to a given type and its machine"
  (wcar 
    (car/hset host :machine m)
    (car/hset host :type t)))

(defn host [h]
  (apply merge 
    (map (partial apply hash-map) (partition 2 (wcar (car/hgetall h))) )))


(new-type "z" {})
(register-host "foo" "z" {:foo 1})
(host "foo")


