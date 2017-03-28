(comment 
   re-core, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns re-core.persistency.common
  "common persistency layer functions"
  (:require 
    [clojure.string :refer (escape)]))

(defn args-of [s]
  "grab args from string"
  (into #{} (map #(escape % {\~ "" \{ "" \} ""}) (re-seq #"~\{\w+\}" s))))

(defn with-transform [t f & [a1 a2 & r :as args]]
  (cond
    (map? a1) (apply f (t a1) r)
    (map? a2) (apply f a1 (t a2) r)
    :else (apply f args)))
