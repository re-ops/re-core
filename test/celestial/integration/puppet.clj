(ns celestial.integration.puppet
  "Requires both kvm redis and a celetial instance to reply to puppet ext queries"
  (:require
    [celestial.persistency.systems :as s]
    [celestial.persistency.types :as t]
    [celestial.fixtures.core :refer (with-defaults) :as f]
    [celestial.fixtures.populate :refer (add-users)]
    [celestial.fixtures.data :as d]
    [celestial.workflows :refer (reload provision destroy)]
    [celestial.redis :refer (clear-all)]
    [celestial.config :refer (path)])
  (:use midje.sweet))

(defn run-cycle [spec type]
  (clear-all)
  (add-users)
  (t/add-type type)
  (let [id (s/add-system spec)]
    (try
      (reload (assoc spec :system-id id))
      (provision type (s/get-system id))
      (finally
        (destroy (assoc (s/get-system id) :system-id id))))))

(with-defaults
  (fact "provisioning a kvm instance" :integration :puppet :kvm
        (run-cycle d/redis-kvm-spec d/redis-type)))
