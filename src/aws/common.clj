(ns aws.common
  "Common functionality like connection settings"
  (:require
    [aws.sdk.ec2 :as ec2]
    [celestial.model :refer (hypervisor)]  
    ))

(defn creds [] (dissoc (hypervisor :aws) :ostemplates))

(defmacro with-ctx
  "Run ec2 action with context (endpoint and creds)"
  [f & args]
  `(~f (assoc (creds) :endpoint ~'endpoint) ~@args))

(defn instance-desc [endpoint instance-id & ks]
  (-> (with-ctx ec2/describe-instances (ec2/instance-id-filter instance-id))
      first :instances first (get-in ks)))
