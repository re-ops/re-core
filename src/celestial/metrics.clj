(ns celestial.health
  "health checks"
  (:require 
    [metrics.core :refer (default-registry)]
    [metrics.jvm.core :refer (instrument-jvm)]
    [metrics.health.core :as health :refer (defhealthcheck)])
 )

(defhealthcheck "second-check" 
  (fn [] (let [now (.getSeconds (java.util.Date.))]
     (if (< now 30)
        (health/healthy "%d is less than 30!" now)
        (health/unhealthy "%d is more than 30!" now)))))

(let [m (.getMetrics default-registry)]
  (when (or (nil? m) (not (get m "jvm.thread.deadlock.count")))
    (instrument-jvm)
    ))
