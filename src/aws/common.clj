(ns aws.common
  "Common functionality like connection settings"
  (:require
   [amazonica.aws.ec2 :as ec2]
   [re-core.model :refer (hypervisor)]))

(defn creds [] (dissoc (hypervisor :aws) :ostemplates))

(defn aws [endpoint]
  (assoc (creds) :endpoint endpoint))

(defmacro with-ctx
  "Run ec2 action with context (endpoint and creds)"
  [f & args]
  `(~f (assoc (creds) :endpoint ~'endpoint) ~@args))

(defn instance-desc [endpoint instance-id & ks]
  (->
   (with-ctx ec2/describe-instances {:instance-ids  [instance-id]})
   :reservations first :instances first (get-in ks)))

(defn image-id [machine]
  (hypervisor :aws :ostemplates (machine :os) :ami))
