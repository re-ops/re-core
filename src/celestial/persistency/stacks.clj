(ns celestial.persistency.stacks
  "Stack model"
  (:require 
    [subs.core :as subs :refer (validate! validation when-not-nil every-v)]
    [puny.core :refer (entity)]
    )
 )


(entity {:version 1} stack)

(validation :count {:template #{:required :Keyword} :count #{:required :Integer}})

(validation :system* (every-v  #{:count}))

(validation :shared {:owner #{:required :String} :env #{:required :Keyword} :machine #{:Map}})

(def stack-base {
   :systems #{:system*}
   :shared #{:required }
  })

(defn validate-stack
  [stack]
  (validate! stack stack-base :error ::non-valid-stack))
