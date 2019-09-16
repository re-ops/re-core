(ns re-core.presets.instance-types
  "Instance size types (RAM/CPU)"
  (:require
   [re-core.model :refer (figure-virt)]))

(defn supports
  "Setting supported hypervisors"
  [vs m]
  (into {} (map (fn [[k v]] [k (assoc v :supports vs)]) m)))

; common sizes
(def simple
  (supports #{:lxc :kvm}
            {:tiny {:cpu 1 :ram 0.5}
             :small {:cpu 2 :ram 1}
             :large {:cpu 4 :ram 4}
             :xlarge {:cpu 8 :ram 8}}))

; slugs
(def slugs
  (supports #{:digital-ocean}
            (into {}
                  (map
                   (fn [{:keys [slug vcpus memory]}] [(keyword slug) {:cpu vcpus :ram (/ memory 1024)}])
                   (clojure.edn/read-string (slurp "resources/slugs.edn"))))))

; based on https://github.com/dustinkirkland/instance-type/blob/master/yaml/aws.yaml
(def aws
  (supports #{:lxc :aws :kvm}
            {:c1-medium {:cpu 2 :ram 1.7}
             :c1-xlarge  {:cpu 8 :ram 7}
             :c3-2xlarge  {:cpu 8 :ram 15}
             :c3-4xlarge  {:cpu 16 :ram 30}
             :c3-8xlarge  {:cpu 32 :ram 60}
             :c3-large  {:cpu 2 :ram 3.75}
             :c3-xlarge  {:cpu 4 :ram 7.5}
             :c4-2xlarge  {:cpu 8 :ram 15}
             :c4-4xlarge  {:cpu 16 :ram 30}
             :c4-8xlarge  {:cpu 36 :ram 60}
             :c4-large  {:cpu 2 :ram 3.75}
             :c4-xlarge  {:cpu 4 :ram 7.5}
             :c5-18xlarge  {:cpu 72 :ram 144}
             :c5-2xlarge  {:cpu 8 :ram 16}
             :c5-4xlarge  {:cpu 16 :ram 32}
             :c5-9xlarge  {:cpu 36 :ram 72}
             :c5-large  {:cpu 2 :ram 4}
             :c5-xlarge  {:cpu 4 :ram 8}
             :cc2-8xlarge  {:cpu 32 :ram 60.5}
             :cg1-4xlarge  {:cpu 16 :ram 22.5}
             :cr1-8xlarge  {:cpu 32 :ram 244}
             :d2-2xlarge  {:cpu 8 :ram 61}
             :d2-4xlarge  {:cpu 16 :ram 122}
             :d2-8xlarge  {:cpu 36 :ram 244}
             :d2-xlarge  {:cpu 4 :ram 30.5}
             :f1-16xlarge  {:cpu 64 :ram 976}
             :f1-2xlarge  {:cpu 8 :ram 122}
             :g2-2xlarge  {:cpu 8 :ram 15}
             :g2-8xlarge  {:cpu 32 :ram 60}
             :g3-16xlarge  {:cpu 64 :ram 488}
             :g3-4xlarge  {:cpu 16 :ram 122}
             :g3-8xlarge  {:cpu 32 :ram 244}
             :hi1-4xlarge  {:cpu 16 :ram 60.5}
             :hs1-8xlarge  {:cpu 16 :ram 117}
             :i2-2xlarge  {:cpu 8 :ram 61}
             :i2-4xlarge  {:cpu 16 :ram 122}
             :i2-8xlarge  {:cpu 32 :ram 244}
             :i2-xlarge  {:cpu 4 :ram 30.5}
             :i3-16xlarge  {:cpu 64 :ram 488}
             :i3-2xlarge  {:cpu 8 :ram 61}
             :i3-4xlarge  {:cpu 16 :ram 122}
             :i3-8xlarge  {:cpu 32 :ram 244}
             :i3-large  {:cpu 2 :ram 15.25}
             :i3-xlarge  {:cpu 4 :ram 30.5}
             :m1-large  {:cpu 2 :ram 7.5}
             :m1-medium  {:cpu 1 :ram 3.75}
             :m1-small  {:cpu 1 :ram 1.7}
             :m1-xlarge  {:cpu 4 :ram 15}
             :m2-2xlarge  {:cpu 4 :ram 34.2}
             :m2-4xlarge  {:cpu 8 :ram 68.4}
             :m2-xlarge  {:cpu 2 :ram 17.1}
             :m3-2xlarge  {:cpu 8 :ram 30}
             :m3-large  {:cpu 2 :ram 7.5}
             :m3-medium  {:cpu 1 :ram 3.75}
             :m3-xlarge  {:cpu 4 :ram 15}
             :m4-10xlarge  {:cpu 40 :ram 160}
             :m4-16xlarge  {:cpu 64 :ram 256}
             :m4-2xlarge  {:cpu 8 :ram 32}
             :m4-4xlarge  {:cpu 16 :ram 64}
             :m4-large  {:cpu 2 :ram 8}
             :m4-xlarge  {:cpu 4 :ram 16}
             :p2-16xlarge  {:cpu 64 :ram 732}
             :p2-8xlarge  {:cpu 32 :ram 488}
             :p2-xlarge  {:cpu 4 :ram 61}
             :r3-2xlarge  {:cpu 8 :ram 61}
             :r3-4xlarge  {:cpu 16 :ram 122}
             :r3-8xlarge  {:cpu 32 :ram 244}
             :r3-large  {:cpu 2 :ram 15.25}
             :r3-xlarge  {:cpu 4 :ram 30.5}
             :r4-16xlarge  {:cpu 64 :ram 488}
             :r4-2xlarge  {:cpu 8 :ram 61}
             :r4-4xlarge  {:cpu 16 :ram 122}
             :r4-8xlarge  {:cpu 32 :ram 244}
             :r4-large  {:cpu 2 :ram 15.25}
             :r4-xlarge  {:cpu 4 :ram 30.5}
             :t1-micro  {:cpu 1 :ram 0.613}
             :t2-2xlarge  {:cpu 8 :ram 32}
             :t2-large  {:cpu 2 :ram 8}
             :t2-medium  {:cpu 2 :ram 4}
             :t2-micro  {:cpu 1 :ram 1}
             :t2-nano  {:cpu 1 :ram 0.5}
             :t2-small  {:cpu 1 :ram 2}
             :t2-xlarge  {:cpu 4 :ram 16}
             :t3-2xlarge  {:cpu 8 :ram 32}
             :t3-large  {:cpu 2 :ram 8}
             :t3-medium  {:cpu 2 :ram 4}
             :t3-micro  {:cpu 2 :ram 1}
             :t3-nano  {:cpu 2 :ram 0.5}
             :t3-small  {:cpu 2 :ram 2}
             :t3-xlarge  {:cpu 4 :ram 16}
             :x1-16xlarge  {:cpu 64 :ram 976}
             :x1-32xlarge  {:cpu 128 :ram 1952}}))

(def all (merge aws simple slugs))

(defn size [k {:keys [cpu ram supports] :as v}]
  (list 'defn (symbol (name k))
        (str "A system with " cpu " cpu units and " ram " GB of ram supports " supports)
        '[system]
        (list 'let ['virt (list re-core.model/figure-virt 'system)]
              (list 'when-not (list supports 'virt)
                    (list 'throw (list 'ex-info (list str "Provided size " k " does not support " 'virt) {})))
              (list 'case 'virt
                    :digital-ocean (list 'assoc-in 'system [:digital-ocean :size] (name k))
                    :aws (list 'assoc-in 'system [:aws :instance-type] (name k))
                    (list 'update-in 'system [:machine] (list 'fn '[m] (list 'merge 'm v)))))))

(defmacro types []
  (let [fns (map (fn [[k v]] (size k v)) all)
        names (mapv (fn [[k _]] (symbol (name k))) all)]
    `(do ~@fns)))

(defmacro refers []
  (let [names (mapv (fn [[k _]] (symbol (name k))) all)]
    `(def refer-instance-types
       (fn []
         (require '[re-core.presets.instance-types :as instance-types :refer ~names])))))

(types)
(refers)

(comment
  (clojure.pprint/pprint (macroexpand-1 '(types)))
  (clojure.pprint/pprint (macroexpand-1 '(refers))))
