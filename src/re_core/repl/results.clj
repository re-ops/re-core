(ns re-core.repl.results
  "Collecting re-core pipeline results in memeory for fast repl use")

(def repl-results (atom []))

(defn append
  "Append last result"
  [m]
  (swap! repl-results (fn [r] (if (> (count r) 5) (subvec (conj r m) 1) (conj r m))))
  m)

(defn *all
  "The entire last result captured"
  []
  (last @repl-results))

(defn *last
  "Last system captured"
  []
  (map (comp first :args)
       (filter identity (flatten ((juxt :success :failure) (-> (*all) :results))))))

(defn *ids
  "Last system ids captured"
  []
  (map :system-id (*last)))

(defn *1
  "Last system id captured"
  []
  (if (= (count (*ids)) 1)
    (first (*ids))
    (throw (ex-info "No IDs are present from last run" {:ids *ids}))))

(defn refer-results []
  (require '[re-core.repl.results :as results :refer [*last *ids *1]]))
