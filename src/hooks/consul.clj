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

(ns hooks.consul
  (:require
    [re-core.persistency.systems :as s]
    [conjul.catalog :refer (register de-register)]
    [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn add-node
  [{:keys [system-id consul]:as args}]
  (let [{:keys [machine env]} (s/get-system system-id)
        {:keys [dc host] :as c} (consul env)]
    (when c
      (register host (machine :hostname) (machine :ip) dc)
      (debug "registered node in consul host" host "dc" dc))))

(defn remove-node
  [{:keys [env machine consul]:as args}]
  (when-let [{:keys [dc host]} (consul env)]
    (de-register host (machine :hostname) dc)
    (debug "removed node from consul host" host "dc" dc)))


(def actions {:reload {:success add-node} :create {:success add-node}
              :destroy {:success remove-node :error remove-node}
              :stage {:success add-node}})

(defn with-defaults
  "Add an empty consul if not defined"
  [args]
  (merge {:consul {}} args))

(defn update-node [{:keys [event workflow] :as args}]
    (when-let [action (get-in actions [workflow event])] (action (with-defaults args))))
