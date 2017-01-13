(ns celestial.integration.persistency.persmissions
  "System access permissions and envs"
  (:refer-clojure :exclude [type])
  (:import clojure.lang.ExceptionInfo)
  (:require
    [flatland.useful.map :refer (dissoc-in*)]
    [celestial.security :refer (current-user)]
    [celestial.fixtures.data :refer (redis-ec2-spec redis-ec2-spec redis-type)]
    [celestial.fixtures.core :refer (is-type? with-conf)]
    [celestial.fixtures.populate :refer (add-users re-initlize)]
    [celestial.persistency.types :as t]
    [celestial.persistency.systems :as s])
  (:use midje.sweet))

(with-conf
  (against-background
    [(current-user) => {:identity "admin", :roles #{:celestial.roles/admin}, :username "admin"}]
    (with-state-changes [(before :facts (do (re-initlize) (add-users) (t/add-type redis-type)))]
      (fact "type and host sanity" :integration :redis :systems
            (let [id (s/add-system redis-ec2-spec)]
              (t/get-type "redis") => redis-type
              (s/get-system id) => redis-ec2-spec
              (provided
                (current-user) => {:identity "admin", :roles #{:celestial.roles/admin} :username "admin"})))

      (fact "host update" :integration  :redis :systems
            (let [id (s/add-system redis-ec2-spec)]
              (s/update-system id (assoc redis-ec2-spec :foo 2))
              (:foo (s/get-system id)) => 2))

      (fact "simple clone" :integration :redis :systems
            (let [spec {:hostname "bar" :owner "admin"}
                  id (s/add-system redis-ec2-spec)
                  cloned (s/clone-system id spec)]
              (s/get-system cloned) =>
                 (-> redis-ec2-spec
                   (assoc-in [:machine :hostname] "bar"))))

      #_(fact "persmissionless access" :integration :redis :systems
            (let [id (s/add-system (assoc redis-ec2-spec :env :prod))]
             (s/get-system id) =>
               (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-env-violation))
              ; note that the assert-access precondition adds a current-user call
              (provided (current-user) => {:username "ronen"} :times 2)
              (s/update-system id redis-ec2-spec) =>
                 (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-env-violation))
              (provided (current-user) => {:username "ronen"} :times 4)
              (s/delete-system id) =>
                  (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-env-violation))
              (provided (current-user) => {:username "ronen"} :times 2)
              (s/add-system {:env :prod :owner "ronen"}) =>
                (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-env-violation))
              (provided (current-user) => {:username "ronen"} :times 2)
              ))

      #_(fact "with persmission" :integration :redis :systems
        (let [id (s/add-system redis-ec2-spec)]
          (s/get-system id) => redis-ec2-spec
          (provided (current-user) => {:username "ronen"} :times 2)
          (s/update-system id (assoc redis-ec2-spec :cpus 20)) => (contains ["OK"])
          (provided (current-user) => {:username "ronen"} :times 12)
          (s/delete-system id) => [1 1 1 1]
          (provided (current-user) => {:username "ronen"} :times 6)
          (s/add-system redis-ec2-spec) => truthy
          (provided (current-user) => {:username "ronen"} :times 6)))

      #_(fact "user trying to create on another username" :integration :redis :systems
         (s/add-system (assoc redis-ec2-spec :owner "foo")) =>
           (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-owner-violation))
         (provided
           (current-user) => {:username "ronen"} :times 2)
         (s/add-system (assoc redis-ec2-spec :user "ronen")) => 1
         (provided
           (current-user) => {:username "admin"} :times 6))

      #_(fact "partial update" :integration :redis :systems
         (let [id (s/add-system (assoc redis-ec2-spec :owner "admin"))]
           (s/partial-system id {:machine {:ip "1.2.3.4"}}) => (contains ["OK"])
           (provided (current-user) => {:username "admin"} :times 12)
           (s/partial-system id {:machine {:ip "1.2.3.4"}}) =>
             (throws ExceptionInfo (is-type? :celestial.persistency.systems/persmission-owner-violation))
           (provided (current-user) => {:username "ronen"} :times 2)))

      )))

