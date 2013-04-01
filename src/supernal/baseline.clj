(ns supernal.baseline
  "A group of base recipes for deployment etc.."
  (:import java.util.Date)
  (:use 
    [clojure.core.strint :only (<<)]
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (ns- copy)]))

(defn target [name* t] (<< "/u/apps/releases/~{name}/~{t}"))
 
(ns- deploy 
  (task update-code
    (let [{:keys [app-name src]} args]
      (debug "updating code on" remote) 
      (copy src (target app-name (Date.)) ))) 
 
  (task start 
    (debug "starting service on" remote)) 
 
  (task stop
    (debug "stopping service on" remote))) 
 
