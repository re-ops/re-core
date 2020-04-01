(ns re-flow.core
  "Persistent flow engine"
  (:require
   [com.rpl.specter :refer [select ALL keypath]]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [re-core.repl :refer :all]
   [clara.rules :refer :all]))

(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(defn get-ids [f]
  (select [ALL (keypath :results :success) ALL :args ALL :system-id] @f))

(defrule create-system
  "Create restore systems"
  [:creating (= ?flow :restore)]
  =>
  (let [ds (get-ids (create kvm defaults local c1-medium :backup "restore flow instance"))]
    (insert! {:state :created :flow ?flow :ids ds})))

(defrule restore-system-created
  "System was created for within a flow"
  [?e <- :created  (= ?flow :restore)]
  =>
  (println "systems created!" (?e :ids))
  #_(provision (matching id)))

(comment
  (-> (mk-session 're-flow.core :fact-type-fn :state)
      (insert {:state :creating :flow :restore})
      (fire-rules)))
