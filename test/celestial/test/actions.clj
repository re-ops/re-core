(ns celestial.test.actions
  (:require capistrano.remoter)
  (:use 
    [flatland.useful.map :only  (dissoc-in*)]
    midje.sweet
    [celestial.model :only (rconstruct)]
    [celestial.persistency :only (validate-action)]
    [celestial.fixtures :only (redis-actions with-m?)] 
    )
  (:import clojure.lang.ExceptionInfo)
  )

(fact "actions to remoter construction"
      (let [cap (rconstruct redis-actions {:action :deploy :target "192.168.5.31"})]
        cap  => (contains  {:args ["deploy" "-s" "hostname=192.168.5.31"]
                            :src  "git://github.com/narkisr/cap-demo.git"})))


(fact "action validations"
      (validate-action redis-actions)  => true
      (validate-action (assoc-in redis-actions [:actions :deploy :capistrano :args] nil))
         => (throws ExceptionInfo (with-m? {:capistrano {:args '("args must be present")}}))
      (validate-action (assoc-in redis-actions [:actions :stop :supernal :args] nil)) => true
)
