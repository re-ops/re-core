(ns celestial.persistency
  (:use 
    [celestial.redis :only (wcar)]
    [clojure.core.strint :only (<<)]) 
  (:require 
    [taoensso.carmine :as car])
  )

(def hosts 
  {:redis-test.local {:classes {:redis {:append true}}}
   :redis-test.home.net {:classes {:redis {:append true}}}
   :redis {:classes {:redis {:append true}}}
   })

(defn profile [host]
  (hosts host))

(defn sys-key []
  (let [id (car/incr "systems")]
    (<< "systems:~{id}")
    ))

(defn new-type [t profile]
  (wcar (car/set t profile)))


