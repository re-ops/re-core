(ns re-share.config.core
  "Configuration handling"
  (:refer-clojure :exclude  [load])
  (:require
   [clojure.core.strint :refer (<<)]
   [aero.core :as aero]
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]
   [re-share.config.spec :as cs]))

(def config (atom nil))

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

(defn get!
  "Reading a keys path from configuration raises an error of keys not found"
  [& ks]
  {:pre [@config]}
  (if-let [v (get-in @config ks)]
    v
    (throw (ex-info (<< "No matching configuration keys ~{ks} found") {:keys ks :type ::missing-conf}))))

(defn get*
  "nil on missing version of get!"
  [& keys]
  {:pre [@config]}
  (get-in @config keys))

