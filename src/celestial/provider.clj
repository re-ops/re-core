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

(ns celestial.provider
  "common providers functions"
    (:use 
      [celestial.common :only (get* import-logging)]
      [clojure.core.strint :only (<<)]
      [slingshot.slingshot :only  [throw+ try+]]))

(def str? [string? :msg "not a string"])

(def vec? [vector? :msg "not a vector"])

(defn- key-select [v] (fn [m] (select-keys m (keys v))))

(defn mappings [res ms]
  "Maps raw model keys to specific model"
  (let [vs ((key-select ms) res) ]
     (merge 
       (reduce (fn [r [k v]] (dissoc r k)) res ms)
       (reduce (fn [r [k v]] (assoc r (ms k) v)) {} vs)) 
     ))

(defn os->template 
  "Os key to vmware template" 
  [hyp]
  (fn [os]
   (let [ks [:hypervisor hyp :ostemplates os]]
     (try+ 
      (apply get* ks)
      (catch [:type :celestial.common/missing-conf] e
        (throw+ {:type :missing-template :message 
          (<< "no matching vmware template found for ~{os} add one to configuration under ~{ks}")}))))))

(defn transform 
  "specific model transformations"
  [res ts]
    (reduce 
      (fn [res [k v]] (update-in res [k] v )) res ts))

