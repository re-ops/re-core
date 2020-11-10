(ns re-flow.integration.certs
  (:require
   re-flow.setup
   re-flow.certs
   [re-core.repl :refer :all]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.resources.file :refer (file)]
   [re-core.presets.systems :refer (kvm defaults local)]
   [re-core.presets.instance-types :refer (large)]
   [re-flow.core :refer (trigger)]
   [re-flow.session :refer (fact-type session)]
   [taoensso.timbre :refer (info)]
   [clara.rules :refer :all]
   [re-flow.common :refer (run-?e failure? successful-ids hosts-results*)]
   [re-flow.common :refer (create-fact)])
  (:use clojure.test))

(defn plugged-session
  "A session that is plugged to capture testing related facts"
  []
  (mk-session
   're-flow.setup 're-flow.certs 're-flow.integration.certs :fact-type-fn fact-type :cache false))

(def fact (promise))

(defrule domains
  "Setup the domains we will generate certs for"
  [?e <- :re-flow.setup/provisioned [{:keys [failure]}] (= failure false)]
  =>
  (let [{:keys [ids]} ?e
        r (hosts-results* (run-hosts (hosts (with-ids ids) :hostname) file ["/tmp/1" :present] [1 :second]))]
    (deliver fact r)))

(defn setup
  "This test assumes an up an running system"
  [f]
  (reset! session (plugged-session))
  (f))

(deftest cert-distribution
  (trigger (create-fact kvm defaults local large :letsencrypt "cert generation"))
  (let [r (deref fact)]
    (info "got ready system" r)))

(use-fixtures :once setup)
