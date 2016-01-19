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
    [celestial.model :refer (check-validity)]
    [clojure.core.strint :refer (<<)]
    [subs.core :as subs :refer (validate! combine every-v every-kv validation when-not-nil)]))

(def machine-entity
  {:machine {
     :hostname #{:required :String} :domain #{:required :String} 
     :user #{:required :String} :os #{:required :Keyword} 
  }})

(def ebs-type #{"io1" "standard" "gp2"})

(validation :ebs-type
  (when-not-nil ebs-type (<< "EBS type must be either ~{ebs-type}")))

(validation :volume {
    :device #{:required :device} :size #{:required :Integer}
    :clear #{:required :Boolean} :volume-type #{:required :ebs-type}
    :iops #{:Integer}
   })

(validation :iops 
  (fn [{:keys [volume-type iops]}] 
    (when (and (= volume-type "io1") (nil? iops)) "iops required if io1 type is used"))) 

(validation :volume* (every-v #{:volume}))

(validation :io-volume*  (every-v #{:iops}))

(validation :group* (every-v #{:String}))

(def aws-entity
  {:aws {
     :instance-type #{:required :String} :key-name #{:required :String}
     :endpoint #{:required :String} :volumes #{:volume* :io-volume*}
     :security-groups #{:Vector :group*} :availability-zone #{:String}
     :ebs-optimized #{:Boolean}
    }})


(defmethod check-validity [:aws :entity] [aws]
  (validate! aws (combine machine-entity aws-entity) :error ::invalid-system))

(def aws-provider
  {:instance-type #{:required :String} :key-name #{:required :String}
   :placement {:availability-zone #{:String}} :security-groups #{:Vector :group*}
   :min-count #{:required :Integer} :max-count #{:required :Integer} 
   :ebs-optimized #{:Boolean}
   })

(defn provider-validation [{:keys [aws] :as spec}]
  (validate! aws aws-provider :error ::invalid-aws))
