(ns re-core.repl.base
  "Core repl functions"
  (:require
   [re-core.model :refer (figure-virt)]
   [re-mote.zero.management :refer (registered?)]
   [clansi.core :refer (style)]
   [table.core :refer (table)]
   [clojure.set :refer (intersection)]))

(defprotocol Repl
  "Repl functions on re-core model types"
  (ls [this] [this & opts])
  (filter-by [this items f])
  (ack [this items opts])
  (add- [this specs])
  (valid? [this base args])
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

(defn system-row [[id s]]
  (-> s
      (assoc :id (str ".." (apply str (take-last 4 id))) :hyp (figure-virt s))
      (assoc :registered (if (registered? (get-in s [:machine :hostname])) (style "✔" :green) (style "x" :red)))
      (select-keys*
       [:id] [:machine :hostname] [:machine :ip] [:type] [:machine :os] [:hyp] [:description] [:registered])))

(defmethod pretty :systems [this {:keys [systems]}]
  (table
   (map system-row systems) :style :borderless))

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
  (require '[re-core.repl.base :as base :refer (run | ls grep rm add- pretty filter-by ack summary valid?)]))
