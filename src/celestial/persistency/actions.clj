(ns celestial.persistency.actions
  "Actions persistency"
  (:require
    [slingshot.slingshot :refer  [throw+]]
    [subs.core :as subs :refer (validate!)]
    [clojure.core.strint :refer (<<)]
    [puny.core :refer (entity)]))

(entity action :indices [operates-on])

(defn find-action-for [name type]
  (let [ids (get-action-index :operates-on type) 
        actions (map #(-> % Long/parseLong  get-action) ids)]
    (first (filter #(= (-> % :name) name) actions))))

(defn cap? [m] (contains? m :capistrano))

(def cap {:args #{:required :Vector}})

(def action-base-validation
  {:src #{:required :String} :operates-on #{:required :String :type-exists}
   :name #{:required :String}})

(defn validate-action [{:keys [capistrano] :as action}]
  (when capistrano (validate! capistrano cap :error ::invalid-cap-action))
  (validate! action action-base-validation :error ::invalid-action ))
 
