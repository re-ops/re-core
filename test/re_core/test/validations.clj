(ns re-core.test.validations
  (:require
   [flatland.useful.map :refer (dissoc-in*)]
   [re-core.persistency
    [types :refer (validate-type)]]
   [re-core.model :refer (check-validity)]
   [aws.validations :as awsv]
   [re-core.fixtures.data :refer
    (redis-type user-quota redis-ec2-spec redis-physical)]
   [re-core.fixtures.core :refer (is-type? with-m?)])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo))

(fact "non puppet type"
      (validate-type {:type "foo" :puppet {:tar "bla"}}) => truthy)

(fact "aws volume validations"
  ; TODO this should fail! seems to be a subs issue
      (check-validity
       (merge-with merge redis-ec2-spec
                   {:aws {:volumes [{:device "/dev/sdg"}]}})) =>
      (throws ExceptionInfo
              (with-m?
                '{:aws {:volumes ({0 {:clear "must be present", :size "must be present", :volume-type "must be present"}})}}))

      (check-validity
       (merge-with merge redis-ec2-spec
                   {:aws {:volumes [{:device "/dev/sdb" :volume-type "gp2" :size 100 :clear true}]}})) => {}

      (check-validity
       (merge-with merge redis-ec2-spec
                   {:aws {:volumes [{:device "/dev/sda" :volume-type "io1" :iops 100 :size 10 :clear false}]}})) => {}

      (check-validity
       (merge-with merge redis-ec2-spec
                   {:aws {:volumes [{:device "/dev/sda" :volume-type "io1" :size 10 :clear false}]}})) =>
      (throws ExceptionInfo
              (with-m?  {:aws {:volumes '({0 "iops required if io1 type is used"})}})))

(fact "aws entity validations"
      (check-validity redis-ec2-spec) => {}

      (check-validity
       (merge-with merge redis-ec2-spec {:aws {:security-groups [1]}})) =>
      (throws ExceptionInfo (with-m? {:aws {:security-groups '({0 "must be a string"})}}))

      (check-validity
       (merge-with merge redis-ec2-spec {:aws {:availability-zone 1}})) =>
      (throws ExceptionInfo (with-m? {:aws {:availability-zone "must be a string"}})))

(fact "aws provider validation"
      (let [base {:aws {:instance-type "m1.small" :key-name "foo" :min-count 1 :max-count 1}}]

        (awsv/provider-validation base) => {}

        (awsv/provider-validation (merge-with merge base {:aws {:placement {:availability-zone "eu-west-1a"}}})) => {}

        (awsv/provider-validation (merge-with merge base {:aws {:placement {:availability-zone 1}}})) =>
        (throws ExceptionInfo
                (with-m? {:placement {:availability-zone "must be a string"}}))))

(fact "physical systmes validation"
      (check-validity redis-physical) => {}

      (check-validity (assoc-in redis-physical [:physical :mac] "aa:bb")) =>
      (throws ExceptionInfo (with-m? {:physical {:mac "must be a legal mac address"}}))

      (check-validity (assoc-in redis-physical [:physical :broadcast] "a.1.2")) =>
      (throws ExceptionInfo (with-m? {:physical {:broadcast "must be a legal ip address"}})))

