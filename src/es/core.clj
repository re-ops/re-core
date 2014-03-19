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

(ns es.core
  "Core elasticsearch module"
  (:require 
    [components.core :refer (Lifecyle)] 
    [es.systems :refer (initialize)]
    [es.node :refer (start-n-connect stop)]))

(defrecord Elastic 
  [] 
  Lifecyle
  (setup [this]
    (start-n-connect) 
    (initialize))
  (start [this]
    (start-n-connect))
  (stop [this]
    (stop)))

(defn instance 
   "creates a Elastic components" 
   []
  (Elastic.))
