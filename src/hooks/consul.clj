(ns hooks.consul
  (:require
   [re-core.persistency.systems :as s]
   [conjul.catalog :refer (register de-register)]
   [taoensso.timbre :refer (refer-timbre)]))

(refer-timbre)

(defn add-node
  [{:keys [system-id consul] :as args}]
  (let [{:keys [machine env]} (s/get-system system-id)
        {:keys [dc host] :as c} (consul env)]
    (when c
      (register host (machine :hostname) (machine :ip) dc)
      (debug "registered node in consul host" host "dc" dc))))

(defn remove-node
  [{:keys [env machine consul] :as args}]
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
