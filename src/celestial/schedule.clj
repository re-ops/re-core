(ns celestial.schedule
  "Scheduled tasks"
 (:require 
   [chime :refer [chime-ch]]
   [clj-time.core :as t]
   [clj-time.periodic :refer  [periodic-seq]]
   [clojure.core.async :as a :refer [<! <!! go-loop]]))  

(import-logging)

(defn schedule [f t args]
  (let [chimes (chime-ch t)]
    (<!! 
      (go-loop []
        (when-let [msg (<! chimes)]
         (f args)
         (recur))))))

#_(schedule (periodic-seq (t/now) (-> 5 t/minutes)))
