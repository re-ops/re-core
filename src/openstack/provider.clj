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

(ns openstack.provider
  (:import 
    org.openstack4j.api.Builders
    org.openstack4j.openstack.OSFactory))

(defn servers [endpoint username password tenant]
  (-> (OSFactory/builder) 
     (.endpoint endpoint)
     (.credentials username  password)
     (.tenantName tenant)
     (.authenticate)
     (.compute)
     (.servers)))

(defconstrainedrecord Instance [endpoint spec user]
  "An Openstack compute instance"
  [#_(provider-validation spec) (-> endpoint nil? not)]
  Vm
  (create [this])

  (start [this])

  (delete [this])

  (stop [this])

  (status [this] ))

