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

(ns hooks.consul
  (:require 
    [celestial.persistency.systems :as s] 
    [conjul.catalog :refer (register de-register)]
    [celestial.common :refer (import-logging)]))

(import-logging)

(defn add-node
  [{:keys [system-id consul dc] :as args}]
  (let [{:keys [machine]} (s/get-system system-id)]
    (debug "registered node" (register consul (machine :hostname) (machine :ip) dc))))

(defn remove-node
  [{:keys [machine consul dc] :as args}]
  (debug "removed node" (de-register consul (machine :hostname) dc)))


(def actions {:reload {:success add-node} :create {:success add-node} 
              :destroy {:success remove-node :error remove-node}
              :stage {:success add-node}})

(defn update-node [{:keys [event workflow] :as args}]
    (when-let [action (get-in actions [workflow event])] (action args)))
