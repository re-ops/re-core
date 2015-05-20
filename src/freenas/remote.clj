(ns freenas.remote
  (:require 
    [celestial.common :refer (import-logging)]
    [cheshire.core :refer :all]
    [celestial.model :refer (hypervisor)]
    [clojure.core.strint :refer (<<)]
    [org.httpkit.client :as client])
 )

(defn freenas [ks]
  )
(defn root 
   []
   (<< "https://~(hypervisor :freenas :host)/api/v1.0/"))

(defn auth  
   []
  [(hypervisor :freenas :user) (hypervisor :freenas :password)]
  )

(defn call [verb api args]
   @(verb (<< "~(root)~{api}") (merge args {:basic-auth (auth) :insecure? true})))


;; (clojure.pprint/pprint (parse-string (:body (call client/get  "jails/jails/?format=json" {})) true))
;; (clojure.pprint/pprint (parse-string (:body (call client/get  "jails/templates/?format=json" {})) true))

