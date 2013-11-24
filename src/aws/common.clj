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

(ns aws.common
  "Common functionality like connection settings"
  (:require
    [amazonica.aws.ec2 :as ec2]
    [celestial.model :refer (hypervisor)]  
    ))

(defn creds [] (dissoc (hypervisor :aws) :ostemplates))

(defmacro with-ctx
  "Run ec2 action with context (endpoint and creds)"
  [f & args]
  `(~f (assoc (creds) :endpoint ~'endpoint) ~@args))

(defn instance-desc [endpoint instance-id & ks]
  (-> 
    (with-ctx ec2/describe-instances {:instance-ids  [instance-id]})
      first :instances first (get-in ks)))

(defn image-id [machine]
  (hypervisor :aws :ostemplates (machine :os) :ami))
