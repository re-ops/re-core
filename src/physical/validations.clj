(ns physical.validations
  "physical machine validations"
  (:require 
    [re-core.model :refer (check-validity)] 
    [subs.core :refer (validate! combine validation when-not-nil)]
    ))

(def machine-entity
  {:machine {
    :domain #{:required :String} :ip #{:ip :required} 
    :hostname #{:required :String} :os #{:Keyword}
    :user #{:required :String}}})

(def physical-entity 
  {:physical {
    :mac #{:mac} :broadcast #{:ip}
   }})

(defmethod check-validity [:physical :entity] [physical]
  (validate! physical (combine machine-entity physical-entity) :error ::invalid-system))

(def physical-provider {
  :remote {:host #{:required :String} :user #{:required :String}}                      :interface {:broadcast #{:required :ip} :mac #{:required :mac}}  
 })

(defn validate-provider [remote interface]
 (validate! {:remote remote :interface interface}
     physical-provider :error ::invalid-machine))
