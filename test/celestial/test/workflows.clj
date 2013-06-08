(ns celestial.test.workflows
  (:use 
    midje.sweet
    [celestial.config :only (config)]
    [celestial.workflows :only (resolve- run-hooks)])  
  (:import clojure.lang.ExceptionInfo))

(defn post-hook [v] v)
(defn error-hook [v] v)

(def identity-hook
  {:hooks 
    {:post-create {'celestial.test.workflows/post-hook {:foo 1}}
     :post-error {'celestial.test.workflows/error-hook {:foo 1}}
     }})

(let [machine {:machine {:hostname "foo" :ip_address "192.168.2.1"}} merged (merge machine {:foo 1})]
  (with-redefs [config identity-hook]
    (fact "post hook invoke"
      (run-hooks machine :post-create) => nil
      (provided 
        (post-hook merged) => merged :times 1))
    (fact "post error hook invoke"
      (run-hooks machine :post-error) => nil
      (provided 
        (error-hook merged) => merged :times 1))
    ))

(fact "missing fn resolution error"
  (resolve- "non.existing/fn") => (throws ExceptionInfo))
