(ns celestial.test.actions
 (:require capistrano.remoter)
 (:use 
    midje.sweet
    [celestial.model :only (rconstruct)]
    [celestial.fixtures :only (redis-actions)] 
    ))

(fact "actions to remoter construction"
  (let [cap (rconstruct redis-actions {:action :deploy :target "192.168.5.31"})]
     cap  => (contains  {:args ["deploy" "-s" "hostname=192.168.5.31"]
                         :src  "git://github.com/narkisr/cap-demo.git" 
                         })))

