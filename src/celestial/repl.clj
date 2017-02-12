(ns celestial.repl
  "Repl Driven Celestial "
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.persistency.types :as t]
    [es.systems :as es :refer (set-flush)]
    [celestial.security :refer (set-user current-user)]
    [taoensso.timbre  :as timbre :refer (set-level!)]
    [io.aviso.columns :refer (format-columns write-rows)]
    [io.aviso.ansi :refer :all]
    ))

(set-level! :debug)

(def admin {
  :username "admin" :password "foo"
  :envs [:dev :qa :prod] 
  :roles #{:celestial.roles/admin} 
  :operations [] })

(defmacro with-admin [& body]
  `(with-redefs [celestial.security/current-user (fn [] {:username "admin"})
                celestial.persistency.users/get-user! (fn [a#] admin)]
       ~@body 
       ))

(defprotocol Repl 
  "Basic shell like functions on Celestial model types" 
  (ls [this] [this & opts])
  (find [& opts])
  (rm [& opts])
  )


(defrecord Systems [] Repl 
  (ls [this] 
    (with-admin (celestial.api.systems/systems-range 0 10)))
  (ls [this & opts]
   (let [m (apply hash-map opts)]
     ))
  (find [& opts])
  (rm [& opts])
  )

(def systems (Systems.))

(defrecord Types [] Repl 
  (ls [this]
    {:types (map t/get-type (t/all-types))})
  (ls [this & opts])
  (find [& opts])
  (rm [& opts]))

(def types (Types.))

(defn select-keys* [m & paths]
  (into {} (map (fn [p] [(last p) (get-in m p)])) paths))



(defmulti pretty 
  (fn [m] (clojure.set/intersection (into #{} (keys m)) #{:systems :types})))

(defn render [[id m]]
 (-> m 
   (select-keys* [:owner] [:machine :hostname] [:machine :os] [:machine :ip])
   (assoc :id id)))

(defmethod pretty #{:systems} [{:keys [meta systems]}]
  (let [formatter (format-columns blue-font [:right 10] "  " reset-font [:right 2] "  "[:right 5] "  " [:right 12] "  " :none)]
    (write-rows *out* formatter [:hostname :id :owner :os :ip] (map render systems))))

(defmethod pretty #{:systems} [{:keys [meta systems]}]
  (let [formatter (format-columns blue-font [:right 10] "  " reset-font [:right 2] "  "[:right 5] "  " [:right 12] "  " :none)]
    (write-rows *out* formatter [:hostname :id :owner :os :ip] (map render systems))))
 
(defmethod pretty #{:types} [{:keys [types]}]
  (let [formatter (format-columns bold-white-font [:right 10] "  " reset-font [:right 10] "  "[:right 20] :none)]
    (write-rows *out* formatter [:type (comp first keys) :description] types)))

(pretty (ls types))
(pretty (ls systems))
 
