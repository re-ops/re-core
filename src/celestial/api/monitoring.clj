(ns celestial.api.monitoring
  (:require 
    [compojure.core :refer [defroutes GET]]
    [metrics.ring.expose :refer [serve-metrics]]
    [cheshire.core :refer  [generate-string]]
    [metrics.health.core :refer [default-healthcheck-registry]] 
    [clojure.java.data :refer  [from-java]]
    [celestial.common :refer [success]]
    [ring.util.response :refer  [header response]]
    )
 )

(defn health-checks
   []
   (into {} (map (fn [[k v]] [k (from-java v)]) (.runHealthChecks default-healthcheck-registry)))
  )

(defroutes metrics 
  (GET "/metrics" [:as request] (serve-metrics request))
  (GET "/health" [:as request] 
      (-> (health-checks) generate-string response (header "Content-Type" "application/json"))))

