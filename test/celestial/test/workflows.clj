(ns celestial.test.workflows
  (:use 
    midje.sweet
    [celestial.config :only (config)]
    [celestial.workflows :only (resolve- run-hooks)])  
  (:import clojure.lang.ExceptionInfo))

(defn hook [v] v)

(def identity-hook
  {:hooks {'celestial.test.workflows/hook {:foo 1} }})


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

