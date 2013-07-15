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
    [vc.vijava :only (disk-format-types)]
    [clojure.core.strint :only (<<)]
    [celestial.validations :only (validate)]
    [bouncer [core :as b] [validators :as v :only (defvalidatorset)]])
  (:require 
    [celestial.validations :as cv]))

(defvalidatorset machine-entity 
  :user [v/required cv/str?] 
  :password [v/required cv/str?] 
  :os [v/required cv/keyword?])

(defn has-ip [instance] (-> instance :machine :ip empty? not))

(defvalidatorset machine-networking
    :mask [cv/str? (v/required :pre has-ip)]
    :network [cv/str? (v/required :pre has-ip)]
    :gateway [cv/str? (v/required :pre has-ip)]
    :search [cv/str? (v/required :pre has-ip)]
    :names [cv/vec? (v/required :pre has-ip)]
  )

(defvalidatorset machine-common
    :cpus [v/number v/required]
    :memory [v/number v/required]
    :ip [cv/str?]
  )

(defvalidatorset machine-provider
     :template [v/required cv/str?])

(def formats (into #{} (keys disk-format-types)))

(defvalidatorset allocation-provider
    :pool [cv/str?]
    :disk-format [v/required (v/member formats :message (<< "disk format must be either ~{formats}"))]
    :datacenter [cv/str? v/required] 
  )

(defvalidatorset vcenter-provider 
   :allocation allocation-provider
   :machine machine-common
   :machine machine-networking 
   :machine machine-provider  
  )

(defn provider-validation [allocation machine]
  (validate ::invalid-vm {:allocation allocation :machine machine} vcenter-provider))


(defvalidatorset vcenter-entity
    :pool [cv/str?]
    :datacenter [cv/str? v/required] 
    :disk-format [v/required (v/member formats :message (<< "disk format must be either ~{formats}"))]
  )

(defvalidatorset entity-validation
   :machine machine-common 
   :machine machine-networking
   :machine machine-entity
   :vcenter  vcenter-entity
  )

(defn validate-entity
 "vcenter based system entity validation for persistence layer" 
  [vcenter]
   (validate ::invalid-system vcenter entity-validation)
  )

