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

(ns physical.validations
  "physical machine validations"
  (:require 
    [subs.core :refer (validate! combine validation when-not-nil)]
    ))

(def machine-entity
  {:machine {
    :domain #{:required :String} :ip #{:ip :required} 
    :hostname #{:required :String} :os #{:Keyword}
    :user #{:required :String}}})

(def physical-entity 
  {:physical {
    :mac #{:required :mac} :broadcast #{:required :ip}
   }})

(defn validate-entity [physical]
  (validate! physical (combine machine-entity physical-entity) 
     :error ::invalid-system))

(def physical-provider {
  :remote {:host #{:required :String} :user #{:required :String}}                      :interface {:broadcast #{:required :ip} :mac #{:required :mac}}  
 })

(defn validate-provider [remote interface]
 (validate! {:remote remote :interface interface}
     physical-provider :error ::invalid-machine))
