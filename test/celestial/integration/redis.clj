(ns celestial.integration.redis
  "Redis test assume a redis sandbox on localhost, use https://github.com/narkisr/redis-sandbox"
  (:use midje.sweet
        [celestial.fixtures :only (is-type?)]
        [celestial.redis :only (clear-all hsetall* missing-keys wcar)])
  (:require [taoensso.carmine :as car])
  (:import clojure.lang.ExceptionInfo))

(fact "hsetall sanity" :integration :redis
     (wcar (car/del "play")) 
     (wcar (hsetall* "play" {:one {:two {:three 1}}})) => "OK"
     (wcar (car/hgetall* "play" true)) => {:one {:two {:three 1}}}
     (wcar (hsetall* "play" {:one {:six {:seven 3} :four {:five 2}}})) => "OK"
     (wcar (car/hgetall* "play" true)) => {:one {:six {:seven 3} :four {:five 2}}}
     (let [missing (wcar (missing-keys "play" {:two {:three 2}}))]
       (wcar (hsetall* "play" {:two {:three 2}} missing))) => "OK"
     (wcar (car/hgetall* "play" true)) =>  {:two {:three 2}}
      ) 
