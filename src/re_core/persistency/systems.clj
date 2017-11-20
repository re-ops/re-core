(ns re-core.persistency.systems
  "systems persistency layer"
  (:refer-clojure :exclude [type])
  (:require
   [es.systems :as es]
   [robert.hooke :as h]
   [re-core.persistency [types :as t]]
   [physical.validations :as ph]
   [taoensso.timbre :refer (refer-timbre)]
   [kvm.validations :as kv]
   [aws.validations :as av]
   [subs.core :as subs :refer (validate! validation when-not-nil)]
   [puny.core :refer (entity)]
   [slingshot.slingshot :refer  [throw+]]
   [re-core.model :refer (clone hypervizors figure-virt check-validity)]
   [clojure.core.strint :refer (<<)]
   aws.model))

(refer-timbre)

(declare validate-system es-put es-delete)

(entity {:version 1} system :indices [type env]
        :intercept {:create [es-put]
                    :update [es-put]
                    :delete [es-delete]})

(defn is-system? [s]
  (and (map? s) (s :env)))

(defn es-put
  "runs a specified es function on system fn call"
  [f & args]
  (if (map? (first args))
    (let [id (apply f args) spec (first args)]
      (es/put (str id) spec) id)
    (apply f args)))

(defn es-delete
  "reducing usage quotas for owning user on delete"
  [f & args]
  (let [system (first (filter map? args)) id (first (filter number? args))]
    (when-not (is-system? system)
      (es/delete (str id))))
  (apply f args))

(defn system-ip [id]
  (get-in (get-system id) [:machine :ip]))

(def system-base {:type #{:required}
                  :env #{:required :Keyword}})

(defn validate-system
  [system]
  (validate! system system-base :error ::non-valid-system)
  (check-validity system))

(defn clone-system
  "clones an existing system"
  [id {:keys [hostname] :as spec}]
  (add-system
   (-> (get-system id)
       (assoc-in [:machine :hostname] hostname)
       (clone spec))))

(declare validate-template)

(entity {:version 1} template :id name :indices [type])

(validation :empty (fn [v] (when-not (nil? v)  "value must be empty")))

(def template-base {:type #{:required} :defaults #{:required :Map}
                    :name #{:required :String} :description #{:String}
                    :machine {:hostname #{:empty} :domain #{:empty}}})

(defn validate-template
  [template]
  (validate! template template-base :error ::non-valid-template)
  (check-validity (assoc template :as :template)))

(defn templatize
  "Create a system from a template"
  [name {:keys [env machine] :as provided}]
  {:pre [machine (machine :hostname) (machine :domain)]}
  (let [{:keys [defaults] :as t} (get-template! name)]
    (add-system (merge-with merge t (defaults env) provided))))

(defn system-val
  "grabbing instance id of spec"
  [spec ks]
  (get-in (get-system (spec :system-id)) ks))
