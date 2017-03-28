(ns re-core.test.workflows
  (:require 
    [re-core.common :refer (resolve-)]
    [re-core.config :refer (config)]
    [re-core.workflows :refer (run-hooks)])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo))

(defn hook [v] v)

(def identity-hook
  {:hooks {'re-core.test.workflows/hook {:foo 1} }})


(let [machine {:machine {:hostname "foo" :ip_address "192.168.2.1"}}
      success (merge machine {:foo 1} {:event :success :workflow :reload})
      fail   (merge machine {:foo 1} {:event :error :workflow :reload})]
  (with-redefs [config identity-hook]
    (fact "post hook invoke"
      (run-hooks machine :reload :success) => nil
      (provided 
        (hook success) => success :times 1))
    (fact "post error hook invoke"
      (run-hooks machine :reload :error) => nil
      (provided 
        (hook fail) => fail :times 1))
    ))

(fact "missing fn resolution error"
  (resolve- "non.existing/fn") => (throws ExceptionInfo))

