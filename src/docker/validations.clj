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

(ns docker.validations
  "Docker validations"
  (:require 
    [clojure.core.strint :refer (<<)] 
    [subs.core :refer (validate! combine validation when-not-nil every-v)]
    ))

(validation :port* (every-v #{:Integer}))

(validation :mount* (every-v #{:String}))

(def docker-entity
  {:docker
   {:node #{:required :Keyword} :image #{:required :String} 
    :ports #{:required :Vector :port*} :mounts #{:required :Vector :mount*}
    } 
   }
  )

(def machine-common
  {:machine
    {:cpus #{:required :Integer} 
     :memory #{:required :Integer} 
     :disk #{:required :Integer} }})

(defn validate-entity [spec]
  (validate! spec (combine machine-common)))

(defn validate-provider [start-spec create-spec]
   true  
  )

