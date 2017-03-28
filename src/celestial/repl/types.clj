(ns celestial.repl.types
  "Types repl functions"
  (:require
    [celestial.persistency.types :as t]
    [celestial.repl.base :refer [Repl]])
  (:import [celestial.repl.base Types])
  )


(extend-type Types
  Repl
  (ls [this]
    [this {:types (map t/get-type (t/all-types))}])
  (ls [this & opts])
  (find [this exp])
  (rm [this types ])
  (grep [this types k v]))
