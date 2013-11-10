(ns celestial.persistency.actions
  "Actions persistency"
  (:refer-clojure :exclude  [name type])
  (:require
    [slingshot.slingshot :refer  [throw+]]
    [subs.core :as subs :refer (validate!)]
    [clojure.core.strint :refer (<<)]
    [puny.core :refer (entity)]))

(declare unique-name)

(entity action :indices [operates-on] :intercept {:create [unique-name]})

(defn find-action-for [name type]
  (let [ids (get-action-index :operates-on type) 
        actions (map #(-> % Long/parseLong  get-action) ids)]
    (first (filter #(= (-> % :name) name) actions))))

(defn unique-name [f & [{:keys [name operates-on]} & r :as args]]
  (when (and name operates-on (find-action-for name operates-on))
    (throw+ {:type ::duplicated-action :msg (<< "action for ~{operates-on} named ~{name} already exists")}))
  (apply f args))

(def has-args {:args #{:required :Vector}})

(def action-base-validation
  {:src #{:required :String} :operates-on #{:required :String :type-exists}
   :name #{:required :String}})

(defn validate-action [{:keys [capistrano ruby name type] :as action}]
  (when (or capistrano ruby) 
    (validate! capistrano has-args :error ::invalid-cap-action))
  (validate! action action-base-validation :error ::invalid-action))

(defn- args-of [s]
  "grab args from string"
  (map #(clojure.string/escape % {\~ "" \{ "" \} ""}) (re-seq #"~\{\w+\}" s)))

;; (println (args-of "-S ~{target}"))

(defn action-args [as]
  "appends action expected arguments drived from args strings"

  )
