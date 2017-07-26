(ns re-core.metrics
  "health checks"
  (:require
   [es.node :as es]
   [es.common :refer (index)]
   [re-core.redis :refer (wcar)]
   [taoensso.carmine :as car]
   [components.core :refer (Lifecyle)]
   [metrics.core :refer (default-registry)]
   [metrics.jvm.core :refer (instrument-jvm)]
   [metrics.health.core :refer [default-healthcheck-registry]]
   [metrics.health.core :as health :refer (defhealthcheck)])
  (:import [java.util Calendar]))

(defn seconds []
  (.get (Calendar/getInstance) (Calendar/SECOND)))

(defhealthcheck "redis"
  (fn []
    (let [now (seconds)]
      (if (= (wcar (car/ping)) "PONG")
        (health/healthy "Managed to ping redis" now)
        (health/unhealthy "Failed to ping redis" now)))))

(defhealthcheck "elasticsearch"
  (fn []
    (let [now (seconds) h (es/health (into-array [index]))]
      (if (= h "GREEN")
        (health/healthy "ES index health is GREEN" now)
        (health/unhealthy (str "ES index health is" h)  now)))))

(defrecord Metrics
           []
  Lifecyle
  (setup [this]
    (let [m (.getMetrics default-registry)]
      (when (or (nil? m) (not (get m "jvm.thread.deadlock.count")))
        (instrument-jvm))))
  (start [this])
  (stop [this]
    (.unregister default-healthcheck-registry "elasticsearch")))

(defn instance
  "Creating metrics instance "
  []
  (Metrics.))
