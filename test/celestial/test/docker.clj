(ns celestial.test.docker
  "Docker construction"
 (:require
  [clojure.core.strint :refer (<<)]
  [celestial.model :refer (vconstruct)]
  [celestial.fixtures.data :refer [redis-docker-spec]]
  [celestial.fixtures.core :refer [with-conf with-m?]] 
  )
 (:use midje.sweet))


(with-conf
  (fact "basic creation"
    (let [{:keys [start-spec create-spec]} (vconstruct redis-docker-spec)]
      start-spec => 
         (contains {:binds ["/vagrant:/vagrant"]
                    :port-bindings {"22/tcp" [{:host-ip "0.0.0.0" :host-port "2222"}]}})
      create-spec => 
         (contains 
          {:exposed-ports {"22/tcp"  {}} :image "narkisr:latest" :memory 
           4294967296 :volumes  {"/tmp"  {}}})
      )))

