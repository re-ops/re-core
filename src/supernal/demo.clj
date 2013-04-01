(ns supernal.demo
  (:use 
    [supernal.baseline :only (basic-deploy) ]
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (ns- execute execute-task run copy env)]))


(env 
  {:roles {
      :web #{{:host "192.168.5.9" :user "vagrant" :sudo true}}}
   })

(ns- deploy 
  (task stop
    (debug "stopping service" remote)
    (run "hostname")))

(ns- bar 
    (task stop2
     (let [foo 1]
       (debug "stoping in bar!"))))

; (def artifact "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox-0.3.4.tar.gz")
(def artifact "git://github.com/narkisr/swag.git")

(execute basic-deploy {:app-name "foo" :src artifact} :web)

; (execute-task deploy/stop {:app-name "foo" :src artifact} :web) 
