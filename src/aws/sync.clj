(ns aws.sync
  "EC2 sync support"
  (:require
   [amazonica.aws.ec2 :as ec2]
   [aws.common :refer (aws)]))

(defn tagged-instances
  "get instances taged with re-ops"
  [endpoint]
  (ec2/describe-instances (aws endpoint)
                          {:filters [{:name "tag:re-ops" :values ["true"]}]}))

(defn tag-instance [instance-id endpoint]
  (ec2/create-tags (aws endpoint) {:resources [instance-id] :tags [{:key "re-ops" :value "true"}]}))
