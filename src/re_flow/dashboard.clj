(ns re-flow.dashboard
  "Tilled Dashboard flow"
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

(s/def ::url string?)

(s/def ::user string?)

(s/def ::password string?)

(s/def ::id string?)

(s/def ::screen number?)

(s/def ::sleep number?)

(s/def ::login boolean?)

(s/def ::auth (s/keys :req-un [::user ::password]))

(s/def ::site (s/keys :req-un [::url ::auth ::screen ::sleep ::login]))

(s/def ::sites (s/coll-of ::site))

(s/def ::dashboard
  (s/keys :req-un [::sites ::id]))

(derive ::start :re-flow.core/state)
(derive ::spec :re-flow.core/state)
(derive ::launch :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(defrule check
  "Check that the fact is matching the ::dashboard spec"
  [?e <- ::start]
  =>
  (let [failed? (not (s/valid? ::dashboard ?e))]
    (info (expound/expound-str ::dashboard ?e))
    (insert! (assoc ?e :state ::spec :ids [(?e :id)] :failure failed? :message (when failed? "Failed to validate dashboard spec")))))

(defrule start
  "Triggering dashboard start"
  [?e <- ::spec [{:keys [failure]}] (= failure false)]
  =>
  (info "Starting to setup dashboard")
  (doseq [site (?e :sites)]
    (insert! (merge site (assoc ?e :state ::open)))))

(defn run-wait [k ?e arg t]
  (let [r (run k ?e arg)]
    (Thread/sleep t) r))

(defn actions [{:keys [user password]}]
  [[:type user 500]
   [:send-key [:tab] 500]
   [:type password 500]
   [:send-key [:tab] 500]
   [:send-key [:return] 1000]])

(defn run-auth
  "Run all auth steps, return false if any step failed"
  [{:keys [auth sleep] :as ?e}]
  (Thread/sleep (* 1000 sleep))
  (nil?
   (first
    (filter
     (comp not identity)
     (map
      (fn [[k arg t]] (failure? ?e (run-wait k ?e arg t))) (actions auth))))))

(defrule launch
  "Launch a site (open + login)"
  [?e <- ::open [{:keys [login]}] (= login true)]
  =>
  (info "Launching browser with url" (?e :url))
  (let [r0 (run :tile ?e)
        r1 (run :browse ?e (?e :url))
        r2 (run-auth ?e)
        r3 (run :send-key ?e [:alt :shift (str (?e :screen))])]
    (insert! (assoc ?e :state ::opened :failure (or r2 (failure? ?e r3) (failure? ?e r1) (failure? ?e r0))))))

(defrule open
  "Open a site (no login)"
  [?e <- ::open [{:keys [url login]}] (= login false)]
  =>
  (info "Open browser with url" (?e :url))
  (let [r1 (run :browse ?e (?e :url))
        r2 (run :send-key ?e [:alt :shift (str (?e :screen))])]
    (insert! (assoc ?e :state ::opened :failure (or (failure? ?e r1) (failure? ?e r2))))))
