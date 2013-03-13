(ns celestial.test.tasks
  (:use 
    slingshot.test
    expectations.scenarios
    [celestial.common :only (config)]
    [celestial.dnsmasq :only (add-host)] 
    [celestial.tasks :only (resolve- reload post-create-hooks)])  
  (:import clojure.lang.ExceptionInfo)
 )

(def identity-hook
  {:hooks {:post-create {'clojure.core/identity {:foo 1}}}})

(scenario 
  (let [machine {:machine {:hostname "foo" :ip_address "192.168.2.1"}}]
    (with-redefs [config identity-hook]
      (post-create-hooks machine))
      (expect (interaction (identity (merge machine {:foo 1}))))))

(scenario
  (expect ExceptionInfo (resolve- "non.existing/fn")))
