(ns re-core.fixtures.populate
  "data population"
  (:require
   [re-core.log :refer (setup-logging)]
   [es.types :as t]
   [rubber.core :refer (list-indices delete-index)]
   [re-share.components.elastic :as esc]
   [es.common :refer (types)]
   [re-core.model :refer (figure-virt)]
   [re-core.fixtures.core :refer (with-conf)]
   [re-share.config :as conf]
   [clojure.test.check.generators :as g]
   [es.systems :as s]
   [re-core.fixtures.data :refer (admin ronen) :as d]))

(setup-logging)

(defn add-types
  "populates types"
  []
  (t/create d/smokeping-type)
  (t/create d/jvm-type)
  (t/create d/redis-type))

(def host
  (g/fmap (partial apply str)
          (g/tuple (g/elements ["zeus-" "atlas-" "romulus-" "remus-"]) g/nat)))

(def ip (g/fmap #(str "192.168.1." %) (g/such-that #(<= (.length %) 3) (g/fmap str g/nat))))

(def machines
  (g/fmap (partial zipmap [:hostname :ip]) (g/tuple host ip)))

(def host-env-gen
  (g/fmap (partial zipmap [:env :type])
          (g/tuple
           (g/elements [:dev :qa :prod])
           (g/elements ["redis" "smokeping"]))))

(defn with-ids [m]
  (let [virt (figure-virt m) ids {:aws :instance-id}]
    (g/fmap #(merge-with merge m %)
            (g/hash-map virt
                        (g/hash-map (ids virt) (g/such-that #(> (.length %) 10) g/string-alphanumeric 100))))))

(def systems-gen
  (g/bind host-env-gen
          (fn [v]
            (g/fmap #(merge % v)
                    (g/one-of
                     (into [(g/return d/redis-kvm-spec)] (mapv with-ids [d/redis-ec2-spec])))))))

(def systems-with-machines
  (g/bind machines
          (fn [v]
            (g/fmap #(update-in % [:machine] (fn [m] (merge m v))) systems-gen))))

(defn puts []
  (doseq [s (g/sample systems-with-machines 100)]
    (s/create s)))

(def elastic (esc/instance types :re-core false))

(defn re-initlize
  "Re-init datastore"
  ([]
   (re-initlize false))
  ([c]
   (conf/load (fn [_] {}))
   (.start elastic)
   (when c
    (doseq [idx (filter #(.startsWith % "re-core") (list-indices))]
       (delete-index idx)))
   (esc/initialize :re-core types false)))

(def populators {:types add-types :systems puts})

(defn populate-all
  "populates all data types"
  [& {:keys [skip] :or {skip []}}]
  (re-initlize true)
  (doseq [[_ p]
          (dissoc populators skip)] (p)))

(defn populate-system
  "Adds single type and system"
  [type system id]
  (.setup elastic)
  (re-initlize)
  (t/create type)
  (s/create system id))

(defn -main
  "run populate all"
  [& args]
  (populate-all)
  (.stop elastic)
  (println "populate done!"))
