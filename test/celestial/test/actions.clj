(ns celestial.test.actions
  (:require capistrano.remoter)
  (:use 
    [flatland.useful.map :only  (dissoc-in*)]
    midje.sweet
    [celestial.model :only (rconstruct)]
    [celestial.persistency :only (validate-action type-exists?)]
    [celestial.fixtures :only (redis-actions with-m?)] 
    )
  (:import clojure.lang.ExceptionInfo)
  )

(fact "actions to remoter construction"
      (let [cap (rconstruct redis-actions {:action :deploy :target "192.168.5.31"})]
        cap  => (contains  {:args ["deploy" "-s" "hostname=192.168.5.31"]
                            :src  "git://github.com/narkisr/cap-demo.git"})))


(fact "action validations"
    (with-redefs [type-exists? (fn [_] true)]

      (validate-action redis-actions)  => truthy

      (validate-action (assoc-in redis-actions [:actions :deploy :capistrano :args] nil)) =>
                    (throws ExceptionInfo (with-m? {:capistrano {:args '("must be present")}})) 

      (validate-action (assoc-in redis-actions [:actions :stop :supernal :args] nil)) => truthy

     )

(fact "missing type" 
   (validate-action (assoc redis-actions :operates-on "foo")) =>
      (throws ExceptionInfo (with-m? {:operates-on  '("type not found, create it first")})) 
   (provided 
     (type-exists? anything)  => false
     )
  )
      
      
)
