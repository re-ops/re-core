(ns celestial.repl
  "Repl Driven Celestial "
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.persistency.types :as t]
    [es.systems :as es :refer (set-flush)]
    [celestial.security :refer (set-user current-user)]
    [taoensso.timbre  :as timbre :refer (set-level!)]
    [io.aviso.columns :refer (format-columns write-rows)]
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
  (ls [this])
  (ls [this & opts]
   (clojure.pprint/pprint {:types (map t/get-type (t/all-types))}))
  (find [& opts])
  (rm [& opts]))

(def types (Types.))

(defn select-keys* [m & paths]
  (into {} (map (fn [p] [(last p) (get-in m p)])) paths))

(defn format-systems 
  [{:keys [meta systems]}]
  (let [formatter (format-columns  [:right 10] "  "  [:right 5] "  " :none)
        systems' (map #(select-keys* % [:owner] [:machine :hostname] [:machine :os]) (map second systems))]
      (write-rows *out* formatter  [:hostname :owner :os] systems')))
 
(ls types :foo 1)
(format-systems (ls systems))
;; (ls types)
 
