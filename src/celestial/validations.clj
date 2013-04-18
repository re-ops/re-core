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

(ns celestial.validations
  (:use 
    [slingshot.slingshot :only  [throw+]]
    [bouncer.validators :only (defvalidator)]))

(defvalidator hash-v
  {:default-message-format "%s must be a hash"}
  [c] (map? c))

(defvalidator set-v
  {:default-message-format "%s must be a set"}
  [c] (set? c))

(defvalidator set-v
  {:default-message-format "%s must be a set"}
  [c] (set? c))

(defvalidator vec-v
  {:default-message-format "%s must be a vector"}
  [c] 
  (vector? c))

(defvalidator str-v
  {:default-message-format "%s must be a string"}
  [c] (string? c))

(defmacro validate-nest 
  "Bouncer nested maps validation with prefix key"
  [target pref & body]
  (let [with-prefix (reduce (fn [r [ks vs]] (cons (into pref ks) (cons vs  r))) '() (partition 2 body))]
  `(b/validate ~target ~@with-prefix)))

(defn validate! [t r]
   (when-let [e (:bouncer.core/errors (second r))] 
      (throw+ {:type t :errors e})))
