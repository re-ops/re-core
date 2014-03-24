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

(ns celestial.roles
  "Celetial roles 
  * Admin can access any machine and change any user
  * Super user can access any machine but cant change users
  * User can access only his machines (no user manipulation at all)
  * Anonymous can't access any machine nor any other service
  Note that by 'any machine' we mean any machine in the environments he is allowed 
  to access.
  ")

(def ^{:doc "roles string to keyword map"}
  roles-m {"admin" ::admin "user" ::user "super-user" ::super-user 
           "anonymous" ::anonymous "system" ::system})

(def roles (into #{} (vals roles-m)))

(derive ::admin ::super-user)

(derive ::super-user ::user)

(derive ::system ::super-user)

(def user #{::user})

(def admin #{::admin})
 
(def su #{::super-user})

(def system #{::system})

(defn admin? [{:keys [roles] :as user}]
  (clojure.set/subset? admin roles))

(defn su? [{:keys [roles] :as user}]
  (some #(isa? % ::super-user) roles))

(defn system? [{:keys [roles] :as user}]
  (some #(= % ::system) roles))
