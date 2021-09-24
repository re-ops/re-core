(ns re-flow.disposable
  "Disposable flows"
  (:require
   [expound.alpha :as expound]
   [re-flow.actions :refer (run)]
   [me.raynes.fs :as fs]
   [re-mote.spec :refer (valid?)]
   [clojure.spec.alpha :as s]
   [clojure.core.strint :refer (<<)]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets materialize-preset)]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(refer-kvm-presets)
(refer-system-presets)
(refer-instance-types)

(defn url? [s]
  (try
    (not (nil? (.toURI (java.net.URL. s))))
    (catch Exception e
      false)))

(derive ::start :re-flow.core/state)
(derive ::spec :re-flow.core/state)
(derive ::match :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(defn instance [{:keys [::target ::key]}]
  {:base kvm :args [default-machine large local (os :ubuntu-desktop-20.04) :disposable (<< "dispoable ~{target}")]})

(def supported-extensions #{".html" ".pdf" ".docx" ".doc" ".odt"})

(s/def ::target string?)

(s/def ::disposable
  (s/keys :req-un [::target]))

(defrule check
  "Check that the fact is matching the ::disposable spec"
  [?e <- ::start]
  =>
  (let [failed? (not (s/valid? ::disposable ?e))]
    (info (expound/expound-str ::disposable ?e))
    (insert! (assoc ?e :state ::spec :failure failed? :message (when failed? "Failed to validate disposable spec")))))

(defrule create
  "Triggering the creation of the dispoable instance"
  [?e <- ::spec [{:keys [failure]}] (= failure false)]
  =>
  (info "Starting to setup dispoable instance")
  (insert! (assoc ?e :state ::match ::url? (url? (?e :target)) ::file? (fs/exists? (?e :target))))
  (insert! (assoc ?e :state :re-flow.setup/creating :spec (instance ?e))))

(defrule open-url
  [?t <- ::match [{:keys [url?]}] (= url? true)]
  [?e <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::disposable) (= failure false)]
  =>
  (info "Launching browser with url")
  (run :browse ?e (?t :target)))

(defrule upload-file
  [?e <- :re-flow.setup/provisioned [{:keys [flow failure]}] (= flow ::disposable) (= failure false)]
  [?t <- ::match [{:keys [file?]}] (= file? true)]
  =>
  (info "uploading file to target host"))
