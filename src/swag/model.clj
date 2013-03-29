(ns swag.model
  "Swagger model related functionality" 
  (:use 
    [swag.common :only (defstruct-)]
    [clojure.string :only (capitalize)])
  )

(def ^{:doc "User defined types"} models (atom {}))

(defn add-model [k m] (swap! models assoc k m))

(defstruct- model :id :properties)

(defn nest-types [m]
  (reduce (fn [r [k v]] (assoc r k (if (keyword? v) {:type v} v))) {} m))

(defmacro defmodel
  "Defining a swagger model: 
  (defmodel module :name :string :src :string) " 
  [name & props]
  `(add-model ~(keyword name) (model- ~(-> name str capitalize) (nest-types (hash-map ~@props)))))

