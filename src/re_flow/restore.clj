(ns re-flow.restore
  "Restore a backup"
  (:require
   [re-mote.spec :refer (valid?)]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [clojure.edn :as edn]
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.restic :as restic]
   [re-mote.zero.datalog :as datalog]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets materialize-preset)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-flow.common :refer (run-?e run-?e-non-block results successful-ids)]
   [clara.rules :refer :all]))

(refer-timbre)

(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(derive ::start :re-flow.core/state)
(derive ::spec :re-flow.core/state)
(derive ::restoring :re-flow.core/state)
(derive ::validate :re-flow.core/state)
(derive ::volume-ready :re-flow.core/state)
(derive ::restored :re-flow.core/state)
(derive ::timedout :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(defn instance [{:keys [::size ::key]}]
  {:base kvm :args [defaults (local) c1-medium :restore (<< "restoring ~{key}") (kvm-volume size :restore)]})

(s/def ::backups string?)

(s/def ::key keyword?)

(s/def ::target string?)

(s/def ::size integer?)

(s/def ::timeout integer?)

(s/def ::restore
  (s/keys :req [::backups ::key ::target ::size ::timeout]))

(defrule check
  "Check that the fact is matching the ::restore spec"
  [?e <- ::start]
  =>
  (let [failed? (not (s/valid? ::restore ?e))]
    (info (expound/expound-str ::restore ?e))
    (insert! (assoc ?e :state ::spec :failure failed? :message (when failed? "Failed to validate restore spec")))))

(defrule create
  "Triggering the creation of the instance"
  [?e <- ::spec [{:keys [failure]}] (= failure false)]
  =>
  (info "starting to create system")
  (insert! (assoc ?e :state :re-flow.setup/creating :spec (instance ?e) :provision? true)))

(defrule check-volume
  "Check that our volume is ready and has enough capacity"
  [?e <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::restore) (= failure false)]
  =>
  (let [r (run-?e datalog/query ?e '[:find ?s :where [?e :disk-stores/name "/dev/vdb"] [?e :disk-stores/size ?s]])
        size (-> r results flatten first (/ (Math/pow 1024 3)) int)]
    (insert!
     (-> ?e
         (dissoc :message :failure)
         (assoc :state ::volume-ready :failure (and (= (successful-ids r) (?e :ids)) (= size (?e ::size))))))))

(defn restored? [m]
  (let [{:keys [code]} (-> m vals first)]
    (if-not (= code 0)
      (warn m)
      (debug m))
    (not= code 0)))

(defrule volume-ready
  "Trigger actual restore if all prequisits are met (volume is ready)"
  [?e <- ::volume-ready [{:keys [failure]}] (= failure false)]
  =>
  (let [backups (edn/read-string (slurp (?e ::backups)))]
    (info (<< "initiating the restoration process into ~(?e ::target)"))
    (run-?e-non-block restic/restore ?e ::restored [(?e ::timeout) :hour] restored? (backups (?e ::key)) (?e ::target))))

(defrule restoration-successful
  "Restoration was successful"
  [?e <- ::restored [{timeout :timeout failure :failure :or {timeout false failure false}}] (and (= timeout false) (= failure false))]
  =>
  (let [{:keys [::key]} ?e]
    (insert! (-> ?e (dissoc :timeout) (assoc :state ::done :message {:success (<< "Restoration of ~{key} was successful")} :subject "Restoration result")))
    (insert! (assoc ?e :state :re-flow.setup/cleanup))
    (info (<< "restoration of ~{key} was successful"))))

(defrule restoration-failed
  "Restoration failed"
  [?e <- ::restored [{:keys [failure timeout]}] (= failure true) (= timeout false)]
  =>
  (let [{:keys [::key]} ?e]
    (insert! (assoc ?e :state ::failed :message {:failure (<< "Restoration of ~{key} failed!")} :subject "Restoration result"))
    (info (<< "restoration of ~{key} failed!"))))

(defrule restoration-timeout
  "Processing the restoration result"
  [?e <- ::restored [{:keys [timeout failure]}] (= failure true) (= timeout true)]
  =>
  (let [{:keys [::key]} ?e]
    (insert! (assoc ?e :state ::timedout :message {:failure (<< "Restoration of ~{key} has timed out")} :subject "Restoration result"))
    (info (<< "restoration of ~{key} has timed out"))))
