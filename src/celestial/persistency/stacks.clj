(ns celestial.persistency.stacks
  "Stack model"
  (:require 
    [subs.core :as subs :refer (validate! validation when-not-nil)]
    [puny.core :refer (entity)]
    )
 )


(entity {:version 1} stack)

(def stack-base {
   :systems #{:required :system*}                
  })
