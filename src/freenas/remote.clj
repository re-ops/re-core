(ns freenas.remote
  (:require 
    [cheshire.core :refer :all]
    [clojure.core.strint :refer (<<)]
    [org.httpkit.client :as client])
 )

(defn root 
   []
  "https://freenas/api/v1.0/"
  )

(defn auth  
   []
  ["root" ""]
  )

(defn call [verb api args]
 @(verb (<< "~(root)~{api}") (merge args {:basic-auth (auth) :insecure? true})))


;; (clojure.pprint/pprint (parse-string (:body (call client/get  "jails/jails/?format=json" {})) true))
;; (clojure.pprint/pprint (parse-string (:body (call client/get  "jails/templates/?format=json" {})) true))

