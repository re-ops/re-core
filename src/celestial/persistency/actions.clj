(ns celestial.persistency.actions
  "Actions persistency"
  (:require
    [subs.core :as subs :refer (validate!)]
    [puny.core :refer (entity)]))

(entity action :indices [operates-on])

(defn find-action-for [action-key type]
  (let [ids (get-action-index :operates-on type) 
        actions (map #(-> % Long/parseLong  get-action) ids)]
    (first (filter #(-> % :actions action-key nil? not) actions))))

(defn cap? [m] (contains? m :capistrano))

(def cap-nested {:capistrano {:args #{:required :Vector}}})

(def action-base-validation
  {:src #{:required :String} :operates-on #{:required :String :type-exists}})

(defn validate-action [{:keys [actions] :as action}]
  (doseq [[k {:keys [capistrano] :as m}] actions] 
    (when capistrano (validate! m cap-nested :error ::invalid-action)))
  (validate! action action-base-validation :error ::invalid-nested-action ))
 
