(ns re-core.test.kvm
  (:require
   [re-core.fixtures.core :as f :refer (with-dev)]
   [re-core.fixtures.data :refer [redis-kvm]]
   [re-core.model :refer (vconstruct)])
  (:use clojure.test))

(def expected-domain
  {:user "re-ops" :name "red1" :hostname "red1"
   :image {:flavor :debian :template "ubuntu-16.04"}
   :cpu 2 :ram 1024})

(deftest kvm-sanity
  (testing "legal instance spec"
    (with-dev
      (let [domain (vconstruct (assoc redis-kvm :system-id "1"))]
        (is (= "1" (:system-id domain)))
        (is (= {:user "ronen" :host "localhost" :port 22} (:node domain)))
        (is (= expected-domain (:domain domain))))))
  (testing "volume pool"
    (with-dev
      (let [with-vol (assoc-in redis-kvm [:kvm :volumes] [volume])
            domain (vconstruct (assoc with-vol :system-id "1"))]
        (is (= (assoc volume :pool {:id "default" :path "/var/lib/libvirt/images/"})
               (first (:volumes domain))))))))
