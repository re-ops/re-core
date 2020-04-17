(ns re-flow.restore
  "Restore a backup"
  (:require
   [re-core.repl :refer (hosts with-ids)]
   [re-mote.zero.disk :as disk]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (successful-hosts successful-ids)]
   [clara.rules :refer :all]))

(refer-timbre)

(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::restore :re-flow.core/state)
(derive ::validate :re-flow.core/state)
(derive ::partitioning :re-flow.core/state)
(derive ::mounting :re-flow.core/state)

(def instance {:base kvm :args [defaults local c1-medium :backup "restore flow instance" (kvm-volume 128 :restore)]})

(defrule start
  "Start the restore process by triggering the creation of the instance"
  [?e <- ::start]
  =>
  (info "Starting to run setup instance" ?e)
  (insert! (assoc ?e :state :re-flow.setup/creating :spec instance)))

(defn run-?e
  "Run Re-mote pipeline on system ids provided by ?e and check if all were successful"
  [f {:keys [ids] :as ?e} & args]
  (let [result (apply (partial f (hosts (with-ids ids) :hostname)) args)]
    (= (into #{} ids) (successful-ids result))))

(defrule initialize-volume
  "Prepare volume for restoration"
  [?e <- :re-flow.setup/provisioned (= ?flow ::restore) (= ?failure false)]
  =>
  (info "Preparing volume for restoration")
  (insert! (assoc ?e :state ::partitioning :failure (not (run-?e disk/partition- ?e "/dev/vdb"))))
  (insert! (assoc ?e :state ::mounting :failure (not (run-?e disk/mount ?e "/dev/vdb" "/media")))))
