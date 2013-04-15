(ns celestial.integration.supernal
  "Test basic dsl uses https://github.com/narkisr/puppet-supernal" 
  (:use 
    midje.sweet
    [supernal.baseline :only (basic-deploy)]
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (ns- execute execute-task run copy env)]))

(env 
  {:roles {
      :web #{{:host "192.168.1.26" :user "vagrant" :sudo true}}}
   })

(ns- deploy 
     (task stop
           (debug "stopping service" remote)
           (run "hostname")))

(ns- bar 
     (task stop2
           (let [foo 1]
             (debug "stoping in bar!"))))

(def artifact "git://github.com/narkisr/swag.git")

(fact "basic deployment tasks" :integration :supernal
    (execute basic-deploy {:app-name "foo" :src artifact} :web :join true))

(fact "single task" :integration :supernal
   (execute-task deploy/stop {:app-name "foo" :src artifact} :web :join true))
