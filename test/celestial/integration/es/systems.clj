(ns celestial.integration.es.systems
  "Testing system searching"
  (:require 
    [es.node :as es]
    [es.systems :as esys])
  (:use midje.sweet))

(with-state-changes [(before :facts (do (es/start-n-connect) (esys/initialize)))
                     (after :facts (es/stop))]
  
  )
