(ns aws.validations
  "AWS based validations"
  (:use 
    [clojure.core.strint :only (<<)]
    [bouncer [core :as b] [validators :as v :only (defvalidatorset )]])
  (:require 
    [celestial.validations :as cv]))

(defvalidatorset machine-entity
  :hostname [v/required cv/str?]
  :user [v/required cv/str?]
  :os [v/required cv/keyword?])

(defvalidator aws-entity
  :instance-type [v/required cv/str?]
  :image-id [v/required cv/str?]
  :key-name [v/required cv/str?]
  :endpoint [v/required cv/str?]
  )

(defvalidator entity-validation
  :aws aws 
  :machine machine-entity)

(defn validate-entity 
 "aws based systems entity validation " 
  [aws]
  (cv/validate!! ::invalid-system aws entity-validation))
