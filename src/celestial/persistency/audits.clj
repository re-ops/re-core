(ns celestial.persistency.audits
  "Audits persistency"
  (:refer-clojure :exclude  [name type])
  (:require
    [puny.migrations :refer (Migration register)]
    [clojure.string :refer (join escape)]
    [celestial.model :refer (figure-rem)]
    [slingshot.slingshot :refer  [throw+]]
    [subs.core :as subs :refer (validate!)]
    [clojure.core.strint :refer (<<)]
    [puny.core :refer (entity)]))

(declare unique-name with-provided)

(entity action :indices [operates-on] :intercept {:create [unique-name with-provided] :update [with-provided]})

(def audit-types #{:kibana})

(entity audit :id [name])

(validation :audit-type
  (when-not-nil audit-types (<< "Audit type must be either ~{audit-types}")))

(def audit-validation
  {:name #{:required :String} :query #{:required :String} 
   :args #{:required :Vector} :type #{:required :audit-type}
   })

(defn validate-audit [{:keys [name type] :as audit}]
  (validate! audit audit-validation :error ::invalid-audit))
