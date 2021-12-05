(ns re-core.test.kvm
  (:require
   [re-core.fixtures.data :refer [redis-kvm volume]]
   [re-core.model :refer (vconstruct)])
  (:use clojure.test))

(def expected-domain
  {:user "re-ops" :name "red1" :hostname "red1" :fqdn "red1.local" :cpu 4 :ram 1
   :image {:flavor :debian :template "ubuntu-server-20.04_corretto-8"}})

(deftest kvm-sanity
  (testing "legal instance spec"
    (let [domain (vconstruct (assoc redis-kvm :system-id "1"))]
      (is (= "1" (:system-id domain)))
      (is (= {:user "ronen" :name :localhost :host "localhost" :port 22} (:node domain)))
      (is (= expected-domain (dissoc (:domain domain) :description)))))
  (testing "volume pool"
    (let [with-vol (assoc-in redis-kvm [:kvm :volumes] [volume])
          domain (vconstruct (assoc with-vol :system-id "1"))]
      (is (= (assoc volume :pool {:id "default" :path "/var/lib/libvirt/images/"})
             (first (:volumes domain)))))))
