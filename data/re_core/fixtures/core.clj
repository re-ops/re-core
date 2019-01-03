(ns re-core.fixtures.core
  (:refer-clojure :exclude [type]))

(defn is-type? [type]
  (fn [exception]
    (= type (-> exception ex-data :type))))

(defn with-m? [m actual]
  (= (-> actual ex-data :errors) m))

(defmacro with-conf
  "Using fixture/re-core.edn conf file"
  [f & body]
  (if (symbol? f)
    `(with-redefs [re-core.config/config ~f re-core.model/env :dev]
       ~@body)
    `(with-redefs [re-core.config/config re-core.fixtures.data/local-prox re-core.model/env :dev]
       ~@(conj body f))))

(defmacro with-dev
  [f & body]
  `(with-redefs [re-core.model/env :dev]
     ~@body))

(defmacro with-defaults
  "A fact that includes default conf and admin user"
  [& args]
  `(with-admin
     (with-conf ~@args)))
