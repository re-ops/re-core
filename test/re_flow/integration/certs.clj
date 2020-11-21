(ns re-flow.integration.certs
  (:require
   [me.raynes.fs :refer (delete-dir)]
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.resources.file :refer (file directory)]
   [re-core.presets.systems :refer (kvm defaults local)]
   [re-core.presets.instance-types :refer (large)]
   [re-flow.pubsub :refer (subscribe-?e)]
   [re-flow.core :refer (trigger)]
   [taoensso.timbre :refer (info)]
   [re-core.repl :refer :all]
   [re-flow.session :refer (update-)]
   [re-core.repl.results :as results]
   [clojure.core.async :refer (chan <!! <! go timeout alts!! close!)]
   [re-flow.common :refer (run-?e failure? successful-ids hosts-results* create-fact)])
  (:use clojure.test))

(defn cert-files
  "Creating cert files to pull"
  [{:keys [ids]} f]
  (hosts-results*
   (run-hosts (hosts (with-ids ids) :hostname) file [f :present] [1 :second])))

(defn cert-directory
  "Creating cert containing directory"
  [{:keys [ids]} d]
  (hosts-results*
   (run-hosts (hosts (with-ids ids) :hostname) directory [d :present] [1 :second])))

(def five-min (* 5 60 1000))

(defn setup
  "This test requires an up and running system"
  [f]
  (let [output (subscribe-?e :re-flow.setup/provisioned (chan))]
    (trigger
     (create-fact kvm defaults local large :letsencrypt "cert generation"))
    (let [[?e c] (alts!! [output (timeout five-min)])]
      (try
        (if (= c output)
          (f)
          (throw (ex-info "Failed to create target system" {})))
        (finally
          (close! output)
          (destroy (matching (results/*1)) {:force true})
          (delete-dir "/tmp/certs/"))))))

(defn is-success
  [result {:keys [ids]}]
  (let [systems (:systems (second (list (with-ids ids) :systems :print? false)))
        names (mapv (fn [[_ m]] (get-in m [:machine :hostname])) systems)]
    (is (= names result))))

(deftest cert-distribution
  (let [domain "example.com"
        parent "/srv/dehydrated/certs/"
        files []
        ?e {:ids [(results/*1)]}]
    (is-success (cert-directory ?e parent) ?e)
    (is-success (cert-directory ?e (<< "~{parent}/~{domain}")) ?e)
    (is-success (cert-files ?e (<< "~{parent}/~{domain}/privkey.pem")) ?e)
    (is-success (cert-files ?e (<< "~{parent}/~{domain}/cert.csr")) ?e)
    (let [output (subscribe-?e :re-flow.certs/copied (chan))]
      (update- [(assoc ?e :state :re-flow.certs/renewed :flow :re-flow.certs/certs :failure false :domains [domain])])
      (let [[?e c] (alts!! [output (timeout (* 30 1000))])]
        (is (= c output))
        (is (= (?e :failure) false))
        (close! output)))))

(use-fixtures :once setup)
