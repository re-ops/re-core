(ns aws.sync
  "EC2 sync support")

(defn tagged-instances
  "get instances taged with re-ops"
  [c])

#_(defn tag-instance []
    (with-ctx ec2/create-tags
      {:resources [instance-id] :tags [{:key "Name" :value hostname}]}))
