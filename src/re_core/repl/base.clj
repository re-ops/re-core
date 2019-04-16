(ns re-core.repl.base
  "Core repl functions"
  (:require
   [table.core :refer (table)]
   [clojure.set :refer (intersection)]))

(def admin {:username "admin" :password "foo"
            :envs [:dev :qa :prod]
            :roles #{:re-core.roles/admin}
            :operations []})

(defprotocol Repl
  "Repl functions on re-core model types"
  (ls [this] [this & opts])
  (filter-by [this items f])
  (ack [this items opts])
  (add- [this specs])
  (rm [this items])
  (grep [this items k v]))

(defprotocol Report
  (summary [this m]))

(defmulti pretty
  (fn [_ m]
    (let [k (first (intersection (into #{} (keys m)) #{:systems :types :jobs}))]
      (when (not (empty? (m k)))
        k))))

(defn select-keys* [m & paths]
  (into {} (map (fn [p] [(last p) (get-in m p)])) paths))

(defn render [[id m]]
  (-> m
      (select-keys* [:type] [:machine :hostname] [:machine :os] [:machine :ip])
      (assoc :id id)))

(defmethod pretty nil [_ _] nil)

(defmethod pretty :systems [this {:keys [systems]}]
  (table
   (map
    (fn [[id s]]
      (select-keys* (assoc s :id id) [:id] [:machine :hostname] [:type] [:machine :os] [:machine :ip])) systems) :style :borderless))

(defmethod pretty :types [_ {:keys [types]}]
  (table
   (map (fn [[id t]] (select-keys* (assoc t :id id) [:id] [:description] [:re-conf :src])) types) :style :borderless))

(defmacro | [source fun & funs]
  (let [f (first fun) args (rest fun)]
    `(let [[this# res#] ~source]
       (~f this# res# ~@args))))

(defmacro run [f p s & fns]
  (if-not (empty? fns)
    `(run (~p ~f ~s) ~(first fns) ~(second fns) ~@(rest (rest fns)))
    `(~p ~f ~s)))

(defrecord Systems [])

(defrecord Types [])

(defn refer-base []
  (require '[re-core.repl.base :as base :refer (run | ls grep rm add- pretty filter-by ack summary)]))
