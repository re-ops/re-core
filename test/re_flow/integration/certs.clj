(ns re-flow.integration.certs
  "In this test we check that our renewl process works correctly, We don't run the actual renewl in order to not trigger rate limits, instead we create empty cert files and trigger the rules to see that the system functions correctly.

   We \"generate\" the certs and distribute them into the same instance in order to simplify our code."
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
    (let [[_ c] (alts!! [output (timeout five-min)])]
      (try
        (if (= c output)
          (f)
          (throw (ex-info "Failed to create target system" {})))
        (finally
          (close! output)
          (destroy (matching (results/*1)) {:force true})
          (delete-dir "/tmp/certs/"))))))

(defn is-success
  "Assert that a hosts-results are successful"
  [result {:keys [ids]}]
  (let [systems (:systems (second (list (with-ids ids) :systems :print? false)))
        names (mapv (fn [[_ m]] (get-in m [:machine :hostname])) systems)]
    (is (= names result))))

(defn is-sub-success
  "Checking that a subscription channel didn't time out and that ?e didn't fail"
  [input-c wait]
  (let [[?e c] (alts!! [input-c (timeout (* 10 1000))])]
    (is (= c input-c))
    (is (and (not (nil? ?e)) (= (?e :failure) false)))
    (close! c)))

(deftest cert-distribution
  (let [domain "example.com"
        domains {domain {:id (results/*1) :dest "/tmp/"}}
        parent "/srv/dehydrated/certs/"
        ?e {:ids [(results/*1)]}]
    (is-success (cert-directory ?e parent) ?e)
    (is-success (cert-directory ?e (<< "~{parent}/~{domain}")) ?e)
    (is-success (cert-files ?e (<< "~{parent}/~{domain}/privkey.pem")) ?e)
    (is-success (cert-files ?e (<< "~{parent}/~{domain}/cert.pem")) ?e)
    (let [downloads (subscribe-?e :re-flow.certs/downloaded (chan))
          uploads (subscribe-?e :re-flow.certs/delivered (chan))]
      (update- [(assoc ?e :state :re-flow.certs/renewed :flow :re-flow.certs/certs
                       :failure false :domains domains :intermediary "/tmp/certs")])
      (is-sub-success downloads (* 10 1000))
      (is-sub-success uploads (* 10 1000)))))

(use-fixtures :once setup)
