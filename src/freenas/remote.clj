(ns freenas.remote
  (:require 
    [cheshire.core :refer :all]
    [clojure.core.strint :refer (<<)]
    [org.httpkit.client :as client])
 )

(defn root 
   []
  "https://192.168.3.117/api/v1.0/"
  )

(defn auth  
   []
  ["root" "foobar"]
  )

(defn call [verb api args]
 @(verb (<< "~(root)~{api}") (merge args {:basic-auth (auth) :insecure? true})))


(call client/get  "jails/jails/?format=json" {})

