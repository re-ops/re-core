(ns celestial.persistency.actions
  "Actions persistency"
  (:refer-clojure :exclude  [name type])
  (:require
    [clojure.string :refer (join escape)]
    [celestial.model :refer (figure-rem)]
    [slingshot.slingshot :refer  [throw+]]
    [subs.core :as subs :refer (validate!)]
    [clojure.core.strint :refer (<<)]
    [puny.core :refer (entity)]))

(declare unique-name with-provided)

(entity action :indices [operates-on] :intercept {:create [unique-name with-provided] :update [with-provided]})

(defn- args-of [s]
  "grab args from string"
  (map #(escape % {\~ "" \{ "" \} ""}) (re-seq #"~\{\w+\}" s)))

(defn remoter [action]
  (get action (figure-rem action)))

(defn add-provided [action]
  "appends action expected arguments drived from args strings"
   (assoc action :provided (remove #{"target" "hostname"} (args-of (join " " ((remoter action) :args)))))) 

(defn with-provided [f & [a1 a2 & r :as args]]
  (cond
    (map? a1) (apply f (add-provided a1) r)
    (map? a2) (apply f a1 (add-provided a2) r)
    :else (apply f args)))

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

(defn validate-action [{:keys [name type] :as action}]
  (if-let [r (remoter action)] 
    (validate! r has-args :error ::invalid-remoter)
    (throw+ {:type ::no-matching-remoter :msg (<< "no matching remoter found for ~{action}")})
    )
  (validate! action action-base-validation :error ::invalid-action))

