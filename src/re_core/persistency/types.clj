(ns re-core.persistency.types
  (:refer-clojure :exclude [type])
  (:require
   [puny.core :refer (entity)]
   [subs.core :as subs :refer (validate! combine every-kv validation)]))

(entity type :id type)

(validation :puppet* {:tar #{:required :String}
                      :src  #{:String}
                      :args #{:Vector}})

(defn validate-type [t]
  (validate! t {:type #{:required :String} :puppet #{:puppet*}} :error ::non-valid-type))

