(ns re-core.repl.base
  "Core repl functions"
  (:require
    [io.aviso.ansi :refer :all]
    [io.aviso.columns :refer (format-columns write-rows)]
    [clojure.set :refer (intersection)]))

(def admin {
  :username "admin" :password "foo"
  :envs [:dev :qa :prod]
  :roles #{:re-core.roles/admin}
  :operations []})

(defprotocol Repl
  "Repl functions on re-core model types"
  (ls [this] [this & opts])
  (filter-by [this items f])
  (ack [this items])
  (rm [this items])
  (add [this specs])
  (grep [this items k v]))

(defmulti pretty
  (fn [_ m] (intersection (into #{} (keys m)) #{:systems :types :jobs})))

(defn select-keys* [m & paths]
  (into {} (map (fn [p] [(last p) (get-in m p)])) paths))

(defn render [[id m]]
 (-> m
   (select-keys* [:owner] [:machine :hostname] [:machine :os] [:machine :ip])
   (assoc :id id)))

(defmethod pretty #{:systems} [this {:keys [systems] :as m}]
  (let [formatter (format-columns bold-white-font [:right 10] "  " reset-font [:right 2] "  "
                     [:right 5] "  " [:right 12] "  " :none)]
    (write-rows *out* formatter [:hostname :id :owner :os :ip] (map render systems))
    ))

(defmethod pretty #{:types} [_ {:keys [types]}]
  (let [formatter (format-columns bold-white-font [:right 10] "  " reset-font [:right 10] [:right 20] :none)]
    (write-rows *out* formatter [:type (comp first keys) :description] types)))

(defmethod pretty #{:jobs} [_ {:keys [jobs]}]
  (let [formatter (format-columns bold-white-font [:right 3] "  " reset-font [:right 10] :none)]
    (write-rows *out* formatter [:system  :job] jobs)))

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
  (require '[re-core.repl.base :as base :refer (run | ls grep rm add pretty filter-by ack)]))
