(ns celestial.repl
  "Repl Driven Celestial "
  (:require 
    [celestial.persistency.systems :as s]
    [celestial.persistency.types :as t]
    [es.systems :as es :refer (set-flush)]
    [celestial.security :refer (set-user current-user)]
    ))

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

(defn systems []
  (with-admin (celestial.api.systems/systems-range 0 10)))

(defn types []
  {:types (map t/get-type (t/all-types))})

(defmulti ls 
   "list systems/types"
   (fn [f] 
     (println f)
     (-> (str f) (.split  "@") first (.split "\\$") last keyword)))

(defmethod ls :systems [systems]
  (clojure.pprint/pprint (systems)))

(defn find 
  "Find systems"
   [user]
   )

(defn rm 
   [k id]
  )

(ls systems)

