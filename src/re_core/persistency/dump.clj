(ns re-core.persistency.dump
  (:require
   [re-core.persistency.types :as t]
   [re-core.persistency.systems :as s]))

(defn dump []
  (let [systems (list identity :systems :print? false)
        types (list identity :types :print? false)]
    (spit "systems.edn" (with-out-str (pr (-> systems second :systems))))
    (spit "types.edn" (with-out-str (pr (-> types second :types))))))

(defn restore []
  (let [systems (clojure.edn/read-string (slurp "systems.edn"))
        types (clojure.edn/read-string (slurp "types.edn"))]
    (doseq [[id s] systems]
      (s/create s id))
    (doseq [[_ t] types]
      (t/create t))))
