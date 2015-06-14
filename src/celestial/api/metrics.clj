(ns celestial.api.metrics
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

(defroutes metrics 
  (GET "/metrics" [:as request] (serve-metrics request))
  (GET "/health" [:as request] 
    (let [res (map (fn [[k v]] [k (from-java v)]) (.runHealthChecks default-healthcheck-registry))]
      (-> res (into {}) first generate-string response (header "Content-Type" "application/json")))))
