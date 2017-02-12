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
  (find [this exp])
  (rm [this items])
  (grep [this items k v]))

(defn select-keys* [m & paths]
  (into {} (map (fn [p] [(last p) (get-in m p)])) paths))

(defn grep-system [k v [id system]]
  (let [sub (select-keys* system [:owner] [:machine :hostname] [:machine :os] [:machine :ip])]
    (= v (sub k))
    ))

(defrecord Systems [] Repl 
  (ls [this] 
    (with-admin
      (let [systems (into [] (s/systems-for (celestial.api.systems/working-username)))]
        [this {:systems (doall (map (juxt identity s/get-system) systems))}])))
  (ls [this & opts]
   (let [m (apply hash-map opts)]))
  (find [this exp])
  (rm [this systems]
    (doseq [id (map first systems)]
      (s/delete-system! (Integer/valueOf id))))
  (grep [this systems k v]
      [this {:systems (filter (partial grep-system k v) (systems :systems))}]))


(defrecord Types [] Repl 
  (ls [this]
    [this {:types (map t/get-type (t/all-types))}])
  (ls [this & opts])
  (find [this exp])
  (rm [this types ])
  (grep [this types k v]) 
  )


(defmulti pretty 
  (fn [[_ m]] (clojure.set/intersection (into #{} (keys m)) #{:systems :types})))

(defn render [[id m]]
 (-> m 
   (select-keys* [:owner] [:machine :hostname] [:machine :os] [:machine :ip])
   (assoc :id id)))

(defmethod pretty #{:systems} [[_ {:keys [systems]}]]
  (let [formatter (format-columns bold-white-font [:right 10] "  " reset-font [:right 2] "  "
                     [:right 5] "  " [:right 12] "  " :none)]
    (write-rows *out* formatter [:hostname :id :owner :os :ip] (map render systems))))
 
(defmethod pretty #{:types} [[_ {:keys [types]}]]
  (let [formatter (format-columns bold-white-font [:right 10] "  " reset-font [:right 10] "  "[:right 20] :none)]
    (write-rows *out* formatter [:type (comp first keys) :description] types)))

(defmacro |> [source fun]
  (let [f (first fun) args (rest fun)]
   `(let [[this# res#] ~source]
      (~f this# res# ~@args))))


(def systems (Systems.))
(def types (Types.))

;; (pretty (ls types))
;; (macroexpand-1 '(|> (ls systems) (grep :os :ubuntu-14.04 :four)))
;; (pretty (|> (ls systems) (grep :os :ubuntu-14.04 )))
