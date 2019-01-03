(ns re-core.test.validations
  (:require
   [flatland.useful.map :refer (dissoc-in*)]
   [re-core.model :refer (check-validity)]
   [aws.validations :as awsv]
   [re-core.fixtures.data :refer
    (redis-type redis-ec2-spec redis-physical)]
   [re-core.fixtures.core :refer (is-type? with-m?)])
  (:use clojure.test)
  (:import clojure.lang.ExceptionInfo))

(defn errors [f args]
  (try (f args)
       (throw (ex-info "expected an exception with info" {}))
       (catch ExceptionInfo e
         (-> e ex-data :errors))))

(deftest aws-validations
  (testing "aws volume validations"
  ; TODO this should fail! seems to be a subs issue
    (is (= '{:aws {:volumes ({0 {:clear "must be present", :size "must be present", :volume-type "must be present"}})}}
           (errors check-validity (merge-with merge redis-ec2-spec {:aws {:volumes [{:device "/dev/sdg"}]}}))))

    (is (= (check-validity
            (merge-with merge redis-ec2-spec {:aws {:volumes [{:device "/dev/sdb" :volume-type "gp2" :size 100 :clear true}]}})) {}))

    (is (= (check-validity
            (merge-with merge redis-ec2-spec
                        {:aws {:volumes [{:device "/dev/sda" :volume-type "io1" :iops 100 :size 10 :clear false}]}})) {}))

    (is (= {:aws {:volumes '({0 "iops required if io1 type is used"})}}
           (errors check-validity
                   (merge-with merge redis-ec2-spec {:aws {:volumes [{:device "/dev/sda" :volume-type "io1" :size 10 :clear false}]}})))))

  (testing "aws entity validations"
    (is (= {} (check-validity redis-ec2-spec)))

    (is (= {:aws {:security-groups '({0 "must be a string"})}}
           (errors check-validity (merge-with merge redis-ec2-spec {:aws {:security-groups [1]}}))))

    (is (= {:aws {:availability-zone "must be a string"}}
           (errors check-validity (merge-with merge redis-ec2-spec {:aws {:availability-zone 1}})))))

  (testing "aws provider validation"
    (let [base {:aws {:instance-type "m1.small" :key-name "foo" :min-count 1 :max-count 1}}]

      (is (= {} (awsv/provider-validation base)))

      (is (= {} (awsv/provider-validation (merge-with merge base {:aws {:placement {:availability-zone "eu-west-1a"}}}))))

      (is (= {:placement {:availability-zone "must be a string"}}
             (errors awsv/provider-validation (merge-with merge base {:aws {:placement {:availability-zone 1}}})))))))

(deftest physical-validations
  (is (= {} (check-validity redis-physical)))

  (is (= {:physical {:mac "must be a legal mac address"}}
         (errors check-validity (assoc-in redis-physical [:physical :mac] "aa:bb"))))

  (is (= {:physical {:broadcast "must be a legal ip address"}}
         (errors check-validity (assoc-in redis-physical [:physical :broadcast] "a.1.2")))))
