(comment 
   re-core, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns gce.validations
  "Google compute validations"
  (:require 
    [re-core.model :refer (check-validity)]
    [re-core.provider :refer (mappings)]
    [clojure.core.strint :refer (<<)] 
    [subs.core :refer (validate! combine validation when-not-nil every-v every-kv)]
    ))

(def machine-entity
  {:machine {
    :domain #{:required :String} :ip #{:String} 
    :os #{:required :Keyword} :user #{:required :String}}})

(validation :tag* (every-v #{:String}))

(def gce-entity
  {:gce {
    :machine-type #{:required :String} :zone #{:required :String}
    :tags #{:Vector :tag*} :project-id #{:required :String}
   }})

(defmethod check-validity [:gce :entity] [spec]
  (validate! spec (combine machine-entity gce-entity) :error ::invalid-system))

(validation :params {:sourceImage #{:required :String}})

(validation :disk {:initializeParams #{:required :Map}})

(validation :disk* (every-v #{:disk}))

(def gce-provider { 
   :name #{:required :String}
   :machineType #{:required :String}
   :disks #{:required :disk*}
   })

(defn validate-provider [gce spec]
   (validate! gce gce-provider :error ::invalid-gce-instance))

