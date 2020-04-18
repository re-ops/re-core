(ns re-flow.restore
  "Restore a backup"
  (:require
   [re-mote.zero.disk :as disk]
   [re-mote.zero.restic :as restic]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (run-?e run-?e-non-block)]
   [clara.rules :refer :all]))

(refer-timbre)

(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::restoring :re-flow.core/state)
(derive ::validate :re-flow.core/state)
(derive ::partitioned :re-flow.core/state)
(derive ::mounted :re-flow.core/state)
(derive ::restored :re-flow.core/state)

(def instance {:base kvm :args [defaults local c1-medium :backup "restore flow instance" (kvm-volume 128 :restore)]})

(defrule start
  "Start the restore process by triggering the creation of the instance"
  [?e <- ::start]
  =>
  (info "Starting to run setup instance")
  (insert! (assoc ?e :state :re-flow.setup/creating :spec instance)))

(defrule initialize-volume
  "Prepare volume for restoration"
  [?e <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::restore) (= failure false)]
  =>
  (info "Preparing volume for restoration")
  (insert!
   (assoc ?e :state ::partitioned :failure (not (run-?e disk/partition- ?e "/dev/vdb")))
   (assoc ?e :state ::mounted :failure (not (run-?e disk/mount ?e "/dev/vdb" "/media")))))

(defrule volume-ready
  "Trigger actual restore if all prequisits are met (volume is ready)"
  [::partitioned [{:keys [failure]}] (= failure false)]
  [?e <- ::mounted [{:keys [failure]}] (= failure false)]
  =>
  (info "Initiating the restoration process")
  (run-?e-non-block restic/restore ?e ::restored [1 :hour] (fn [m] (not= (-> m vals first :code) 0)) (?e :bckp) "/media"))

(defrule restoration-successful
  "Processing the restoration result"
  [?e <- ::restored [{timeout :timeout failure :failure :or {timeout false failure false}}] (and (= timeout false) (= failure false))]
  =>
  (info "restoration was successful"))

(defrule restoration-failed
  "Processing the restoration result"
  [?e <- ::restored [{:keys [timeout failure]}] (or (= timeout true) (= failure true))]
  =>
  (info "restoration failed!"))
