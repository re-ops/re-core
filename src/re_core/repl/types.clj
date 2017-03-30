(ns re-core.repl.types
  "Types repl functions"
  (:require
    [re-core.persistency.types :as t]
    [re-core.repl.base :refer [Repl]])
  (:import [re_core.repl.base Types])
  )


(extend-type Types
  Repl
  (ls [this]
    [this {:types (map t/get-type (t/all-types))}])
  (ls [this & opts])
  (find [this exp])
  (rm [this types ])
  (grep [this types k v]))
