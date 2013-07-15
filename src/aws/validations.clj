(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns aws.validations
  "AWS based validations"
  (:use 
    [clojure.core.strint :only (<<)]
    [bouncer [core :as b] [validators :as v :only (defvalidatorset)]])
  (:require 
    [celestial.validations :as cv]))

(defvalidatorset machine-entity
  :hostname [v/required cv/str?]
  :user [v/required cv/str?])

(defvalidatorset aws-entity
  :instance-type [v/required cv/str?]
  :image-id [v/required cv/str?]
  :key-name [v/required cv/str?]
  :endpoint [v/required cv/str?]
  )

(defvalidatorset entity-validation
  :aws aws-entity 
  :machine machine-entity)

(defn validate-entity 
 "aws based systems entity validation " 
  [aws]
  (cv/validate!! ::invalid-system aws entity-validation))


(defvalidatorset aws-provider
  :instance-type [v/required cv/str?]
  :image-id [v/required cv/str?]
  :key-name [v/required cv/str?])

(defn provider-validation [{:keys [aws] :as spec}]
  (cv/validate!! ::invalid-aws aws aws-provider))
