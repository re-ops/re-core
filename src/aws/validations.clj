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
  (:require 
    [clojure.core.strint :refer (<<)]
    [subs.core :as subs :refer (validate! combine every-v every-kv validation)]))

(def machine-entity
  {:machine {:hostname #{:required :String} :domain #{:required :String} :user #{:required :String}}})

(def aws-entity
  {:aws {
     :instance-type #{:required :String} :image-id #{:required :String}
     :key-name #{:required :String} :endpoint #{:required :String}}
   })


(defn validate-entity 
  "aws based systems entity validation " 
  [aws]
  (validate! aws (combine machine-entity aws-entity) :error ::invalid-system ))


(def aws-provider
  {:instance-type #{:required :String} :image-id #{:required :String} :key-name #{:required :String}})

(defn provider-validation [{:keys [aws] :as spec}]
  (validate! aws aws-provider :error ::invalid-aws))
