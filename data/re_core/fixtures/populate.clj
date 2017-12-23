(ns re-core.fixtures.populate
  "data population"
  (:gen-class true)
  (:require
   [re-core.log :refer (setup-logging)]
   [es.types :as t]
   [es.common :as es]
   [es.node :refer (stop connect)]
   [re-core.model :refer (figure-virt)]
   [re-core.fixtures.core :refer (with-conf)]
   [clojure.test.check.generators :as g]
   [re-core.redis :as red]
   [es.systems :as s]
   [re-core.fixtures.data :refer (admin ronen) :as d]))

(setup-logging)

(defn add-types
  "populates types"
  []
  (t/create d/smokeping-type (:type d/smokeping-type))
  (t/create d/jvm-type (:type d/jvm-type))
  (t/create d/redis-type (:type d/redis-type)))

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

(defn re-initlize
  "Re-init datastores"
  ([] (re-initlize false))
  ([clear-es]
   (connect)
   (when clear-es (es/clear))
   (es/initialize)
   (red/clear-all)))

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
  (connect)
  (re-initlize)
  (t/create type (:type type))
  (s/create system id))

(defn -main
  "run populate all"
  [& args]
  (populate-all)
  (stop)
  (println "populate done!"))
