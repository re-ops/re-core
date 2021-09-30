(ns re-flow.disposable
  "Disposable flow"
  (:require
   [clojure.java.io :refer (file)]
   [expound.alpha :as expound]
   [re-flow.actions :refer (run)]
   [me.raynes.fs :as fs]
   [re-mote.spec :refer (valid?)]
   [clojure.spec.alpha :as s]
   [clojure.core.strint :refer (<<)]
   [re-core.repl :refer (spice-into with-ids)]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [re-flow.common :refer (failure? into-ids)]
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
(derive ::uploaded :re-flow.core/state)
(derive ::opened :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(defn instance [{:keys [::target ::key]}]
  {:base kvm :args [default-machine large local (os :ubuntu-desktop-20.04) :disposable (<< "dispoable ~{target}")]})

(def supported-extensions #{"html" "pdf" "docx" "doc" "odt"})

(s/def ::target string?)

(s/def ::disposable
  (s/keys :req-un [::target] :opt-un [::ext]))

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
  (insert! (assoc ?e :state ::match :url? (url? (?e :target)) :file? (fs/file? (?e :target))))
  (insert! (assoc ?e :state :re-flow.setup/creating :provision? false :spec (instance ?e))))

(defrule open-url
  [?e <- :re-flow.setup/registered [{:keys [flow failure]}] (= flow ::disposable) (= failure false)]
  [?t <- ::match [{:keys [url? target]}] (= url? true) (= target (?e :target))]
  =>
  (info "Launching browser with url" (?e :target))
  (let [r (run :browse ?e (?e :target))]
    (insert! (assoc ?e :state ::opened :failure (failure? ?e r)))))

(defn browse-type [ext]
  (when (#{".html" ".pdf"} ext) true))

(defn doc-type [ext]
  (when (#{".odt" ".doc" ".docx"} ext) true))

(defrule upload-file
  [?e <- :re-flow.setup/registered [{:keys [flow failure]}] (= flow ::disposable) (= failure false)]
  [?t <- ::match [{:keys [file? target]}] (= file? true) (= target (?e :target))]
  =>
  (let [{:keys [target]} ?e
        base (fs/base-name target)
        ext (fs/extension target)]
    (info (<< "Uploading ~{base} to host"))
    (let [r (run :upload ?e target (<< "/tmp/~{base}"))]
      (insert! (assoc ?e :state ::uploaded :browser? (browse-type ext) :writer? (doc-type ext) :remote-target (<< "/tmp/~{base}") :failure (failure? ?e r))))))

(defrule browser-supported
  [?e <- ::uploaded [{:keys [browser?]}] (= browser? true)]
  =>
  (info "Launching browser with file" (?e :remote-target))
  (let [r (run :browse ?e (?e :remote-target))]
    (insert! (assoc ?e :state ::opened :failure (failure? ?e r)))))

(defrule writer
  [?e <- ::uploaded [{:keys [writer?]}] (= writer? true)]
  =>
  (info "Launching writer with file" (?e :target))
  (let [r (run :writer ?e (?e :remote-target))]
    (insert! (assoc ?e :state ::opened :failure (failure? ?e r)))))

(defn run-spice [ids]
  (spice-into (with-ids ids)))

(defrule opened
  [?e <- ::opened [{:keys [failure]}] (= failure false)]
  =>
  (info "spice into" (?e :ids))
  (run-spice (?e :ids)))
