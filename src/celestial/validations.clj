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
  (:refer-clojure :exclude (set? sequential? keyword?))
  (:use 
    [slingshot.slingshot :only  [throw+]]
    [bouncer.validators :only (defvalidator)]
    ))

(defvalidator hash?
  {:default-message-format "%s must be a hash"}
  [c] 
    (if c (map? c) true))

(defvalidator set?
  {:default-message-format "%s must be a set"}
  [c] (if c (clojure.core/set? c) true))

(defvalidator vec?
  {:default-message-format "%s must be a vector"}
  [c] 
  (if c (vector? c) true))

(defvalidator sequential?
  {:default-message-format "%s must be a sequential (list or vector)"}
  [c] 
  (if c (clojure.core/sequential? c) true))

(defvalidator str?
  {:default-message-format "%s must be a string"}
  [c] (if c (string? c) true))

(defvalidator keyword?
  {:default-message-format "%s must be a keyword"}
  [c] (if c (clojure.core/keyword? c) true))

(defmacro validate-nest 
  "Bouncer nested maps validation with prefix key"
  [target pref & body]
  (let [with-prefix (reduce (fn [r [ks vs]] (cons (into pref ks) (cons vs  r))) '() (partition 2 body))]
  `(b/validate ~target ~@with-prefix)))

(defmacro valid
  "Checks validation result (r), throws exepction of type t in case errors are found else returns true"
  [error-type target vset]
   `(if-let [e# (:bouncer.core/errors (second (bouncer.core/validate ~target ~vset)))] 
      (throw+ {:type ~error-type :errors e#}) 
     true))
