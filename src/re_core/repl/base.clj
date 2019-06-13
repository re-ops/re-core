(ns re-core.repl.base
  "Core repl functions"
  (:require
   [re-core.model :refer (figure-virt)]
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

(defmethod pretty nil [_ _] nil)

(defmethod pretty :systems [this {:keys [systems]}]
  (table
   (map
    (fn [[id s]]
      (select-keys* (merge s {:id id :hypervisor (figure-virt s)})
                    [:id] [:machine :hostname] [:type] [:machine :os] [:hypervisor] [:machine :ip])) systems) :style :borderless))

(defmethod pretty :types [_ {:keys [types]}]
  (table
   (map
    (fn [[id {:keys [cog] :as m}]]
      (-> m
          (select-keys*  [:description])
          (merge  cog)
          (assoc :id id)
          (update :args #(clojure.string/join " " %)))) types)
   :style :borderless))

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
