(ns re-flow.dashboard
  "Tilled Dashboard flow"
  (:require
   [re-share.config.core :refer (get!)]
   [re-mote.repl.zero.desktop :refer (cycle-screens)]
   [re-core.repl :refer (hosts matching)]
   [expound.alpha :as expound]
   [re-flow.actions :refer (run)]
   [clojure.spec.alpha :as s]
   [clojure.core.strint :refer (<<)]
   [re-core.repl :refer (spice-into with-ids)]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [re-flow.common :refer (into-ids with-fails)]
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

(s/def ::sites (s/coll-of ::site :kind vector?))

(s/def ::dashboard
  (s/keys :req-un [::sites ::id]))

(derive ::start :re-flow.core/state)
(derive ::spec :re-flow.core/state)
(derive ::launch :re-flow.core/state)
(derive ::open :re-flow.core/state)
(derive ::opened :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(defrule check
  "Check that the fact is matching the ::dashboard spec"
  [?e <- ::start]
  =>
  (let [failed? (not (s/valid? ::dashboard ?e))]
    (when failed?
      (info (expound/expound-str ::dashboard ?e)))
    (insert! (assoc ?e :state ::spec :ids [(?e :id)] :failure failed? :message (when failed? "Failed to validate dashboard spec")))))

(defrule start
  "Triggering dashboard start"
  [?e <- ::spec [{:keys [failure]}] (= failure false)]
  =>
  (info "Starting to setup dashboard")
  (let [r (run :tile ?e)]
    (insert!
     (with-fails (assoc ?e :state ::open :site (peek (?e :sites)) :sites (pop (?e :sites))) [r]))))

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
  [{:keys [site] :as ?e}]
  (let [{:keys [auth sleep]} site]
    (Thread/sleep (* 1000 sleep))
    (mapv
     (fn [[k arg t]] (run-wait k ?e arg t)) (actions auth))))

(defrule launch
  "Launch a site (open + login)"
  [?e <- ::open [{:keys [site failure]}] (= (site :login) true) (= failure false)]
  =>
  (info "Launching browser with url" (-> ?e :site :url))
  (let [{:keys [site]} ?e
        r1 (run :browse ?e (site :url))
        r2 (run-auth ?e)
        r3 (run :send-key ?e [:alt :shift (str (site :screen))])
        ?e' (with-fails (assoc ?e :state ::opened) (into [r1 r3] r2))]
    (insert! ?e')))

(defrule open
  "Open a site (no login)"
  [?e <- ::open [{:keys [site failure]}] (= (site :login) false) (= failure false)]
  =>
  (let [{:keys [site]} ?e
        {:keys [url auth screen]} site]
    (info "Open browser with url" url)
    (let [r1 (run :browse ?e url)
          r2 (run :send-key ?e [:alt :shift (str screen)])
          ?e' (with-fails (assoc ?e :state ::opened) [r1 r2])]
      (insert! ?e'))))

(defrule next-
  "We open the next website if available"
  [?e <- ::opened [{:keys [failure sites]}] (= failure false) (= (empty? sites) false)]
  =>
  (info "Opening next site")
  (insert! (assoc ?e :state ::open :site (peek (?e :sites)) :sites (pop (?e :sites)))))

(defrule done
  "Dashboard setup is done"
  [?e <- ::opened [{:keys [failure sites]}] (= failure false) (= (empty? sites) true)]
  =>
  (info "Dashboard setup is done setting up cycle")
  (cycle-screens
   (hosts (matching (?e :id)) :hostname) 5 20 (keyword "dashboard" (-> ?e :system :machine :hostname)))
  (insert! (assoc ?e :state ::done :site nil :sites [])))

(defrule halt
  "In case we failed to open any url we skip opening any additional urls"
  [?e <- ::opened [{:keys [failure]}] (= failure true)]
  =>
  (info "Failed to launch dashboard!")
  (insert! (assoc ?e :state ::failed :failure true)))

(defrule dashboard-bootstrap
  "Trigger dashboard cycle"
  [?e <- :re-flow.react/typed [{:keys [system]}] (= (system :type) :dashboard)]
  =>
  (let [sites (clojure.edn/read-string (slurp (get! :sites)))
        r (run :kill ?e "chrome")]
    (insert!
     (merge sites (assoc ?e :state ::start)))))
