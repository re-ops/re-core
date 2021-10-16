(ns re-flow.dashboard
  "Tilled Dashboard flow"
  (:require
   [re-share.config.core :refer (get!)]
   [re-mote.repl.zero.desktop :refer (cycle-screens halt-cycle)]
   [re-core.repl :refer (hosts matching)]
   [expound.alpha :as expound]
   [re-flow.actions :refer (run)]
   [clojure.spec.alpha :as s]
   [clojure.core.strint :refer (<<)]
   [re-core.repl :refer (spice-into with-ids)]
   [re-core.presets.kvm :refer (refer-kvm-presets)]
   [re-core.presets.instance-types :refer (refer-instance-types)]
   [re-core.presets.systems :refer (refer-system-presets)]
   [re-flow.common :refer (into-ids with-fails failure?)]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(s/def ::url string?)

(s/def ::user string?)

(s/def ::password string?)

(s/def ::ids (s/coll-of string?))

(s/def ::screen number?)

(s/def ::sleep number?)

(s/def ::login boolean?)

(s/def ::auth (s/keys :req-un [::user ::password]))

(s/def ::site (s/keys :req-un [::url ::auth ::screen ::sleep ::login]))

(s/def ::sites (s/coll-of ::site :kind vector?))

(s/def ::dashboard
  (s/keys :req-un [::sites ::ids]))

(derive ::start :re-flow.core/state)
(derive ::spec :re-flow.core/state)
(derive ::cache :re-flow.core/state)
(derive ::launch :re-flow.core/state)
(derive ::open :re-flow.core/state)
(derive ::opened :re-flow.core/state)
(derive ::failed :re-flow.core/state)
(derive ::done :re-flow.core/state)

(defrule check
  "Check that the fact is matching the ::dashboard spec"
  [?e <- ::start [{:keys [failure]}] (or (nil? failure) (= failure false))]
  =>
  (let [failed? (not (s/valid? ::dashboard ?e))]
    (when failed?
      (info (expound/expound-str ::dashboard ?e)))
    (insert! (assoc ?e :state ::spec :failure failed? :message (when failed? "Failed to validate dashboard spec")))))

(defrule start
  "Triggering dashboard start"
  [?e <- ::spec [{:keys [failure]}] (= failure false)]
  =>
  (info "Starting to setup dashboard")
  (let [r (run :tile ?e)
        sites (into [] (reverse (?e :sites)))]
    (insert!
     (with-fails (assoc ?e :state ::open :sites sites :site (peek sites) :sites (pop sites)) [r]))))

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

(defn cycle-key [?e]
  (keyword "dashboard" (-> ?e :system :machine :hostname)))

(defrule done
  "Dashboard setup is done"
  [?e <- ::opened [{:keys [failure sites]}] (= failure false) (= (empty? sites) true)]
  =>
  (info "Dashboard setup is done setting up cycle")
  (cycle-screens
   (hosts (with-ids (?e :ids)) :hostname) 5 20 (cycle-key ?e))
  (insert! (assoc ?e :state ::done :site nil :sites [])))

(defrule halt
  "In case we failed to open any url we skip opening any additional urls"
  [?e <- ::opened [{:keys [failure]}] (= failure true)]
  =>
  (info "Failed to launch dashboard!")
  (insert! (assoc ?e :state ::failed :failure true)))

(defrule chromium-cache
  [?e <- :re-flow.react/typed [{:keys [system]}] (= (system :type) :dashboard) (= (-> system :machine :os) :debian-10.0)]
  =>
  (let [user (-> ?e :system :machine :user)]
    (insert!
     {:state ::cache :path (<< "/home/~{user}/snap/chromium/common/chromium/Default")})))

(defrule chrome-cache
  [?e <- :re-flow.react/typed [{:keys [system]}] (= (system :type) :dashboard) (= (-> system :machine :os) :ubuntu-desktop-20.04)]
  =>
  (let [user (-> ?e :system :machine :user)]
    (insert!
     {:state ::cache :path (<< "/home/~{user}/.config/google-chrome/Default")})))

(defrule dashboard-bootstrap
  "Trigger dashboard cycle"
  [?e <- :re-flow.react/typed [{:keys [system]}] (= (system :type) :dashboard)]
  [?c <- ::cache]
  =>
  (let [sites (clojure.edn/read-string (slurp (get! :sites)))
        user (-> ?e :system :machine :user)
        _ (run :kill ?e "chrome")
        ; clearing existing passwords cache
        r2 (run :rmdir ?e (?c :path))]
    (insert!
     (merge sites (assoc ?e :state ::start :failure (failure? ?e r2))))))

(defrule dashboard-teardown
  "Trigger dashboard cycle"
  [?e <- :re-flow.react/cleanup [{:keys [system]}] (= (system :type) :dashboard)]
  =>
  (halt-cycle (cycle-key ?e)))
