(ns re-ops.config.core
  "Configuration handling"
  (:refer-clojure :exclude  [load])
  (:require
   [aero.core :as aero]
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]
   [re-ops.config.spec :as cs]
   [re-share.config.core :refer (config)]
   [clojure.core.strint :refer (<<)]))

(defn load-config
  "Load configuration into an Atom (can be called multiple times)"
  ([]
   (load-config
    ::cs/config (<< "~(System/getProperty \"user.home\")/.re-ops.edn") {:profile :dev}))
  ([spec path profile]
   (let [c (aero/read-config path profile)]
     (if-not (s/valid? ::cs/config c)
       (expound/expound ::cs/config c)
       (reset! config c)))))

