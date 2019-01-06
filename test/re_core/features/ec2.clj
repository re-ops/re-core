(ns re-core.features.ec2
  "ec2 workflows"
  (:require
   [aws.common :refer (with-ctx instance-desc)]
   [es.systems :as s]
   [re-core.fixtures.data :refer (redis-type redis-ec2)]
   [re-core.integration.es.common :refer (populate-system)]
   [re-core.features.common :refer (spec get-spec)]
   [rubber.node :refer (stop)]
   [re-core.workflows :as wf])
  (:import clojure.lang.ExceptionInfo)
  (:use clojure.test))

(defn setup [f]
  (populate-system redis-type redis-ec2 "1")
  (f)
  (stop))

(deftest ec2
  (testing "aws creation"
    (is (nil? (wf/create (spec))))
    (is (thrown? ExceptionInfo (wf/create (spec))))
    (is (nil? (wf/stop (spec))))
    (is (thrown? ExceptionInfo (wf/create (spec))))
    (is (nil? (wf/destroy (spec)))))

  (testing "aws provisioning"
    (is (nil? (wf/create (spec))))
    (is (nil? (wf/reload (spec))))
    (is (nil? (wf/destroy (spec)))))

  (testing "vpc eip"
    (when-let [{:keys [eip]} (clojure.edn/read-string (System/getenv "EIP"))]
      (s/put 1 (spec {:machine {:ip eip :os :ubuntu-15.04}}))
      (is (nil? (wf/create (spec))))
      (is (= eip (:machine (spec))))
      (is (nil? (wf/stop (spec))))
      (is (nil? (wf/reload (spec))))
      (is (= eip (get (instance-desc (get-spec :aws :endpoint) (get-spec :aws :instance-id) :public-ip-address))))
      (is (nil? (wf/destroy (spec))))))

  (testing "aws with ebs volumes"
    (let [with-vol {:aws {:volumes [{:device "/dev/sdn" :size 10 :clear true :volume-type "standard"}]}}]
      (is (nil? (wf/create (spec with-vol))))
      (is (nil? (wf/reload (spec with-vol))))
      (is (nil? (wf/destroy (spec with-vol))))))

  (testing "aws with ephemeral volumes"
    (let [with-vol {:aws {:instance-type "c3.large" :block-devices [{:device-name "/dev/sdb" :virtual-name "ephemeral0"}]}}]
      (is (nil? (wf/create (spec with-vol))))
      (is (nil? (wf/reload (spec with-vol))))
      (is (nil? (wf/destroy (spec with-vol))))))

  (testing "aws with zone and security groups"
    (let [with-zone {:aws {:availability-zone "ap-southeast-2c" :security-groups ["test"]}}]
      (is (nil? (wf/create (spec with-zone))))
      (let [{:keys [placement security-groups]}
            (instance-desc (get-spec :aws :endpoint) (get-spec :aws :instance-id))]
        (is (= "ap-southeast-2c" (:availability-zone placement)))
        (is (= "test" (-> security-groups first :group-name))))
      (is (nil? (wf/reload (spec with-zone))))
      (is (nil? (wf/destroy (spec with-zone))))))

  (testing "aws clone workflows"
    (is (nil? (wf/create (spec))))
    (is (nil? (wf/clone {:system-id 1 :hostname "bar" :owner "ronen"})))
    (is (nil? (wf/destroy (assoc (s/get 2) :system-id 2))))
    (is (nil? (wf/destroy (spec)))))

  (testing "aws ebs-optimized"
    (is (nil? (wf/create
               (-> (spec)
                   (assoc-in [:aws :instance-type] "m4.large")
                   (assoc-in [:aws :ebs-optimized] true)))))
    (is (nil? (wf/destroy (spec))))))

(use-fixtures :once setup)
