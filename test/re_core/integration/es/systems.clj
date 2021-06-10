(ns re-core.integration.es.systems
  "Testing system searching"
  (:require
   [re-core.log :refer (setup-logging)]
   [es.common :as es :refer (types index)]
   [es.systems :as s]
   [rubber.core :refer (refresh-index)]
   [re-core.fixtures.data :refer (redis-kvm)]
   [re-core.integration.es.common :refer (re-initlize stop)])
  (:use clojure.test))

(setup-logging)

(defn puts
  "adds a list of systems into ES" []
  (s/put "1" (assoc redis-kvm :owner "admin"))
  (s/put "4" redis-kvm)
  (refresh-index (index :system)))

(defn total [res] (get-in res [:hits :total]))

(defn setup [f]
  (re-initlize true)
  (puts)
  (f)
  (stop))

(deftest basic-put- get
  (is (= :dev (get (s/get "1") :env))))

(deftest term-searching
  (is (= 2 (total (s/query {:bool {:must {:match {:machine.cpu 4}}}})))))

(deftest pagination
  (let [query {:bool {:must {:term {:machine.cpu 4}}}}
        {:keys [hits]} (s/query query :size 2 :from 1)]
    (is (= 1 (int (-> hits :hits count))))))

(use-fixtures :each setup)
