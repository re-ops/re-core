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
    (:require 
      [flatland.useful.map :refer (dissoc-in*)]
      [subs.core :refer (validation when-not-nil)]
      [supernal.sshj :refer (ssh-up?)] 
      [celestial.model :refer (hypervisor)]
      [minderbinder.time :refer (parse-time-unit)]
      [celestial.common :refer (get! import-logging curr-time)]
      [clojure.core.strint :refer (<<)]
      [slingshot.slingshot :refer (throw+ try+)]))

(defn- key-select [v] (fn [m] (select-keys m (keys v))))

(defn repeates [k v] 
  (if (set? k) (interleave k (repeat (count k) v)) [k v]))

(defn mappings 
  {:test #(assert (= {:template :ubuntu :flavor :ubuntu :search "local"}
                     (mappings {:os :ubuntu :domain "local"} {:os #{:template :flavor} :domain :search})))
   :doc "Maps raw model keys to specific model keys, single key can fan out to multiple keys using a set"} 
  [res ms]
  (let [mapped ((key-select ms) res)]
     (merge 
       (reduce (fn [r [k v]] (dissoc r k)) res ms)
       (reduce (fn [r [k v]] (apply assoc r (repeates (ms k) v))) {} mapped)) 
     ))

(defn selections 
  "Select group of keys from map"
  ([m kys] ((selections kys) m))
  ([kys]
   (letfn [(select [k] (fn [m] (select-keys m k)))]
     (apply juxt (map select kys)))))

(defn os->template 
  "Os key to template/VM/image" 
  [hyp]
  (fn [os]
    (let [ks [hyp :ostemplates os]]
      (try+ 
        (apply hypervisor ks)
        (catch [:type :celestial.common/missing-conf] e
          (throw+ {:type :missing-template} 
                  (<< "no matching template found for ~{os} add one to configuration under ~{ks}")))))))

(defn transform 
  "specific model transformations"
  [res ts]
  (reduce 
    (fn [res [k v]] (update-in res [k] v)) res ts))

(defn wait-for 
  "A general wait for pred function"
  [{:keys [timeout sleep] :or {sleep [1 :seconds]} :as timings} pred err message]
  {:pre [(map? timings)]}
  (let [wait (+ (curr-time) (parse-time-unit timeout))  ]
    (loop []
      (if (> wait (curr-time))
        (if (pred) 
          true
          (do (Thread/sleep (parse-time-unit sleep)) (recur))) 
        (throw+ (merge err timings) message)))))

(defn wait-for-ssh [address user timeout]
  {:pre [address user timeout]}
  (wait-for {:timeout timeout}
            #(try 
               (ssh-up? {:host address :port 22 :user user})
               (catch Throwable e false))
            {:type ::ssh-failed :timeout timeout} "Timed out while waiting for ssh"))

(defn map-key [m from to]
  (dissoc-in* (assoc-in m to (get-in m from)) from))

; common validations
(validation :ip 
    (when-not-nil (partial re-find #"\d+\.\d+\.\d+\.\d+") "must be a legal ip address"))

(validation :mac 
  (when-not-nil (partial re-find #"^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") "must be a legal mac address"))

(test #'mappings)

(defn running? [this] (= (.status this) "running"))

(defn wait-for-stop [this timeout ex]
  "Wait for an ip to be avilable"
  (wait-for {:timeout timeout} #(not (running? this))
    {:type ex :timeout timeout} 
      "Timed out on waiting for ip to be available"))

(defn wait-for-start [this timeout ex]
  "Wait for an ip to be avilable"
  (wait-for {:timeout timeout} #(running? this)
    {:type ex :timeout timeout} 
      "Timed out on waiting for ip to be available"))
