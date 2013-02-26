(ns celestial.test.tasks
  (:use 
    expectations.scenarios
    [celestial.common :only (config)]
    [celestial.dnsmasq :only (add-host)] 
    [celestial.tasks :only (reload post-create-hooks)] 
    )  
 )


(def identity-hook
  {:hooks {:post-create {clojure.core/identity {:foo 1}}}}
  )

; (alter-var-root (var config) (fn [old] identity-hook))

(scenario 
  (let [machine {:machine {:hostname "foo" :ip_address "192.168.2.1"}}]
    (with-redefs [config identity-hook]
      (post-create-hooks machine))
      (expect (interaction (merge machine {:foo 1})))))

; (post-create-hooks {:machine {:hostname "foo" :ip_address "192.168.2.1"}})
