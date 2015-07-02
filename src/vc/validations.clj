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

(ns vc.validations
  (:use 
    [celestial.model :refer (check-validity)]
    [vc.vijava :only (disk-format-types)]
    [clojure.core.strint :only (<<)]
    [subs.core :only (validate! combine validation when-not-nil)]))

; only when ip exists
(def machine-networking
  {:machine {
    :netmask #{:required :String} :gateway #{:required :String} 
    :search #{:required :String} :names #{:required :Vector}}})

(def machine-common
  {:machine 
   {:cpus #{:required :number} :memory #{:required :number} :ip #{:String} :domain #{:required :String}}})

(def machine-provider
  {:machine {:template #{:required :String}}})

(def formats (into #{} (keys disk-format-types)))

(validation :format
  (when-not-nil formats (<< "disk format must be either ~{formats}")))

(def common-allocation  
  {:allocation {:pool #{:String} :hostsystem #{:required :String} :datacenter #{:required :String}} })

(def vcenter-provider 
  (combine {:allocation {:disk-format #{:format :required}}} common-allocation machine-common machine-provider ))

(defn provider-validation [allocation {:keys [ip] :as machine}]
  (validate! {:allocation allocation :machine machine} 
      (if ip (combine vcenter-provider machine-networking) vcenter-provider) :error ::invalid-vm ))

(def format-names (into #{} (map name (keys disk-format-types))))

(def machine-entity 
  {:machine {:user #{:required :String} :password #{:required :String} 
             :os #{:required :Keyword}}})

(validation :format-str
  (when-not-nil format-names (<< "disk format must be either ~{format-names}")))

(def vcenter-entity
  (combine {:allocation {:disk-format #{:format-str :required}}} 
      common-allocation machine-common machine-networking machine-entity))

(defmethod check-validity [:vcenter :entity] [{:keys [machine vcenter] :as vc}]
  (validate! {:allocation vcenter :machine machine} vcenter-entity :error ::invalid-system))
