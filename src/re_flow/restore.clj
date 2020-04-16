(ns re-flow.restore
  "Restore a backup"
  (:require
   [re-core.repl :refer (hosts with-ids)]
   [re-mote.zero.disk :as disk]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (successful-hosts)]
   [clara.rules :refer :all]))

(refer-timbre)

(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::restore :re-flow.core/state)
(derive ::validate :re-flow.core/state)

(def instance {:base kvm :args [defaults local c1-medium :backup "restore flow instance" (kvm-volume 128 :restore)]})

(defrule start
  "Start the restore process by triggering the creation of the instance"
  [?e <- ::start]
  =>
  (info "Starting to run setup instance" ?e)
  (insert! (assoc ?e :state :re-flow.setup/creating :spec instance)))

(defrule initialize-volume
  "Prepare volume for restoration"
  [?e <- :re-flow.setup/provisioned (= ?flow ::restore)]
  =>
  (info "Preparing volume for restoration" ?e)
  (disk/partition- (hosts (with-ids (?e :ids)) :hostname) "/dev/vdb")
  (disk/mount (hosts (with-ids (?e :ids)) :hostname) "/dev/vdb" "/media"))