(ns re-core.presets.instance-types
  "Instance to RAM/CPU mappings based out of:
     https://github.com/dustinkirkland/instance-type/blob/master/yaml/aws.yaml
  ")

(def instances {:c1.medium {:cpu 2.0 :ram 1.7}
                :c1.xlarge  {:cpu 8.0 :ram 7.0}
                :c3.2xlarge  {:cpu 8.0 :ram 15.0}
                :c3.4xlarge  {:cpu 16.0 :ram 30.0}
                :c3.8xlarge  {:cpu 32.0 :ram 60.0}
                :c3.large  {:cpu 2.0 :ram 3.75}
                :c3.xlarge  {:cpu 4.0 :ram 7.5}
                :c4.2xlarge  {:cpu 8.0 :ram 15.0}
                :c4.4xlarge  {:cpu 16.0 :ram 30.0}
                :c4.8xlarge  {:cpu 36.0 :ram 60.0}
                :c4.large  {:cpu 2.0 :ram 3.75}
                :c4.xlarge  {:cpu 4.0 :ram 7.5}
                :c5.18xlarge  {:cpu 72.0 :ram 144.0}
                :c5.2xlarge  {:cpu 8.0 :ram 16.0}
                :c5.4xlarge  {:cpu 16.0 :ram 32.0}
                :c5.9xlarge  {:cpu 36.0 :ram 72.0}
                :c5.large  {:cpu 2.0 :ram 4.0}
                :c5.xlarge  {:cpu 4.0 :ram 8.0}
                :cc2.8xlarge  {:cpu 32.0 :ram 60.5}
                :cg1.4xlarge  {:cpu 16.0 :ram 22.5}
                :cr1.8xlarge  {:cpu 32.0 :ram 244.0}
                :d2.2xlarge  {:cpu 8.0 :ram 61.0}
                :d2.4xlarge  {:cpu 16.0 :ram 122.0}
                :d2.8xlarge  {:cpu 36.0 :ram 244.0}
                :d2.xlarge  {:cpu 4.0 :ram 30.5}
                :f1.16xlarge  {:cpu 64.0 :ram 976.0}
                :f1.2xlarge  {:cpu 8.0 :ram 122.0}
                :g2.2xlarge  {:cpu 8.0 :ram 15.0}
                :g2.8xlarge  {:cpu 32.0 :ram 60.0}
                :g3.16xlarge  {:cpu 64.0 :ram 488.0}
                :g3.4xlarge  {:cpu 16.0 :ram 122.0}
                :g3.8xlarge  {:cpu 32.0 :ram 244.0}
                :hi1.4xlarge  {:cpu 16.0 :ram 60.5}
                :hs1.8xlarge  {:cpu 16.0 :ram 117.0}
                :i2.2xlarge  {:cpu 8.0 :ram 61.0}
                :i2.4xlarge  {:cpu 16.0 :ram 122.0}
                :i2.8xlarge  {:cpu 32.0 :ram 244.0}
                :i2.xlarge  {:cpu 4.0 :ram 30.5}
                :i3.16xlarge  {:cpu 64.0 :ram 488.0}
                :i3.2xlarge  {:cpu 8.0 :ram 61.0}
                :i3.4xlarge  {:cpu 16.0 :ram 122.0}
                :i3.8xlarge  {:cpu 32.0 :ram 244.0}
                :i3.large  {:cpu 2.0 :ram 15.25}
                :i3.xlarge  {:cpu 4.0 :ram 30.5}
                :m1.large  {:cpu 2.0 :ram 7.5}
                :m1.medium  {:cpu 1.0 :ram 3.75}
                :m1.small  {:cpu 1.0 :ram 1.7}
                :m1.xlarge  {:cpu 4.0 :ram 15.0}
                :m2.2xlarge  {:cpu 4.0 :ram 34.2}
                :m2.4xlarge  {:cpu 8.0 :ram 68.4}
                :m2.xlarge  {:cpu 2.0 :ram 17.1}
                :m3.2xlarge  {:cpu 8.0 :ram 30.0}
                :m3.large  {:cpu 2.0 :ram 7.5}
                :m3.medium  {:cpu 1.0 :ram 3.75}
                :m3.xlarge  {:cpu 4.0 :ram 15.0}
                :m4.10xlarge  {:cpu 40.0 :ram 160.0}
                :m4.16xlarge  {:cpu 64.0 :ram 256.0}
                :m4.2xlarge  {:cpu 8.0 :ram 32.0}
                :m4.4xlarge  {:cpu 16.0 :ram 64.0}
                :m4.large  {:cpu 2.0 :ram 8.0}
                :m4.xlarge  {:cpu 4.0 :ram 16.0}
                :p2.16xlarge  {:cpu 64.0 :ram 732.0}
                :p2.8xlarge  {:cpu 32.0 :ram 488.0}
                :p2.xlarge  {:cpu 4.0 :ram 61.0}
                :r3.2xlarge  {:cpu 8.0 :ram 61.0}
                :r3.4xlarge  {:cpu 16.0 :ram 122.0}
                :r3.8xlarge  {:cpu 32.0 :ram 244.0}
                :r3.large  {:cpu 2.0 :ram 15.25}
                :r3.xlarge  {:cpu 4.0 :ram 30.5}
                :r4.16xlarge  {:cpu 64.0 :ram 488.0}
                :r4.2xlarge  {:cpu 8.0 :ram 61.0}
                :r4.4xlarge  {:cpu 16.0 :ram 122.0}
                :r4.8xlarge  {:cpu 32.0 :ram 244.0}
                :r4.large  {:cpu 2.0 :ram 15.25}
                :r4.xlarge  {:cpu 4.0 :ram 30.5}
                :t1.micro  {:cpu 1.0 :ram 0.613}
                :t2.2xlarge  {:cpu 8.0 :ram 32.0}
                :t2.large  {:cpu 2.0 :ram 8.0}
                :t2.medium  {:cpu 2.0 :ram 4.0}
                :t2.micro  {:cpu 1.0 :ram 1.0}
                :t2.nano  {:cpu 1.0 :ram 0.5}
                :t2.small  {:cpu 1.0 :ram 2.0}
                :t2.xlarge  {:cpu 4.0 :ram 16.0}
                :t3.2xlarge  {:cpu 8.0 :ram 32.0}
                :t3.large  {:cpu 2.0 :ram 8.0}
                :t3.medium  {:cpu 2.0 :ram 4.0}
                :t3.micro  {:cpu 2.0 :ram 1.0}
                :t3.nano  {:cpu 2.0 :ram 0.5}
                :t3.small  {:cpu 2.0 :ram 2.0}
                :t3.xlarge  {:cpu 4.0 :ram 16.0}
                :x1.16xlarge  {:cpu 64.0 :ram 976.0}
                :x1.32xlarge  {:cpu 128.0 :ram 1952.0}})

(defn size [k v]
  (list 'defn (symbol (name k)) '[system]
        (list 'update-in 'system [:machine] (list 'fn '[m] (list 'merge 'm v)))))

(defmacro types []
  (let [fns (map (fn [[k v]] (size k v)) instances)
        names (mapv (fn [[k _]] (symbol (name k))) instances)]
    `(do ~@fns)))

(defmacro refers []
  (let [names (mapv (fn [[k _]] (symbol (name k))) instances)]
    `(def refer-instance-types
       (fn []
         (require '[re-core.presets.instance-types :as instance-types :refer ~names])))))

(types)
(refers)

(comment
  (clojure.pprint/pprint (macroexpand-1 '(types)))
  (clojure.pprint/pprint (macroexpand-1 '(refers))))
