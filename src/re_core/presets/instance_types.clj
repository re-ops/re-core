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
             :medium {:cpu 2 :ram 2}
             :large {:cpu 4 :ram 4}
             :xlarge {:cpu 8 :ram 16}}))

; slugs
(def slugs
  (supports #{:digital-ocean}
            (into {}
                  (map
                   (fn [{:keys [slug vcpus memory]}]
                     (if (re-matches #"\d.*" slug)
                       [(keyword (str ">" slug)) {:cpu vcpus :ram (/ memory 1024) :size slug}]
                       [(keyword slug) {:cpu vcpus :ram (/ memory 1024) :size slug}]))
                   (clojure.edn/read-string (slurp "resources/slugs.edn"))))))

; based on https://github.com/dustinkirkland/instance-type/blob/master/yaml/aws.yaml
(def aws
  (supports #{:lxc :kvm}
            (clojure.edn/read-string (slurp "resources/aws.edn"))))

(def all (merge aws simple slugs))

(defn size [k {:keys [cpu ram supports] :as v}]
  (list 'defn (symbol (name k))
        (str "A system with " cpu " cpu units and " ram " GB of ram supports " supports)
        '[system]
        (list 'let ['virt (list re-core.model/figure-virt 'system)]
              (list 'when-not (list supports 'virt)
                    (list 'throw (list 'ex-info (list str "Provided size " k " does not support " 'virt) {})))
              (list 'case 'virt
                    :digital-ocean (list 'assoc-in 'system [:digital-ocean :size] (v :size))
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
