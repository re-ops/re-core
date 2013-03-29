(ns swag.common)

(defmacro defstruct- [name & ks]
  "A struct def that creates a constructor in the form of sname-"
 `(do 
   (defstruct ~name ~@ks)
   (defn ~(symbol (str name "-")) [& k#] (apply struct ~name k# ))))
